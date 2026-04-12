package com.busmonitor.server2.service;

import com.busmonitor.server2.model.BusToken;
import com.busmonitor.server2.model.RoundLog;
import com.busmonitor.server2.repository.RoundLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Value("${server1.url}")
    private String server1Url;

    @Value("${server3.url}")
    private String server3Url;

    private final List<String> logs = new ArrayList<>();

    public void processToken(BusToken token) {
        // Xu ly hanh khach tai Tram 2
        int boarded = new Random().nextInt(8) + 1;
        int alighted = Math.min(token.getCurrentPassengers(), new Random().nextInt(5));
        token.setCurrentPassengers(token.getCurrentPassengers() + boarded - alighted);
        double revenue = boarded * 8000.0;
        token.setTotalRevenue(token.getTotalRevenue() + revenue);
        token.setLastStation("server2-nhi");

        // Luu vao DB rieng cua Server 2
        RoundLog rl = new RoundLog(token.getTotalRounds() + 1, boarded, alighted, revenue, "Tram 2 - Nhi");
        roundLogRepository.save(rl);

        log(String.format("🚉 TRAM 2 (Nhi): +%d len, -%d xuong | Tren xe: %d nguoi | Thu: %.0f VND",
                boarded, alighted, token.getCurrentPassengers(), revenue));

        // Gui token sang server tiep theo trong danh sach active
        forwardToken(token);
    }

    private void forwardToken(BusToken token) {
        List<String> active = new ArrayList<>(token.getActiveServers());
        int myIdx = active.indexOf("server2-nhi");

        // Tim server tiep theo sau minh trong danh sach active
        if (myIdx >= 0 && myIdx < active.size() - 1) {
            String next = active.get(myIdx + 1);
            String nextUrl = getUrlByName(next);
            try {
                log("📤 Gui token den " + next);
                restTemplate.postForObject(nextUrl + "/api/receive-token", token, String.class);
                return;
            } catch (Exception e) {
                log("❌ Loi gui den " + next + " -> thu server tiep theo");
                active.remove(myIdx + 1);
                token.setActiveServers(active);
                forwardToken(token); // De quy
                return;
            }
        }

        // Khong con server nao -> tra ve Server 1
        sendBackToServer1(token);
    }

    private void sendBackToServer1(BusToken token) {
        try {
            log("📤 Tra token ve Server 1 (Ngan)");
            restTemplate.postForObject(server1Url + "/api/receive-token-back", token, String.class);
        } catch (Exception e) {
            log("❌ Khong the lien lac Server 1: " + e.getMessage());
        }
    }

    private String getUrlByName(String name) {
        if ("server3-my".equals(name)) return server3Url;
        return server1Url; // fallback
    }

    private void log(String msg) {
        String entry = "[" + LocalDateTime.now().format(FMT) + "] " + msg;
        logs.add(entry);
        System.out.println(entry);
        if (logs.size() > 100) logs.remove(0);
    }

    public List<String> getLogs() { return new ArrayList<>(logs); }
    public List<RoundLog> getRecentLogs() { return roundLogRepository.findTop10ByOrderByTimestampDesc(); }
}
