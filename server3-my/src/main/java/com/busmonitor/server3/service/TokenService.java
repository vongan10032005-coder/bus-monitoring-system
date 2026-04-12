package com.busmonitor.server3.service;

import com.busmonitor.server3.model.BusToken;
import com.busmonitor.server3.model.RoundLog;
import com.busmonitor.server3.repository.RoundLogRepository;
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
public class TokenService {

    @Autowired
    private RoundLogRepository roundLogRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Value("${station.id}")
    private String myId;

    @Value("${station.order}")
    private int myOrder;

    @Value("${station.name}")
    private String myName;

    @Value("${server1.url:https://server1-ngan.onrender.com}")
    private String url1;
    @Value("${server2.url:https://server2-nhi.onrender.com}")
    private String url2;
    @Value("${server3.url:https://server3-my.onrender.com}")
    private String url3;
    @Value("${server4.url:https://server4-suong.onrender.com}")
    private String url4;
    @Value("${server5.url:https://server5-hang.onrender.com}")
    private String url5;

    private final Map<String, String> allUrls = new LinkedHashMap<>();
    private final Map<String, Integer> allOrders = new LinkedHashMap<>();
    private final Map<String, Boolean> serverStatus = new ConcurrentHashMap<>();

    private BusToken currentToken = new BusToken();
    private boolean isRunning = false;
    private boolean tokenInTransit = false;
    private boolean isLeader = false;
    private String currentLeaderId = "server1-ngan";
    private boolean initialized = false;

    private final List<String> logs = Collections.synchronizedList(new ArrayList<>());

    private void ensureInit() {
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
        serverStatus.put(myId, true);
        allUrls.forEach((id, url) -> {
            if (!id.equals(myId)) serverStatus.putIfAbsent(id, false);
        });
    }

    // ===== PING & LEADER ELECTION moi 10 giay =====
    @Scheduled(fixedDelay = 10000)
    public void pingAndElect() {
        ensureInit();
        allUrls.forEach((id, url) -> {
            if (id.equals(myId)) return;
            boolean wasAlive = serverStatus.getOrDefault(id, false);
            boolean alive = ping(url);
            serverStatus.put(id, alive);
            if (!wasAlive && alive)  log("✅ " + id + " hoi phuc! Them lai vao vong.");
            if (wasAlive  && !alive) log("❌ " + id + " mat ket noi! Loai khoi vong.");
        });
        electLeader();
    }

    private void electLeader() {
        String bestLeader = myId;
        int bestOrder = myOrder;
        for (Map.Entry<String, Boolean> e : serverStatus.entrySet()) {
            if (e.getValue() && !e.getKey().equals(myId)) {
                int ord = allOrders.getOrDefault(e.getKey(), 99);
                if (ord < bestOrder) { bestOrder = ord; bestLeader = e.getKey(); }
            }
        }
        boolean wasLeader = isLeader;
        isLeader = bestLeader.equals(myId);
        currentLeaderId = bestLeader;

        if (!wasLeader && isLeader) {
            log("👑 " + myId + " duoc bau lam LEADER! Server truoc bi mat.");
            if (!isRunning) {
                isRunning = true;
                tokenInTransit = false;
                new Thread(() -> {
                    try { Thread.sleep(2000); processAtMyStation(); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
                }).start();
            }
        } else if (wasLeader && !isLeader) {
            log("📤 Nhuong quyen leader cho " + bestLeader);
            isRunning = false;
        }
    }

    private boolean ping(String url) {
        try { restTemplate.getForObject(url + "/api/health", Map.class); return true; }
        catch (Exception e) { return false; }
    }

    // ===== START / STOP =====
    public String startSystem() {
        ensureInit();
        pingAndElect();
        isRunning = true;
        log("✅ He thong khoi dong! Leader: " + currentLeaderId + " | Toi la: " + myId);
        if (isLeader) {
            currentToken = new BusToken();
            new Thread(() -> {
                try { Thread.sleep(1000); processAtMyStation(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }).start();
        }
        return "Khoi dong! Leader: " + currentLeaderId;
    }

    public String stopSystem() {
        isRunning = false; tokenInTransit = false;
        log("🛑 He thong dung");
        return "Da dung!";
    }

    // ===== XU LY TOKEN =====
    public void processAtMyStation() {
        if (!isRunning || !isLeader || tokenInTransit) return;

        int boarded = new Random().nextInt(10) + 1;
        int alighted = Math.min(currentToken.getCurrentPassengers(), new Random().nextInt(5));
        currentToken.setCurrentPassengers(currentToken.getCurrentPassengers() + boarded - alighted);
        double revenue = boarded * 8000.0;
        currentToken.setTotalRevenue(currentToken.getTotalRevenue() + revenue);
        currentToken.setLastStation(myId);
        currentToken.setCurrentLeader(myId);

        RoundLog rl = new RoundLog(currentToken.getTotalRounds() + 1, boarded, alighted, revenue, myName);
        roundLogRepository.save(rl);
        log(String.format("🚉 %s (LEADER): +%d len, -%d xuong | %d nguoi | %.0f VND",
                myName, boarded, alighted, currentToken.getCurrentPassengers(), revenue));

        buildActiveList();
        tokenInTransit = true;
        forwardToken();
    }

    // Nhan token tu server truoc (transit)
    public void receiveToken(BusToken incoming) {
        ensureInit();
        // Update local state
        this.currentToken = incoming;

        int boarded = new Random().nextInt(8) + 1;
        int alighted = Math.min(currentToken.getCurrentPassengers(), new Random().nextInt(5));
        currentToken.setCurrentPassengers(currentToken.getCurrentPassengers() + boarded - alighted);
        double revenue = boarded * 8000.0;
        currentToken.setTotalRevenue(currentToken.getTotalRevenue() + revenue);
        currentToken.setLastStation(myId);

        RoundLog rl = new RoundLog(currentToken.getTotalRounds() + 1, boarded, alighted, revenue, myName);
        roundLogRepository.save(rl);
        log(String.format("🚉 %s: +%d len, -%d xuong | %d nguoi | %.0f VND",
                myName, boarded, alighted, currentToken.getCurrentPassengers(), revenue));

        // Forward to next in list
        forwardToken();
    }

    // Nhan token quay lai (tu server cuoi cung)
    public void receiveTokenBack(BusToken token) {
        this.currentToken = token;
        this.currentToken.setTotalRounds(token.getTotalRounds() + 1);
        this.tokenInTransit = false;
        this.currentToken.setCurrentLeader(myId);

        log("🏁 Vong #" + currentToken.getTotalRounds() + " hoan thanh! Tong: "
                + String.format("%.0f", currentToken.getTotalRevenue()) + " VND");

        if (isRunning && isLeader) {
            try { Thread.sleep(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            processAtMyStation();
        }
    }

    private void buildActiveList() {
        List<String> active = new ArrayList<>();
        // Add servers in order, skip myself (leader), skip dead ones
        allUrls.forEach((id, url) -> {
            if (!id.equals(myId) && serverStatus.getOrDefault(id, false)) {
                active.add(id);
            }
        });
        currentToken.setActiveServers(active);
    }

    private void forwardToken() {
        List<String> active = new ArrayList<>(currentToken.getActiveServers());

        // Find my position and get next server
        String myPos = currentToken.getLastStation();
        int myIdx = active.indexOf(myPos);

        String next = null;
        if (myIdx == -1) {
            // I'm leader, send to first in active list
            if (!active.isEmpty()) next = active.get(0);
        } else if (myIdx < active.size() - 1) {
            next = active.get(myIdx + 1);
        }
        // else: last in list -> return to leader

        if (next == null) {
            // No more servers or last -> return to leader
            String leaderUrl = allUrls.get(currentLeaderId);
            try {
                log("📤 Tra token ve leader: " + currentLeaderId);
                restTemplate.postForObject(leaderUrl + "/api/receive-token-back", currentToken, String.class);
            } catch (Exception e) {
                log("❌ Khong the tra token ve leader: " + e.getMessage());
                // Leader is dead, will be re-elected in next ping cycle
            }
            return;
        }

        String nextUrl = allUrls.get(next);
        try {
            log("📤 Gui token den " + next);
            restTemplate.postForObject(nextUrl + "/api/receive-token", currentToken, String.class);
        } catch (Exception e) {
            log("❌ Gui that bai den " + next + " -> bo qua");
            serverStatus.put(next, false);
            active.remove(next);
            currentToken.setActiveServers(active);
            currentToken.setLastStation(myPos); // keep my position
            forwardToken(); // try next
        }
    }

    // ===== GETTERS =====
    public BusToken getCurrentToken() { return currentToken; }
    public boolean isRunning() { return isRunning; }
    public boolean isLeader() { return isLeader; }
    public String getCurrentLeaderId() { return currentLeaderId; }
    public boolean isTokenInTransit() { return tokenInTransit; }
    public String getMyId() { ensureInit(); return myId; }
    public Map<String, Boolean> getServerStatus() {
        ensureInit();
        Map<String, Boolean> s = new LinkedHashMap<>(serverStatus);
        s.put(myId, true);
        return s;
    }
    public List<String> getLogs() { return new ArrayList<>(logs); }
    public List<RoundLog> getRecentLogs() { return roundLogRepository.findTop10ByOrderByTimestampDesc(); }

    private void log(String msg) {
        String e = "[" + LocalDateTime.now().format(FMT) + "] " + msg;
        logs.add(e); System.out.println(e);
        if (logs.size() > 200) logs.remove(0);
    }
}
