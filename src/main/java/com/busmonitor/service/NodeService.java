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

    // Scheduler thay cho Thread.sleep de tranh de quy va blocking
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // MY IDENTITY - set qua bien moi truong tren Render
    @Value("${station.id}")
    private String myId;

    @Value("${station.order}")
    private int myOrder;

    @Value("${station.name}")
    private String myName;

    // ALL SERVER URLs
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

    // Registry
    private final Map<String, String>  allUrls   = new LinkedHashMap<>();
    private final Map<String, Integer> allOrders = new LinkedHashMap<>();
    private final Map<String, Boolean> peerStatus = new ConcurrentHashMap<>();

    // State - volatile de dam bao thread safety
    private BusToken token          = new BusToken();
    private volatile boolean isRunning   = false;
    private volatile boolean inTransit   = false;
    private volatile boolean isLeader    = false;
    private volatile String  leaderId    = "";
    private volatile int     currentEpoch = 0;
    private volatile boolean shuttingDown = false;
    private boolean  initialized = false;

    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());

    // Constructor: cau hinh RestTemplate voi timeout
    public NodeService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);   // 5s connect timeout
        factory.setReadTimeout(10000);     // 10s read timeout
        this.restTemplate = new RestTemplate(factory);
    }

    // ==================== INIT ====================

    private synchronized void ensureInit() {
        if (initialized) return;
        initialized = true;
        allUrls.put("server1-ngan", url1);
        allUrls.put("server2-nhi",  url2);
        allUrls.put("server3-my",   url3);
        allUrls.put("server4-suong",url4);
        allUrls.put("server5-hang", url5);
        allOrders.put("server1-ngan", 1);
        allOrders.put("server2-nhi",  2);
        allOrders.put("server3-my",   3);
        allOrders.put("server4-suong",4);
        allOrders.put("server5-hang", 5);
        // Default: all peers offline until pinged
        allUrls.forEach((id, url) -> {
            if (!id.equals(myId)) peerStatus.put(id, false);
        });
        log("🚀 Khoi dong: " + myId + " (thu tu: " + myOrder + ")");
    }

    // ==================== SHUTDOWN ====================

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        isRunning = false;
        scheduler.shutdownNow();
        log("🛑 Server dang tat...");
    }

    // ==================== PING & LEADER ELECTION ====================

    @Scheduled(fixedDelay = 10000)
    public void pingAndElect() {
        if (shuttingDown) return;
        ensureInit();
        // Ping all peers
        allUrls.forEach((id, url) -> {
            if (id.equals(myId)) return;
            boolean wasAlive = peerStatus.getOrDefault(id, false);
            boolean alive    = ping(url);
            peerStatus.put(id, alive);
            if (!wasAlive && alive) {
                log("✅ " + id + " hoi phuc! Them lai vao vong.");
                // Dong bo du lieu den server vua hoi phuc (chay async)
                final String recoveredId = id;
                scheduler.schedule(() -> syncDataToServer(recoveredId), 2, TimeUnit.SECONDS);
            }
            if (wasAlive  && !alive) log("❌ " + id + " mat ket noi! Loai khoi vong.");
        });
        electLeader();
    }

    private void electLeader() {
        // Find lowest-order alive server = leader (Bully Algorithm)
        String best  = myId;
        int bestOrd  = myOrder;
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
            // Toi vua duoc bau lam leader moi
            currentEpoch++;
            log("👑 " + myId + " duoc bau lam LEADER! (epoch " + currentEpoch + ")");
            // Token recovery: tao token moi de phuc hoi he thong
            isRunning = true;
            inTransit = false;
            token = new BusToken();
            token.setEpoch(currentEpoch);
            log("🔄 Tao token moi (epoch " + currentEpoch + ") de phuc hoi he thong");
            scheduler.schedule(this::processAsLeader, 3, TimeUnit.SECONDS);
        } else if (wasLeader && !isLeader) {
            log("📤 Nhuong leader cho " + best);
            isRunning = false;
            inTransit = false;
        }
    }

    private boolean ping(String url) {
        try {
            restTemplate.getForObject(url + "/api/health", Map.class);
            return true;
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

    // Leader khoi tao vong moi
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

        // Xoa round entries cu, bat dau vong moi
        token.setRoundEntries(new ArrayList<>());
        int roundNum = token.getTotalRounds() + 1;

        // Save to MY DB
        roundLogRepository.save(new RoundLog(
                roundNum, boarded, alighted, revenue, myName));

        // Them entry cua toi vao token de dong bo
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("roundNumber", roundNum);
        entry.put("passengersBoarded", boarded);
        entry.put("passengersAlighted", alighted);
        entry.put("revenue", revenue);
        entry.put("stationName", myName);
        token.getRoundEntries().add(entry);

        log(String.format("🚉 %s [LEADER]: +%d len, -%d xuong | %d nguoi | %.0f VND",
                myName, boarded, alighted, token.getCurrentPassengers(), revenue));

        // Build list of alive peers (excluding self)
        List<String> active = new ArrayList<>();
        allUrls.forEach((id, url) -> {
            if (!id.equals(myId) && Boolean.TRUE.equals(peerStatus.get(id))) {
                active.add(id);
            }
        });
        token.setActiveServers(active);

        inTransit = true;
        sendToNext(myId, active);
    }

    // Non-leader nhan token, xu ly, forwardd
    public synchronized void receiveToken(BusToken incoming) {
        if (shuttingDown) return;
        ensureInit();

        // Tu choi token tu epoch cu (chong split-brain)
        // epoch=0 nghia la token di qua server code cu, khong reject
        if (incoming.getEpoch() > 0 && incoming.getEpoch() < currentEpoch) {
            log("⚠️ Nhan token tu epoch cu (" + incoming.getEpoch() + " < " + currentEpoch + "), bo qua");
            return;
        }

        this.token = incoming;
        // Cap nhat epoch neu cao hon
        if (incoming.getEpoch() > currentEpoch) {
            currentEpoch = incoming.getEpoch();
        }

        int boarded  = new Random().nextInt(8) + 1;
        int alighted = Math.min(token.getCurrentPassengers(), new Random().nextInt(5));
        token.setCurrentPassengers(token.getCurrentPassengers() + boarded - alighted);
        double revenue = boarded * 8000.0;
        token.setTotalRevenue(token.getTotalRevenue() + revenue);
        token.setLastStation(myId);

        int roundNum = token.getTotalRounds() + 1;

        roundLogRepository.save(new RoundLog(
                roundNum, boarded, alighted, revenue, myName));

        // Them entry cua toi vao token de dong bo
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("roundNumber", roundNum);
        entry.put("passengersBoarded", boarded);
        entry.put("passengersAlighted", alighted);
        entry.put("revenue", revenue);
        entry.put("stationName", myName);
        token.getRoundEntries().add(entry);

        log(String.format("🚉 %s: +%d len, -%d xuong | %d nguoi | %.0f VND",
                myName, boarded, alighted, token.getCurrentPassengers(), revenue));

        // Forward to next alive server
        List<String> active = new ArrayList<>(token.getActiveServers());
        sendToNext(myId, active);
    }

    // Leader nhan token sau khi da di het 1 vong
    public synchronized void receiveTokenBack(BusToken incoming) {
        if (shuttingDown) return;
        // Tu choi token tu epoch cu
        // epoch=0 nghia la token di qua server code cu, khong reject
        if (incoming.getEpoch() > 0 && incoming.getEpoch() < currentEpoch) {
            log("⚠️ Nhan token-back tu epoch cu (" + incoming.getEpoch() + " < " + currentEpoch + "), bo qua");
            return;
        }

        this.token = incoming;
        this.token.setTotalRounds(incoming.getTotalRounds() + 1);
        this.inTransit = false;

        log("🏁 Vong #" + token.getTotalRounds() + " hoan thanh! Tong: "
                + String.format("%.0f", token.getTotalRevenue()) + " VND");

        // === DONG BO DU LIEU: broadcast round data den tat ca server ===
        broadcastRoundData(token.getRoundEntries());

        // Len lich vong tiep theo (non-blocking, khong de quy)
        if (isRunning && isLeader) {
            scheduler.schedule(this::processAsLeader, 3, TimeUnit.SECONDS);
        }
    }

    // ==================== CHUYEN TOKEN (VONG LAP, KHONG DE QUY) ====================

    /**
     * Gui token den server tiep theo trong danh sach active.
     * Dung vong lap thay vi de quy de tranh StackOverflow.
     */
    private void sendToNext(String fromId, List<String> active) {
        int maxAttempts = active.size() + 2; // gioi han an toan
        int attempts = 0;

        while (attempts < maxAttempts) {
            attempts++;
            String next = null;

            if (fromId.equals(leaderId)) {
                // Leader vua xu ly -> gui den peer dau tien
                next = active.isEmpty() ? null : active.get(0);
            } else {
                int idx = active.indexOf(fromId);
                if (idx >= 0 && idx < active.size() - 1) {
                    next = active.get(idx + 1);
                }
                // else: cuoi danh sach -> tra ve leader
            }

            // Khong con server nao -> tra token ve leader
            if (next == null) {
                returnTokenToLeader();
                return;
            }

            String nextUrl = allUrls.get(next);
            try {
                log("📤 Gui token den " + next);
                restTemplate.postForObject(nextUrl + "/api/receive-token", token, String.class);
                return; // Thanh cong!
            } catch (Exception e) {
                log("❌ Gui that bai den " + next + " -> bo qua, thu server tiep theo");
                peerStatus.put(next, false);
                active.remove(next);
                token.setActiveServers(active);
                // Vong lap tiep tuc thu server ke tiep
            }
        }

        // Da het tat ca server
        log("⚠️ Khong con server nao de gui! Tra token ve leader.");
        returnTokenToLeader();
    }

    /**
     * Tra token ve cho leader.
     * Neu chinh minh la leader -> xu ly truc tiep (khong can HTTP).
     */
    private void returnTokenToLeader() {
        if (myId.equals(leaderId)) {
            // Toi la leader, xu ly truc tiep
            receiveTokenBack(token);
            return;
        }

        String leaderUrl = allUrls.get(leaderId);
        if (leaderUrl == null) {
            log("❌ Khong tim thay URL cua leader!");
            return;
        }
        try {
            log("📤 Tra token ve leader: " + leaderId);
            restTemplate.postForObject(leaderUrl + "/api/receive-token-back", token, String.class);
        } catch (Exception e) {
            log("❌ Khong the lien lac leader " + leaderId + ": " + e.getMessage());
            log("⚠️ Token co the bi mat! Leader moi se tao token phuc hoi tu dong.");
        }
    }

    // ==================== DONG BO DU LIEU ====================

    /**
     * Leader broadcast du lieu vong hien tai den tat ca server song.
     * Sau moi vong, tat ca server deu co du lieu cua tat ca cac tram.
     */
    private void broadcastRoundData(List<Map<String, Object>> entries) {
        if (entries == null || entries.isEmpty()) return;

        log("📡 Dong bo du lieu vong #" + token.getTotalRounds() + " den tat ca server...");

        allUrls.forEach((id, url) -> {
            if (id.equals(myId)) return;
            if (!Boolean.TRUE.equals(peerStatus.get(id))) return;

            try {
                restTemplate.postForObject(url + "/api/sync-round", entries, String.class);
                log("✅ Dong bo thanh cong den " + id);
            } catch (Exception e) {
                log("⚠️ Dong bo that bai den " + id);
            }
        });
    }

    /**
     * Nhan du lieu dong bo tu leader.
     * Chi luu nhung ban ghi chua co trong DB cua minh.
     */
    public void receiveSyncData(List<Map<String, Object>> entries) {
        if (shuttingDown || entries == null) return;
        int saved = 0;
        for (Map<String, Object> e : entries) {
            try {
                int roundNumber = ((Number) e.get("roundNumber")).intValue();
                String stationName = (String) e.get("stationName");

                // Bo qua entry cua chinh minh (da luu roi)
                if (stationName != null && stationName.equals(myName)) continue;

                // Bo qua neu da ton tai trong DB (tranh trung lap)
                if (roundLogRepository.existsByRoundNumberAndStationName(roundNumber, stationName)) continue;

                int boarded = ((Number) e.get("passengersBoarded")).intValue();
                int alighted = ((Number) e.get("passengersAlighted")).intValue();
                double revenue = ((Number) e.get("revenue")).doubleValue();

                roundLogRepository.save(new RoundLog(roundNumber, boarded, alighted, revenue, stationName));
                saved++;
            } catch (Exception ex) {
                log("⚠️ Loi khi xu ly ban ghi dong bo: " + ex.getMessage());
            }
        }
        if (saved > 0) {
            log("📥 Dong bo: luu " + saved + " ban ghi moi tu cac tram khac");
        }
    }

    /**
     * Khi mot server hoi phuc, dong bo TOAN BO du lieu cho server do.
     * Goi async tu scheduler de khong block ping cycle.
     */
    private void syncDataToServer(String serverId) {
        if (shuttingDown) return;
        String url = allUrls.get(serverId);
        if (url == null) return;

        try {
            List<RoundLog> allLogs = roundLogRepository.findAll();
            if (allLogs.isEmpty()) return;

            List<Map<String, Object>> entries = new ArrayList<>();
            for (RoundLog rl : allLogs) {
                // Bo qua ban ghi SYSTEM_INIT
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
                log("📡 Da dong bo " + entries.size() + " ban ghi den " + serverId + " (hoi phuc)");
            }
        } catch (Exception e) {
            log("⚠️ Dong bo du lieu den " + serverId + " that bai: " + e.getMessage());
        }
    }

    // ==================== GETTERS ====================

    public String  getMyId()          { ensureInit(); return myId; }
    public boolean isLeader()         { return isLeader; }
    public String  getLeaderId()      { return leaderId; }
    public boolean isRunning()        { return isRunning; }
    public boolean isInTransit()      { return inTransit; }
    public BusToken getToken()        { return token; }
    public int     getCurrentEpoch()  { return currentEpoch; }
    public List<RoundLog> getDbLogs() { return roundLogRepository.findTop10ByOrderByTimestampDesc(); }
    public List<String>   getLogs()   { return new ArrayList<>(logs); }

    public Map<String, Boolean> getServerStatus() {
        ensureInit();
        Map<String, Boolean> result = new LinkedHashMap<>();
        allUrls.forEach((id, url) -> {
            if (id.equals(myId)) {
                result.put(id, true); // I am always alive
            } else {
                // Only TRUE if explicitly confirmed alive by ping
                boolean alive = Boolean.TRUE.equals(peerStatus.get(id));
                result.put(id, alive);
            }
        });
        return result;
    }

    private void log(String msg) {
        String entry = "[" + LocalDateTime.now().format(FMT) + "] " + msg;
        logs.add(entry);
        System.out.println(entry);
        if (logs.size() > 300) logs.remove(0);
    }
}
