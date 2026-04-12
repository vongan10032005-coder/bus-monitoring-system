package com.busmonitor.server1.controller;

import com.busmonitor.server1.model.BusToken;
import com.busmonitor.server1.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class StationController {

    @Autowired
    private TokenService tokenService;

    // ==================== HEALTH CHECK ====================
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "UP");
        res.put("server", "server1-ngan");
        res.put("station", "Tram 1 - Ngan (Dieu phoi trung tam)");
        res.put("timestamp", new Date().toString());
        return ResponseEntity.ok(res);
    }

    // ==================== DIEU KHIEN ====================
    @PostMapping("/api/start")
    public ResponseEntity<String> start() {
        return ResponseEntity.ok(tokenService.startSystem());
    }

    @PostMapping("/api/stop")
    public ResponseEntity<String> stop() {
        return ResponseEntity.ok(tokenService.stopSystem());
    }

    // ==================== NHAN TOKEN TU SERVER 5 ====================
    @PostMapping("/api/receive-token-back")
    public ResponseEntity<String> receiveTokenBack(@RequestBody BusToken token) {
        new Thread(() -> tokenService.receiveTokenBack(token)).start();
        return ResponseEntity.ok("Token received back at Server 1");
    }

    // ==================== TRANG THAI ====================
    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("isRunning", tokenService.isRunning());
        data.put("tokenInTransit", tokenService.isTokenInTransit());
        data.put("currentToken", tokenService.getCurrentToken());
        data.put("serverStatus", tokenService.getServerStatus());
        data.put("recentDBLogs", tokenService.getRecentLogs());
        data.put("systemLogs", tokenService.getSystemLogs());
        return ResponseEntity.ok(data);
    }

    // ==================== DASHBOARD HTML ====================
    @GetMapping("/")
    public String dashboard() {
        return """
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Bus Monitor - Server 1 (Ngan)</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { background: #0f172a; color: #e2e8f0; font-family: 'Courier New', monospace; }
  .header { background: #1e293b; padding: 20px; border-bottom: 2px solid #3b82f6; }
  .header h1 { color: #3b82f6; font-size: 1.5rem; }
  .header p { color: #94a3b8; font-size: 0.85rem; margin-top: 4px; }
  .container { max-width: 1200px; margin: 0 auto; padding: 20px; }
  .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; margin-bottom: 20px; }
  .card { background: #1e293b; border-radius: 8px; padding: 16px; border: 1px solid #334155; }
  .card-title { color: #94a3b8; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1px; }
  .card-value { color: #f8fafc; font-size: 2rem; font-weight: bold; margin-top: 8px; }
  .card-value.green { color: #22c55e; }
  .card-value.red { color: #ef4444; }
  .card-value.yellow { color: #f59e0b; }
  .btn { padding: 10px 24px; border: none; border-radius: 6px; cursor: pointer; font-size: 1rem; font-weight: bold; margin: 4px; }
  .btn-start { background: #22c55e; color: white; }
  .btn-stop { background: #ef4444; color: white; }
  .servers { display: grid; grid-template-columns: repeat(5, 1fr); gap: 10px; margin-bottom: 20px; }
  .server-card { background: #1e293b; border-radius: 8px; padding: 12px; text-align: center; border: 2px solid #334155; }
  .server-card.online { border-color: #22c55e; }
  .server-card.offline { border-color: #ef4444; opacity: 0.6; }
  .server-name { font-size: 0.8rem; color: #94a3b8; }
  .server-status { font-size: 1.2rem; margin-top: 6px; }
  .log-box { background: #0f172a; border: 1px solid #334155; border-radius: 8px; padding: 16px; height: 300px; overflow-y: auto; font-size: 0.8rem; line-height: 1.6; }
  .log-line { padding: 2px 0; border-bottom: 1px solid #1e293b; }
  .section-title { color: #3b82f6; font-size: 1rem; font-weight: bold; margin: 20px 0 10px; }
  .ring { display: flex; justify-content: center; align-items: center; gap: 10px; flex-wrap: wrap; margin: 20px 0; }
  .station { background: #1e293b; border: 2px solid #3b82f6; border-radius: 50%; width: 80px; height: 80px; display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 0.7rem; text-align: center; }
  .station.active { border-color: #22c55e; box-shadow: 0 0 10px #22c55e44; }
  .station.dead { border-color: #ef4444; opacity: 0.5; }
  .arrow { color: #3b82f6; font-size: 1.5rem; }
</style>
</head>
<body>
<div class="header">
  <h1>🚌 BUS MONITORING SYSTEM — SERVER 1 (NGAN)</h1>
  <p>Tram dieu phoi trung tam | He thong giam sat vong tron ao</p>
</div>
<div class="container">
  <div style="margin-bottom:16px;">
    <button class="btn btn-start" onclick="startSystem()">▶ KHOI DONG</button>
    <button class="btn btn-stop" onclick="stopSystem()">⏹ DUNG</button>
    <span id="msg" style="color:#f59e0b;margin-left:10px;"></span>
  </div>

  <div class="grid">
    <div class="card">
      <div class="card-title">Trang thai</div>
      <div class="card-value" id="status">--</div>
    </div>
    <div class="card">
      <div class="card-title">So vong hoan thanh</div>
      <div class="card-value green" id="rounds">0</div>
    </div>
    <div class="card">
      <div class="card-title">Hanh khach tren xe</div>
      <div class="card-value yellow" id="passengers">0</div>
    </div>
    <div class="card">
      <div class="card-title">Tong doanh thu</div>
      <div class="card-value" id="revenue">0 VND</div>
    </div>
  </div>

  <div class="section-title">🔵 VONG TRON AO — TRANG THAI CAC TRAM</div>
  <div class="ring">
    <div class="station" id="s1">🏢<br/>Tram 1<br/>Ngan</div>
    <div class="arrow">→</div>
    <div class="station" id="s2">🚉<br/>Tram 2<br/>Nhi</div>
    <div class="arrow">→</div>
    <div class="station" id="s3">🚉<br/>Tram 3<br/>My</div>
    <div class="arrow">→</div>
    <div class="station" id="s4">🚉<br/>Tram 4<br/>Suong</div>
    <div class="arrow">→</div>
    <div class="station" id="s5">🚉<br/>Tram 5<br/>Hang</div>
    <div class="arrow">↩</div>
  </div>

  <div class="section-title">📡 LICH SU HOAT DONG</div>
  <div class="log-box" id="logs">Chua co log...</div>
</div>

<script>
async function fetchStatus() {
  try {
    const r = await fetch('/api/status');
    const d = await r.json();

    document.getElementById('status').textContent = d.isRunning ? '🟢 DANG CHAY' : '🔴 DA DUNG';
    document.getElementById('status').className = 'card-value ' + (d.isRunning ? 'green' : 'red');
    document.getElementById('rounds').textContent = d.currentToken?.totalRounds ?? 0;
    document.getElementById('passengers').textContent = d.currentToken?.currentPassengers ?? 0;
    document.getElementById('revenue').textContent = (d.currentToken?.totalRevenue ?? 0).toLocaleString('vi-VN') + ' VND';

    // Cap nhat trang thai tram
    const ss = d.serverStatus || {};
    document.getElementById('s1').className = 'station active';
    document.getElementById('s2').className = 'station ' + (ss['server2-nhi'] ? 'active' : 'dead');
    document.getElementById('s3').className = 'station ' + (ss['server3-my'] ? 'active' : 'dead');
    document.getElementById('s4').className = 'station ' + (ss['server4-suong'] ? 'active' : 'dead');
    document.getElementById('s5').className = 'station ' + (ss['server5-hang'] ? 'active' : 'dead');

    // Logs
    const logs = d.systemLogs || [];
    const box = document.getElementById('logs');
    box.innerHTML = logs.slice(-50).reverse().map(l => '<div class="log-line">' + l + '</div>').join('');
  } catch(e) {
    console.error(e);
  }
}

async function startSystem() {
  document.getElementById('msg').textContent = 'Dang khoi dong...';
  await fetch('/api/start', {method:'POST'});
  document.getElementById('msg').textContent = 'Da khoi dong!';
  setTimeout(()=>document.getElementById('msg').textContent='', 3000);
}

async function stopSystem() {
  await fetch('/api/stop', {method:'POST'});
  document.getElementById('msg').textContent = 'Da dung!';
  setTimeout(()=>document.getElementById('msg').textContent='', 3000);
}

fetchStatus();
setInterval(fetchStatus, 3000);
</script>
</body>
</html>
""";
    }
}
