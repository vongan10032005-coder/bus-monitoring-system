package com.busmonitor.server1.service;

import com.busmonitor.server1.model.BusToken;
import com.busmonitor.server1.model.RoundLog;
import com.busmonitor.server1.repository.RoundLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TokenService {

    @Autowired
    private RoundLogRepository roundLogRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${server2.url}")
    private String server2Url;

    @Value("${server3.url}")
    private String server3Url;

    @Value("${server4.url}")
    private String server4Url;

    @Value("${server5.url}")
    private String server5Url;

    // Trang thai he thong
    private BusToken currentToken = new BusToken();
    private boolean isRunning = false;
    private boolean tokenInTransit = false;

    // Trang thai tung server (ten -> true/false)
    private final Map<String, Boolean> serverStatus = new LinkedHashMap<>();
    private final Map<String, String> serverUrls = new LinkedHashMap<>();

    // Log he thong
    private final List<String> systemLogs = new ArrayList<>();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ==================== KHOI DONG / DUNG ====================

    public String startSystem() {
        if (isRunning) return "He thong dang chay roi!";

        initServerMap();
        isRunning = true;
        tokenInTransit = false;
        currentToken = new BusToken();

        log("✅ He thong khoi dong tai Server 1 (Ngan) - Tram dieu phoi trung tam");
        log("🔄 Bat dau kiem tra trang thai cac server...");

        checkAllServers();
        buildActiveList();

        log("📋 Danh sach server hoat dong: " + currentToken.getActiveServers());

        // Chay vong dau tien
        new Thread(this::startFirstRound).start();

        return "He thong da khoi dong!";
    }

    public String stopSystem() {
        isRunning = false;
        tokenInTransit = false;
        log("🛑 He thong da dung boi nguoi dung");
        return "He thong da dung!";
    }

    private void initServerMap() {
        serverUrls.clear();
        serverUrls.put("server2-nhi", server2Url);
        serverUrls.put("server3-my", server3Url);
        serverUrls.put("server4-suong", server4Url);
        serverUrls.put("server5-hang", server5Url);

        serverStatus.clear();
        serverUrls.forEach((name, url) -> serverStatus.put(name, false));
    }

    // ==================== KIEM TRA SERVER ====================

    // Tu dong kiem tra suc khoe moi 15 giay
    @Scheduled(fixedDelay = 15000)
    public void checkAllServers() {
        serverUrls.forEach((name, url) -> {
            boolean wasUp = serverStatus.getOrDefault(name, false);
            boolean isUp = pingServer(url);

            serverStatus.put(name, isUp);

            if (!wasUp && isUp) {
                log("✅ Server " + name + " da hoi phuc! Them lai vao vong tron.");
            } else if (wasUp && !isUp) {
                log("❌ Server " + name + " khong phan hoi! Loai khoi vong tron tam thoi.");
            }
        });

        buildActiveList();
    }

    private boolean pingServer(String url) {
        try {
            restTemplate.getForObject(url + "/api/health", Map.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void buildActiveList() {
        List<String> active = new ArrayList<>();
        serverUrls.forEach((name, url) -> {
            if (serverStatus.getOrDefault(name, false)) {
                active.add(name);
            }
        });
        currentToken.setActiveServers(active);
    }

    // ==================== XU LY TOKEN ====================

    private void startFirstRound() {
        try {
            Thread.sleep(2000);
            if (isRunning) processAtStation1();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void processAtStation1() {
        if (!isRunning || tokenInTransit) return;

        // Xu ly hanh khach tai Tram 1
        int boarded = new Random().nextInt(10) + 1;
        int alighted = Math.min(currentToken.getCurrentPassengers(), new Random().nextInt(5));
        currentToken.setCurrentPassengers(currentToken.getCurrentPassengers() + boarded - alighted);
        double revenue = boarded * 8000.0;
        currentToken.setTotalRevenue(currentToken.getTotalRevenue() + revenue);
        currentToken.setLastStation("server1-ngan");

        // Luu vao DB cua Server 1
        RoundLog rl = new RoundLog(currentToken.getTotalRounds() + 1, boarded, alighted, revenue, "Tram 1 - Ngan");
        roundLogRepository.save(rl);

        log(String.format("🚉 TRAM 1 (Ngan): +%d len, -%d xuong | Tren xe: %d nguoi | Thu: %.0f VND",
                boarded, alighted, currentToken.getCurrentPassengers(), revenue));

        // Cap nhat danh sach server active truoc khi gui
        checkAllServers();

        // Gui token sang server tiep theo
        tokenInTransit = true;
        forwardToken();
    }

    private void forwardToken() {
        List<String> active = new ArrayList<>(currentToken.getActiveServers());

        if (active.isEmpty()) {
            // Khong co server nao - token quay lai ngay Server 1
            log("⚠️ Khong co server trung gian nao online! Token quay thang ve Server 1.");
            receiveTokenBack(currentToken);
            return;
        }

        // Gui den server dau tien trong danh sach active
        String nextServer = active.get(0);
        String nextUrl = serverUrls.get(nextServer);

        try {
            log("📤 Gui token den " + nextServer + " (" + nextUrl + ")");
            restTemplate.postForObject(nextUrl + "/api/receive-token", currentToken, String.class);
        } catch (Exception e) {
            log("❌ Loi gui den " + nextServer + ": " + e.getMessage());
            // Danh dau server nay chet, thu server tiep theo
            serverStatus.put(nextServer, false);
            active.remove(0);
            currentToken.setActiveServers(active);
            forwardToken(); // Thu lai voi danh sach moi
        }
    }

    // ==================== NHAN TOKEN TRA VE ====================

    public void receiveTokenBack(BusToken token) {
        this.currentToken = token;
        this.currentToken.setTotalRounds(token.getTotalRounds() + 1);
        this.tokenInTransit = false;

        log("🏁 Token quay ve Server 1! Hoan thanh vong #" + currentToken.getTotalRounds()
                + " | Tong doanh thu: " + String.format("%.0f", currentToken.getTotalRevenue()) + " VND");

        // Tiep tuc neu he thong dang chay
        if (isRunning) {
            try {
                Thread.sleep(3000); // Nghi 3 giay giua cac vong
                processAtStation1();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ==================== LOG ====================

    private void log(String message) {
        String entry = "[" + LocalDateTime.now().format(FMT) + "] " + message;
        systemLogs.add(entry);
        System.out.println(entry);
        if (systemLogs.size() > 200) systemLogs.remove(0);
    }

    // ==================== GETTERS ====================

    public BusToken getCurrentToken() { return currentToken; }
    public boolean isRunning() { return isRunning; }
    public boolean isTokenInTransit() { return tokenInTransit; }
    public List<String> getSystemLogs() { return new ArrayList<>(systemLogs); }
    public Map<String, Boolean> getServerStatus() { return new LinkedHashMap<>(serverStatus); }
    public List<RoundLog> getRecentLogs() { return roundLogRepository.findTop10ByOrderByTimestampDesc(); }
}
