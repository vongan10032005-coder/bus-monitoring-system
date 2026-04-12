package com.busmonitor.server3.controller;

import com.busmonitor.server3.model.BusToken;
import com.busmonitor.server3.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class StationController {

    @Autowired
    private TokenService tokenService;

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "UP");
        res.put("server", "server3-my");
        res.put("station", "Tram 3 - My");
        res.put("timestamp", new Date().toString());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/api/receive-token")
    public ResponseEntity<String> receiveToken(@RequestBody BusToken token) {
        new Thread(() -> tokenService.processToken(token)).start();
        return ResponseEntity.ok("Token received at Server 3 - My");
    }

    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("server", "server3-my");
        data.put("station", "Tram 3 - My");
        data.put("logs", tokenService.getLogs());
        data.put("recentDB", tokenService.getRecentLogs());
        return ResponseEntity.ok(data);
    }

    @GetMapping("/")
    public String dashboard() {
        return """
<!DOCTYPE html><html lang="vi"><head><meta charset="UTF-8">
<title>Server 3 - My</title>
<style>body{background:#0f172a;color:#e2e8f0;font-family:'Courier New',monospace;padding:20px;}
h1{color:#a78bfa;}.log{background:#1e293b;padding:16px;border-radius:8px;height:400px;overflow-y:auto;font-size:0.85rem;line-height:1.7;}
.badge{background:#a78bfa;color:white;padding:4px 12px;border-radius:20px;font-size:0.8rem;}</style>
</head><body>
<h1>🚉 SERVER 3 — TRAM MY</h1>
<p><span class="badge">ONLINE</span> Tram trung gian | Nhan token tu Tram 2, chuyen di Tram 4</p><br/>
<div class="log" id="logs">Loading...</div>
<script>
async function refresh(){const r=await fetch('/api/status');const d=await r.json();
const logs=d.logs||[];document.getElementById('logs').innerHTML=logs.slice(-50).reverse().map(l=>'<div>'+l+'</div>').join('');}
refresh();setInterval(refresh,3000);
</script></body></html>
""";
    }
}
