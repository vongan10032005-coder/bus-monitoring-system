package com.busmonitor.server5.service;

import com.busmonitor.server5.model.BusToken;
import com.busmonitor.server5.model.RoundLog;
import com.busmonitor.server5.repository.RoundLogRepository;
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

    private final List<String> logs = new ArrayList<>();

    public void processToken(BusToken token) {
        // Xu ly hanh khach tai Tram 5 (tram cuoi)
        int boarded = new Random().nextInt(6) + 1;
        // Tram cuoi: nhieu nguoi xuong hon
        int alighted = Math.min(token.getCurrentPassengers(), new Random().nextInt(10) + 2);
        token.setCurrentPassengers(token.getCurrentPassengers() + boarded - alighted);
        double revenue = boarded * 8000.0;
        token.setTotalRevenue(token.getTotalRevenue() + revenue);
        token.setLastStation("server5-hang");

        // Luu vao DB rieng cua Server 5
        RoundLog rl = new RoundLog(token.getTotalRounds() + 1, boarded, alighted, revenue, "Tram 5 - Hang");
        roundLogRepository.save(rl);

        log(String.format("🏁 TRAM 5 (Hang - TRAM CUOI): +%d len, -%d xuong | Tren xe: %d nguoi | Thu: %.0f VND",
                boarded, alighted, token.getCurrentPassengers(), revenue));

        log("🔄 Hoan thanh 1 vong! Dang tra token ve Server 1 (Ngan)...");

        // Tram 5 luon tra token thang ve Server 1 (ket thuc 1 vong)
        sendBackToServer1(token);
    }

    private void sendBackToServer1(BusToken token) {
        int retry = 0;
        while (retry < 3) {
            try {
                restTemplate.postForObject(server1Url + "/api/receive-token-back", token, String.class);
                log("✅ Da tra token ve Server 1 thanh cong!");
                return;
            } catch (Exception e) {
                retry++;
                log("❌ Lan " + retry + ": Khong the lien lac Server 1 - " + e.getMessage());
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        log("🚨 CANH BAO: Khong the tra token ve Server 1 sau 3 lan thu!");
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
