package com.busmonitor.service;

import com.busmonitor.model.BusToken;
import com.busmonitor.model.RoundLog;
import com.busmonitor.repository.RoundLogRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Service
public class NodeService {

    @Autowired
    private RoundLogRepository roundLogRepository;

    private final RestTemplate restTemplate;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService syncExecutor = Executors.newCachedThreadPool();

    @Value("${station.id}")
    private String myId;

    @Value("${station.order}")
    private int myOrder;

    @Value("${station.name}")
    private String myName;

    @Value("${server1.url}")
    private String url1;
    @Value("${server2.url}")
    private String url2;
    @Value("${server3.url}")
    private String url3;
    @Value("${server4.url}")
    private String url4;
    @Value("${server5.url}")
    private String url5;

    private final Map<String, String>  allUrls    = new LinkedHashMap<>();
    private final Map<String, Integer> allOrders  = new LinkedHashMap<>();
    private final Map<String, Boolean> peerStatus = new ConcurrentHashMap<>();

    private volatile BusToken token       = new BusToken();
    private volatile boolean isRunning    = false;
    private volatile boolean inTransit    = false;
    private volatile boolean isLeader     = false;
    private volatile String  leaderId     = "";
    private volatile int     currentEpoch = 0;
    private volatile boolean shuttingDown = false;
    private volatile long    lastTokenTime = System.currentTimeMillis();
    private boolean initialized = false;

    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());

    public NodeService() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5000);
        f.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(f);
    }

    // ==================== INIT ====================

    private synchronized void ensureInit() {
        if (initialized) return;
        initialized = true;

        allUrls.put("server1-ngan", "https://server1-ngan.onrender.com");
        allUrls.put("server2-nhi",  "https://server2-nhi.onrender.com");
        allUrls.put("server3-my",   "https://server3-my.onrender.com");
        allUrls.put("server4-suong","https://server4-suong.onrender.com");
        allUrls.put("server5-hang", "https://server5-hang.onrender.com");

        allOrders.put("server1-ngan", 1);
        allOrders.put("server2-nhi",  2);
        allOrders.put("server3-my",   3);
        allOrders.put("server4-suong",4);
        allOrders.put("server5-hang", 5);

        // Ultimate fail-safe to override ANY environment variable typos!
        if ("server1-ngan".equals(myId)) { myName = "Tram 1 - Ngan"; myOrder = 1; }
        if ("server2-nhi".equals(myId)) { myName = "Tram 2 - Nhi"; myOrder = 2; }
        if ("server3-my".equals(myId)) { myName = "Tram 3 - My"; myOrder = 3; }
        if ("server4-suong".equals(myId)) { myName = "Tram 4 - Suong"; myOrder = 4; }
        if ("server5-hang".equals(myId)) { myName = "Tram 5 - Hang"; myOrder = 5; }

        // BUG FIX 1: Tat ca peer mac dinh la FALSE cho den khi ping xac nhan
        allUrls.forEach((id, url) -> {
            if (!id.equals(myId)) peerStatus.put(id, false);
        });

        log("🚀 Khoi dong: " + myId + " | Thu tu: " + myOrder);
        log("📍 URL cua toi: " + allUrls.get(myId));
        log("🔗 URL cac peer: " + allUrls);
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        isRunning = false;
        scheduler.shutdownNow();
        syncExecutor.shutdownNow();
    }

    // ==================== PING & LEADER ELECTION ====================

    @Scheduled(fixedDelay = 10000)
    public void pingAndElect() {
        if (shuttingDown) return;
        ensureInit();

        allUrls.forEach((id, url) -> {
            if (id.equals(myId)) return;
            boolean wasAlive = peerStatus.getOrDefault(id, false);
            boolean alive    = ping(url);
            peerStatus.put(id, alive);

            if (!wasAlive && alive) {
                log("✅ " + id + " hoi phuc! Them lai vao vong.");
                syncExecutor.submit(() -> syncDataToServer(id));
            }
            if (wasAlive && !alive) {
                log("❌ " + id + " mat ket noi! Loai khoi vong.");
            }
        });

        electLeader();

        // Watchdog
        if (isLeader && isRunning && !shuttingDown) {
            long idle = System.currentTimeMillis() - lastTokenTime;
            if (!inTransit && idle > 6000) {
                log("🔄 Watchdog: khoi dong vong moi...");
                scheduler.schedule(this::processAsLeader, 0, TimeUnit.SECONDS);
            } else if (inTransit && idle > 30000) {
                log("⚠️ Watchdog: Token bi mat 30s! Tao lai token moi.");
                inTransit = false;
                lastTokenTime = System.currentTimeMillis();
                currentEpoch++;
                token = new BusToken();
                token.setEpoch(currentEpoch);
                scheduler.schedule(this::processAsLeader, 0, TimeUnit.SECONDS);
            }
        }
    }

    private void electLeader() {
        String best   = myId;
        int    bestOrd = myOrder;

        for (Map.Entry<String, Boolean> e : peerStatus.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) {
                int ord = allOrders.getOrDefault(e.getKey(), 99);
                if (ord < bestOrd) { bestOrd = ord; best = e.getKey(); }
            }
        }

        boolean wasLeader = isLeader;
        isLeader = best.equals(myId);
        leaderId = best;

        if (!wasLeader && isLeader) {
            currentEpoch++;
            log("👑 " + myId + " DUOC BAU LAM LEADER! (epoch " + currentEpoch + ")");
            isRunning  = true;
            inTransit  = false;
            token      = new BusToken();
            token.setEpoch(currentEpoch);
            scheduler.schedule(this::processAsLeader, 3, TimeUnit.SECONDS);
        } else if (wasLeader && !isLeader) {
            log("📤 Nhuong leader cho " + best + " (epoch " + currentEpoch + ")");
            isRunning = false;
            inTransit = false;
        }
    }

    // BUG FIX 2: Kiem tra ca status tra ve, khong chi exception
    private boolean ping(String url) {
        try {
            Map<?, ?> resp = restTemplate.getForObject(url + "/api/health", Map.class);
            return resp != null && "UP".equals(resp.get("status"));
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== START / STOP ====================

    public String startSystem() {
        ensureInit();
        pingAndElect();
        isRunning = true;
        log("▶️ He thong khoi dong! Leader: " + leaderId + " | Toi: " + myId);
        if (isLeader) {
            currentEpoch++;
            token = new BusToken();
            token.setEpoch(currentEpoch);
            lastTokenTime = System.currentTimeMillis();
            scheduler.schedule(this::processAsLeader, 500, TimeUnit.MILLISECONDS);
        }
        return "Khoi dong! Leader: " + leaderId;
    }

    public String stopSystem() {
        isRunning = false;
        inTransit = false;
        log("⏹️ He thong dung");
        return "Da dung!";
    }

    // ==================== TOKEN PROCESSING ====================

    private synchronized void processAsLeader() {
        if (shuttingDown || !isRunning || !isLeader || inTransit) return;

        int boarded  = new Random().nextInt(10) + 1;
        int alighted = Math.min(token.getCurrentPassengers(), new Random().nextInt(5));
        token.setCurrentPassengers(token.getCurrentPassengers() + boarded - alighted);
        double revenue = boarded * 8000.0;
        token.setTotalRevenue(token.getTotalRevenue() + revenue);
        token.setLastStation(myId);
        token.setCurrentLeader(myId);
        token.setEpoch(currentEpoch);
        token.setRoundEntries(new ArrayList<>());

        int roundNum = token.getTotalRounds() + 1;
        try {
            roundLogRepository.save(new RoundLog(roundNum, boarded, alighted, revenue, myName));
        } catch (Exception e) {
            log("⚠️ DB Save Error (Leader): " + e.getMessage());
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("roundNumber", roundNum);
        entry.put("passengersBoarded", boarded);
        entry.put("passengersAlighted", alighted);
        entry.put("revenue", revenue);
        entry.put("stationName", myName);
        token.getRoundEntries().add(entry);

        log(String.format("🚉 %s [LEADER]: +%d len, -%d xuong | %d nguoi | %.0f VND",
                myName, boarded, alighted, token.getCurrentPassengers(), revenue));

        // BUG FIX 3: Build active list dung - chi lay peer thuc su ONLINE
        List<String> active = new ArrayList<>();
        allUrls.forEach((id, url) -> {
            if (!id.equals(myId) && Boolean.TRUE.equals(peerStatus.get(id))) {
                active.add(id);
            }
        });
        token.setActiveServers(active);

        log("📋 Active peers: " + active);

        inTransit = true;
        lastTokenTime = System.currentTimeMillis();
        syncExecutor.submit(() -> broadcastUiState(token));
        sendToNext(myId, active);
    }

    public synchronized void receiveToken(BusToken incoming) {
        if (shuttingDown) return;
        ensureInit();

        // Luon cap nhat epoch theo token moi de pha ma tran Deadlock cua cac Leader cu
        this.currentEpoch = incoming.getEpoch();

        // BUG FIX 5: Kiem tra vong lap - tram nay da xu ly chua?
        if (incoming.getRoundEntries() != null) {
            for (Map<String, Object> e : incoming.getRoundEntries()) {
                if (myName.equals(e.get("stationName"))) {
                    log("⚠️ Vong lap! Tram " + myName + " da xu ly roi. Bo qua.");
                    return;
                }
            }
        }

        this.token = incoming;
        if (incoming.getEpoch() > currentEpoch) currentEpoch = incoming.getEpoch();

        int boarded  = new Random().nextInt(8) + 1;
        int alighted = Math.min(token.getCurrentPassengers(), new Random().nextInt(5));
        token.setCurrentPassengers(token.getCurrentPassengers() + boarded - alighted);
        double revenue = boarded * 8000.0;
        token.setTotalRevenue(token.getTotalRevenue() + revenue);
        token.setLastStation(myId);

        int roundNum = token.getTotalRounds() + 1;
        try {
            roundLogRepository.save(new RoundLog(roundNum, boarded, alighted, revenue, myName));
        } catch (Exception e) {
            log("⚠️ DB Save Error (Follower): " + e.getMessage());
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("roundNumber", roundNum);
        entry.put("passengersBoarded", boarded);
        entry.put("passengersAlighted", alighted);
        entry.put("revenue", revenue);
        entry.put("stationName", myName);
        token.getRoundEntries().add(entry);

        log(String.format("🚉 %s: +%d len, -%d xuong | %d nguoi | %.0f VND",
                myName, boarded, alighted, token.getCurrentPassengers(), revenue));

        lastTokenTime = System.currentTimeMillis();
        syncExecutor.submit(() -> broadcastUiState(token));

        List<String> active = new ArrayList<>(token.getActiveServers());
        sendToNext(myId, active);
    }

    public synchronized void receiveTokenBack(BusToken incoming) {
        if (shuttingDown) return;
        
        // LEADER phai diet token rác tu cac epoch bi loi (Ghost Tokens loop)
        if (incoming.getEpoch() < currentEpoch) {
            log("⚠️ Token-back la xe rác tu epoch cu (" + incoming.getEpoch() + " < " + currentEpoch + "). Tieu huy!");
            return;
        }

        // Luon cap nhat epoch theo token moi de chan Deadlock
        this.currentEpoch = incoming.getEpoch();

        // Tieu huy ghost token do Watchdog sinh ra thua
        if (!this.inTransit) {
            log("⚠️ Nhan token-back nhung xe da o ben (Ghost token). Tieu huy de chong tang vong ao!");
            return;
        }

        this.token = incoming;
        this.token.setLastStation(myId); // Sửa lỗi giao diện hiển thị xe đứng im ở Hằng
        this.token.setTotalRounds(incoming.getTotalRounds() + 1);
        this.inTransit = false;
        this.lastTokenTime = System.currentTimeMillis();

        log("🏁 Vong #" + token.getTotalRounds() + " hoan thanh! Tong: "
                + String.format("%.0f", token.getTotalRevenue()) + " VND");

        syncExecutor.submit(() -> broadcastUiState(token));

        final List<Map<String, Object>> entries = new ArrayList<>(token.getRoundEntries());
        syncExecutor.submit(() -> broadcastRoundData(entries, token.getTotalRounds()));

        if (isRunning && isLeader) {
            scheduler.schedule(this::processAsLeader, 3, TimeUnit.SECONDS);
        }
    }

    // ==================== CHUYEN TOKEN ====================

    private void sendToNext(String fromId, List<String> active) {
        // BUG FIX 6: Dung vong lap thay vi de quy tranh StackOverflow
        int maxAttempts = active.size() + 2;
        int attempts = 0;
        List<String> remaining = new ArrayList<>(active);

        while (attempts < maxAttempts) {
            attempts++;
            String next = null;
            String tokenLeader = token.getCurrentLeader() != null ? token.getCurrentLeader() : leaderId;

            if (fromId.equals(tokenLeader)) {
                next = remaining.isEmpty() ? null : remaining.get(0);
            } else {
                int idx = remaining.indexOf(fromId);
                if (idx >= 0 && idx < remaining.size() - 1) {
                    next = remaining.get(idx + 1);
                }
            }

            if (next == null) {
                returnTokenToLeader(tokenLeader);
                return;
            }

            String nextUrl = allUrls.get(next);
            if (nextUrl == null) {
                log("❌ Khong tim thay URL cho " + next + " -> bo qua");
                remaining.remove(next);
                token.setActiveServers(new ArrayList<>(remaining));
                continue;
            }

            try {
                log("📤 Gui token den " + next + " (" + nextUrl + ")");
                // BUG FIX 7: Bo targetStation check - gay ra loi khi URL cau hinh sai
                restTemplate.postForObject(nextUrl + "/api/receive-token", token, String.class);
                return; // Thanh cong
            } catch (Exception e) {
                log("❌ Gui that bai den " + next + ": " + e.getMessage());
                peerStatus.put(next, false);
                remaining.remove(next);
                token.setActiveServers(new ArrayList<>(remaining));
            }
        }

        log("⚠️ Tat ca server that bai! Tra token ve leader.");
        returnTokenToLeader(token.getCurrentLeader() != null ? token.getCurrentLeader() : leaderId);
    }

    private void returnTokenToLeader(String tokenLeader) {
        if (tokenLeader == null) tokenLeader = leaderId;

        if (myId.equals(tokenLeader)) {
            receiveTokenBack(token);
            return;
        }

        String leaderUrl = allUrls.get(tokenLeader);
        if (leaderUrl == null) {
            log("❌ Khong tim thay URL leader: " + tokenLeader);
            return;
        }
        try {
            log("📤 Tra token ve leader: " + tokenLeader);
            restTemplate.postForObject(leaderUrl + "/api/receive-token-back", token, String.class);
        } catch (Exception e) {
            log("❌ Khong the tra token ve leader " + tokenLeader + ": " + e.getMessage());
        }
    }

    // ==================== DONG BO ====================

    private void broadcastRoundData(List<Map<String, Object>> entries, int roundNum) {
        if (shuttingDown || entries == null || entries.isEmpty()) return;
        log("📡 Dong bo vong #" + roundNum + " den tat ca server...");
        allUrls.forEach((id, url) -> {
            if (id.equals(myId) || !Boolean.TRUE.equals(peerStatus.get(id))) return;
            try {
                restTemplate.postForObject(url + "/api/sync-round", entries, String.class);
                log("✅ Dong bo thanh cong den " + id);
            } catch (Exception e) {
                log("⚠️ Dong bo that bai den " + id);
            }
        });
    }

    private void broadcastUiState(BusToken t) {
        if (shuttingDown) return;
        allUrls.forEach((id, url) -> {
            if (id.equals(myId) || !Boolean.TRUE.equals(peerStatus.get(id))) return;
            try {
                restTemplate.postForObject(url + "/api/sync-ui", t, String.class);
            } catch (Exception ignored) {}
        });
    }

    public void updateUiToken(BusToken t) {
        if (t != null && t.getEpoch() >= currentEpoch) {
            this.token = t;
            this.lastTokenTime = System.currentTimeMillis();
        }
    }

    public void receiveSyncData(List<Map<String, Object>> entries) {
        if (shuttingDown || entries == null) return;
        int saved = 0;
        for (Map<String, Object> e : entries) {
            try {
                int roundNumber    = ((Number) e.get("roundNumber")).intValue();
                String stationName = (String) e.get("stationName");
                if (stationName == null || stationName.equals(myName)) continue;
                if (roundLogRepository.existsByRoundNumberAndStationName(roundNumber, stationName)) continue;

                int boarded    = ((Number) e.get("passengersBoarded")).intValue();
                int alighted   = ((Number) e.get("passengersAlighted")).intValue();
                double revenue = ((Number) e.get("revenue")).doubleValue();
                roundLogRepository.save(new RoundLog(roundNumber, boarded, alighted, revenue, stationName));
                saved++;
            } catch (Exception ex) {
                log("⚠️ Loi dong bo: " + ex.getMessage());
            }
        }
        if (saved > 0) log("📥 Dong bo: da luu " + saved + " ban ghi moi");
    }

    private void syncDataToServer(String serverId) {
        if (shuttingDown) return;
        String url = allUrls.get(serverId);
        if (url == null) return;
        try {
            List<RoundLog> all = roundLogRepository.findTop200ByOrderByTimestampDesc();
            if (all.isEmpty()) return;
            List<Map<String, Object>> entries = new ArrayList<>();
            for (RoundLog rl : all) {
                if ("SYSTEM_INIT".equals(rl.getStationName())) continue;
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("roundNumber", rl.getRoundNumber());
                entry.put("passengersBoarded", rl.getPassengersBoarded());
                entry.put("passengersAlighted", rl.getPassengersAlighted());
                entry.put("revenue", rl.getRevenue());
                entry.put("stationName", rl.getStationName());
                entries.add(entry);
            }
            if (!entries.isEmpty()) {
                restTemplate.postForObject(url + "/api/sync-round", entries, String.class);
                log("📡 Dong bo " + entries.size() + " ban ghi den " + serverId + " (hoi phuc)");
            }
        } catch (Exception e) {
            log("⚠️ Dong bo den " + serverId + " that bai: " + e.getMessage());
        }
    }

    // ==================== GETTERS ====================

    public String  getMyId()         { ensureInit(); return myId; }
    public boolean isLeader()        { return isLeader; }
    public String  getLeaderId()     { return leaderId; }
    public boolean isRunning()       { return isRunning; }
    public boolean isInTransit()     { return inTransit; }
    public BusToken getToken()       { return token; }
    public int     getCurrentEpoch() { return currentEpoch; }
    public List<RoundLog> getDbLogs(){ return roundLogRepository.findTop10ByOrderByTimestampDesc(); }
    public List<String> getLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    public Map<String, Boolean> getServerStatus() {
        ensureInit();
        Map<String, Boolean> result = new LinkedHashMap<>();
        allUrls.forEach((id, url) ->
            // BUG FIX 8: Dung Boolean.TRUE.equals() tranh NullPointerException
            result.put(id, id.equals(myId) ? true : Boolean.TRUE.equals(peerStatus.get(id)))
        );
        return result;
    }

    private void log(String msg) {
        String entry = "[" + LocalDateTime.now().format(FMT) + "] " + msg;
        logs.add(entry);
        System.out.println(entry);
        if (logs.size() > 300) logs.remove(0);
    }
}
