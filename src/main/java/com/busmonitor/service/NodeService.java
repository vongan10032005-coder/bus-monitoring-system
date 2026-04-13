package com.busmonitor.service;

import com.busmonitor.model.BusToken;
import com.busmonitor.model.RoundLog;
import com.busmonitor.repository.RoundLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NodeService {

    @Autowired
    private RoundLogRepository roundLogRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

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

    // State
    private BusToken token       = new BusToken();
    private boolean  isRunning   = false;
    private boolean  inTransit   = false;
    private boolean  isLeader    = false;
    private String   leaderId    = "";
    private boolean  initialized = false;

    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());

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

    // ==================== PING & LEADER ELECTION ====================

    @Scheduled(fixedDelay = 10000)
    public void pingAndElect() {
        ensureInit();
        // Ping all peers
        allUrls.forEach((id, url) -> {
            if (id.equals(myId)) return;
            boolean wasAlive = peerStatus.getOrDefault(id, false);
            boolean alive    = ping(url);
            peerStatus.put(id, alive);
            if (!wasAlive && alive)  log("✅ " + id + " hoi phuc! Them lai vao vong.");
            if (wasAlive  && !alive) log("❌ " + id + " mat ket noi! Loai khoi vong.");
        });
        electLeader();
    }

    private void electLeader() {
        // Find lowest-order alive server = leader
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
            log("👑 " + myId + " duoc bau lam LEADER!");
            if (!isRunning) {
                isRunning = true;
                inTransit = false;
                token = new BusToken();
                new Thread(() -> {
                    try { Thread.sleep(2000); processAsLeader(); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                }).start();
            }
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
            token = new BusToken();
            new Thread(() -> {
                try { Thread.sleep(500); processAsLeader(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }).start();
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

    // Leader starts a new round
    private void processAsLeader() {
        if (!isRunning || !isLeader || inTransit) return;

        int boarded  = new Random().nextInt(10) + 1;
        int alighted = Math.min(token.getCurrentPassengers(), new Random().nextInt(5));
        token.setCurrentPassengers(token.getCurrentPassengers() + boarded - alighted);
        double revenue = boarded * 8000.0;
        token.setTotalRevenue(token.getTotalRevenue() + revenue);
        token.setLastStation(myId);
        token.setCurrentLeader(myId);

        // Save to MY DB
        roundLogRepository.save(new RoundLog(
                token.getTotalRounds() + 1, boarded, alighted, revenue, myName));

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

    // Non-leader receives token, processes, forwards
    public void receiveToken(BusToken incoming) {
        ensureInit();
        this.token = incoming;

        int boarded  = new Random().nextInt(8) + 1;
        int alighted = Math.min(token.getCurrentPassengers(), new Random().nextInt(5));
        token.setCurrentPassengers(token.getCurrentPassengers() + boarded - alighted);
        double revenue = boarded * 8000.0;
        token.setTotalRevenue(token.getTotalRevenue() + revenue);
        token.setLastStation(myId);

        roundLogRepository.save(new RoundLog(
                token.getTotalRounds() + 1, boarded, alighted, revenue, myName));

        log(String.format("🚉 %s: +%d len, -%d xuong | %d nguoi | %.0f VND",
                myName, boarded, alighted, token.getCurrentPassengers(), revenue));

        // Forward to next alive server
        List<String> active = new ArrayList<>(token.getActiveServers());
        sendToNext(myId, active);
    }

    // Leader receives token back after full round
    public void receiveTokenBack(BusToken incoming) {
        this.token = incoming;
        this.token.setTotalRounds(incoming.getTotalRounds() + 1);
        this.inTransit = false;

        log("🏁 Vong #" + token.getTotalRounds() + " hoan thanh! Tong: "
                + String.format("%.0f", token.getTotalRevenue()) + " VND");

        if (isRunning && isLeader) {
            try { Thread.sleep(3000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            processAsLeader();
        }
    }

    // Send token to next server in active list after 'fromId'
    private void sendToNext(String fromId, List<String> active) {
        int idx  = active.indexOf(fromId);
        String next = null;

        if (fromId.equals(leaderId)) {
            // Leader just processed -> send to first active peer
            next = active.isEmpty() ? null : active.get(0);
        } else if (idx >= 0 && idx < active.size() - 1) {
            next = active.get(idx + 1);
        }
        // else: last in list -> return to leader

        if (next == null) {
            // Return to leader
            String leaderUrl = allUrls.get(leaderId);
            if (leaderUrl == null) { log("❌ Khong tim thay URL cua leader!"); return; }
            try {
                log("📤 Tra token ve leader: " + leaderId);
                restTemplate.postForObject(leaderUrl + "/api/receive-token-back", token, String.class);
            } catch (Exception e) {
                log("❌ Khong the lien lac leader " + leaderId + ": " + e.getMessage());
            }
            return;
        }

        String nextUrl = allUrls.get(next);
        try {
            log("📤 Gui token den " + next);
            restTemplate.postForObject(nextUrl + "/api/receive-token", token, String.class);
        } catch (Exception e) {
            log("❌ Gui that bai den " + next + " -> bo qua, thu tiep");
            peerStatus.put(next, false);
            active.remove(next);
            token.setActiveServers(active);
            sendToNext(fromId, active); // retry with updated list
        }
    }

    // ==================== GETTERS ====================

    public String  getMyId()          { ensureInit(); return myId; }
    public boolean isLeader()         { return isLeader; }
    public String  getLeaderId()      { return leaderId; }
    public boolean isRunning()        { return isRunning; }
    public boolean isInTransit()      { return inTransit; }
    public BusToken getToken()        { return token; }
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
