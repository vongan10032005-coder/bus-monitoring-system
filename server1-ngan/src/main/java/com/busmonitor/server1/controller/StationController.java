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

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> res = new HashMap<>();
        res.put("status", "UP");
        res.put("server", "server1-ngan");
        res.put("station", "Tram 1 - Ngan");
        res.put("timestamp", new Date().toString());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/api/start")
    public ResponseEntity<String> start() {
        return ResponseEntity.ok(tokenService.startSystem());
    }

    @PostMapping("/api/stop")
    public ResponseEntity<String> stop() {
        return ResponseEntity.ok(tokenService.stopSystem());
    }

    @PostMapping("/api/receive-token-back")
    public ResponseEntity<String> receiveTokenBack(@RequestBody BusToken token) {
        new Thread(() -> tokenService.receiveTokenBack(token)).start();
        return ResponseEntity.ok("OK");
    }

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

    @GetMapping("/")
    public String dashboard() {
        return getDashboardHtml();
    }

    private String getDashboardHtml() {
        return "<!DOCTYPE html><html lang='vi'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1.0'>"
            + "<title>Giam Sat Vong Tron Ao</title>"
            + "<link href='https://fonts.googleapis.com/css2?family=Share+Tech+Mono&family=Exo+2:wght@300;400;600;700&display=swap' rel='stylesheet'>"
            + "<style>"
            + ":root{--bg:#080c14;--bg2:#0d1520;--bg3:#111d2e;--border:#1a3050;--cyan:#00d4ff;--green:#00ff88;--red:#ff3d5a;--yellow:#ffd24d;--purple:#a259ff;--text:#c8dff0;--dim:#4a6880;}"
            + "*{box-sizing:border-box;margin:0;padding:0;}"
            + "body{background:var(--bg);color:var(--text);font-family:'Exo 2',sans-serif;min-height:100vh;}"
            + ".header{background:var(--bg2);border-bottom:1px solid var(--border);padding:12px 24px;display:flex;align-items:center;justify-content:space-between;}"
            + ".htitle{font-size:1rem;font-weight:700;color:var(--cyan);letter-spacing:2px;text-transform:uppercase;}"
            + ".hsub{font-size:0.72rem;color:var(--dim);margin-top:2px;font-family:'Share Tech Mono',monospace;}"
            + ".hstat{display:flex;align-items:center;gap:8px;font-size:0.82rem;}"
            + "@keyframes blink{0%,100%{opacity:1}50%{opacity:.3}}"
            + ".dot{width:9px;height:9px;border-radius:50%;background:var(--green);box-shadow:0 0 8px var(--green);animation:blink 1.4s infinite;}"
            + ".dot.off{background:var(--red);box-shadow:0 0 8px var(--red);animation:none;}"
            + ".main{padding:14px 18px;display:grid;grid-template-columns:1fr 1fr 1fr;gap:12px;}"
            + ".card{background:var(--bg2);border:1px solid var(--border);border-radius:10px;padding:14px;}"
            + ".ctitle{font-size:0.65rem;font-weight:600;letter-spacing:2px;text-transform:uppercase;color:var(--dim);margin-bottom:12px;display:flex;align-items:center;gap:6px;}"
            + ".ctitle::before{content:'';display:inline-block;width:3px;height:11px;background:var(--cyan);border-radius:2px;}"
            + ".sgrid{display:grid;grid-template-columns:1fr 1fr;gap:8px;}"
            + ".sbox{background:var(--bg3);border:1px solid var(--border);border-radius:7px;padding:10px 12px;}"
            + ".slabel{font-size:0.65rem;color:var(--dim);margin-bottom:4px;font-family:'Share Tech Mono',monospace;}"
            + ".sval{font-size:1.45rem;font-weight:700;font-family:'Share Tech Mono',monospace;line-height:1.1;}"
            + ".c{color:var(--cyan);} .g{color:var(--green);} .y{color:var(--yellow);} .p{color:var(--purple);} .r{color:var(--red);}"
            + ".ring-wrap{display:flex;justify-content:center;padding:6px 0;}"
            + ".srv-list{display:flex;flex-direction:column;gap:7px;}"
            + ".srv-row{display:flex;align-items:center;justify-content:space-between;background:var(--bg3);border:1px solid var(--border);border-radius:7px;padding:9px 12px;transition:border-color .3s;}"
            + ".srv-row.ok{border-color:#00ff8830;} .srv-row.err{border-color:#ff3d5a30;}"
            + ".srv-name{font-size:0.82rem;font-weight:600;}"
            + ".badges{display:flex;gap:5px;align-items:center;}"
            + ".badge{font-size:0.6rem;font-weight:700;letter-spacing:1px;padding:2px 7px;border-radius:3px;text-transform:uppercase;}"
            + ".badge.ok{background:#00ff8815;color:var(--green);border:1px solid #00ff8835;}"
            + ".badge.err{background:#ff3d5a15;color:var(--red);border:1px solid #ff3d5a35;}"
            + ".badge.st{background:#ffd24d15;color:var(--yellow);border:1px solid #ffd24d35;}"
            + ".badge.db{background:#a259ff15;color:var(--purple);border:1px solid #a259ff35;}"
            + ".logbox{background:var(--bg);border:1px solid var(--border);border-radius:6px;padding:9px 11px;height:190px;overflow-y:auto;font-family:'Share Tech Mono',monospace;font-size:0.7rem;line-height:1.75;}"
            + ".logbox::-webkit-scrollbar{width:3px;} .logbox::-webkit-scrollbar-thumb{background:var(--border);border-radius:2px;}"
            + ".ll{padding:1px 0;border-bottom:1px solid #ffffff07;}"
            + ".ll.ok{color:var(--green);} .ll.er{color:var(--red);} .ll.in{color:var(--cyan);} .ll.wn{color:var(--yellow);} .ll.dm{color:var(--dim);}"
            + ".span2{grid-column:1/3;}"
            + ".htbl{width:100%;border-collapse:collapse;font-size:0.78rem;}"
            + ".htbl th{text-align:left;padding:7px 10px;font-size:0.62rem;letter-spacing:1px;text-transform:uppercase;color:var(--dim);border-bottom:1px solid var(--border);font-weight:600;}"
            + ".htbl td{padding:7px 10px;border-bottom:1px solid #ffffff05;font-family:'Share Tech Mono',monospace;}"
            + ".htbl tr:hover td{background:var(--bg3);}"
            + ".tpath{display:flex;align-items:center;gap:5px;flex-wrap:wrap;margin-top:10px;}"
            + ".tst{font-size:0.68rem;padding:3px 9px;border-radius:20px;border:1px solid var(--border);color:var(--dim);font-family:'Share Tech Mono',monospace;}"
            + ".tst.cur{background:#00d4ff18;border-color:var(--cyan);color:var(--cyan);}"
            + ".tst.dead{opacity:.3;text-decoration:line-through;}"
            + ".sep{color:var(--border);font-size:0.75rem;}"
            + "</style></head><body>"
            + "<div class='header'>"
            + "<div><div class='htitle'>&#128202; He Thong Giam Sat Vong Tron Ao</div>"
            + "<div class='hsub'>Bus Monitoring System &mdash; Dien Toan Dam May &mdash; <span id='clk'>--:--:--</span></div></div>"
            + "<div class='hstat'><div class='dot' id='mdot'></div><span id='mtxt' style='color:var(--dim)'>Dang ket noi...</span></div>"
            + "</div>"
            + "<div class='main'>"
            // Card 1: Stats
            + "<div class='card'><div class='ctitle'>Thong Ke He Thong</div>"
            + "<div class='sgrid'>"
            + "<div class='sbox'><div class='slabel'>So Vong Hoan Thanh</div><div class='sval c' id='s-rounds'>--</div></div>"
            + "<div class='sbox'><div class='slabel'>Hanh Khach Tren Xe</div><div class='sval g' id='s-pass'>--</div></div>"
            + "<div class='sbox'><div class='slabel'>Tong Doanh Thu</div><div class='sval y' id='s-rev'>--</div></div>"
            + "<div class='sbox'><div class='slabel'>Tram Hoat Dong</div><div class='sval p' id='s-act'>--</div></div>"
            + "</div>"
            + "<div style='margin-top:12px'><div class='slabel' style='margin-bottom:6px'>Vi Tri Token</div>"
            + "<div class='tpath' id='tpath'>--</div></div></div>"
            // Card 2: Ring
            + "<div class='card'><div class='ctitle'>Vong Tron Ao</div><div class='ring-wrap'>"
            + "<svg viewBox='0 0 260 260' width='100%' xmlns='http://www.w3.org/2000/svg'>"
            + "<circle cx='130' cy='130' r='90' fill='none' stroke='#1a3050' stroke-width='1' stroke-dasharray='3 5'/>"
            + "<g id='n1'><circle cx='130' cy='40' r='20' fill='#0d1520' stroke='#1a3050' stroke-width='1.5'/>"
            + "<text x='130' y='36' text-anchor='middle' font-size='8' fill='#4a6880' font-family='Exo 2'>Tram 1</text>"
            + "<text x='130' y='47' text-anchor='middle' font-size='9' font-weight='700' fill='#c8dff0' font-family='Exo 2'>Ngan</text></g>"
            + "<g id='n2'><circle cx='218' cy='93' r='20' fill='#0d1520' stroke='#1a3050' stroke-width='1.5'/>"
            + "<text x='218' y='89' text-anchor='middle' font-size='8' fill='#4a6880' font-family='Exo 2'>Tram 2</text>"
            + "<text x='218' y='100' text-anchor='middle' font-size='9' font-weight='700' fill='#c8dff0' font-family='Exo 2'>Nhi</text></g>"
            + "<g id='n3'><circle cx='187' cy='197' r='20' fill='#0d1520' stroke='#1a3050' stroke-width='1.5'/>"
            + "<text x='187' y='193' text-anchor='middle' font-size='8' fill='#4a6880' font-family='Exo 2'>Tram 3</text>"
            + "<text x='187' y='204' text-anchor='middle' font-size='9' font-weight='700' fill='#c8dff0' font-family='Exo 2'>My</text></g>"
            + "<g id='n4'><circle cx='73' cy='197' r='20' fill='#0d1520' stroke='#1a3050' stroke-width='1.5'/>"
            + "<text x='73' y='193' text-anchor='middle' font-size='8' fill='#4a6880' font-family='Exo 2'>Tram 4</text>"
            + "<text x='73' y='204' text-anchor='middle' font-size='9' font-weight='700' fill='#c8dff0' font-family='Exo 2'>Suong</text></g>"
            + "<g id='n5'><circle cx='42' cy='93' r='20' fill='#0d1520' stroke='#1a3050' stroke-width='1.5'/>"
            + "<text x='42' y='89' text-anchor='middle' font-size='8' fill='#4a6880' font-family='Exo 2'>Tram 5</text>"
            + "<text x='42' y='100' text-anchor='middle' font-size='9' font-weight='700' fill='#c8dff0' font-family='Exo 2'>Hang</text></g>"
            + "<circle id='tdot' cx='130' cy='40' r='6' fill='#ffd24d'>"
            + "<animate attributeName='opacity' values='1;0.2;1' dur='1s' repeatCount='indefinite'/>"
            + "<animate attributeName='r' values='6;9;6' dur='1s' repeatCount='indefinite'/>"
            + "</circle></svg></div></div>"
            // Card 3: Server status
            + "<div class='card'><div class='ctitle'>Trang Thai Server</div>"
            + "<div class='srv-list' id='srvlist'><div class='srv-row'><span class='srv-name'>Dang tai...</span></div></div></div>"
            // Card 4: Logs
            + "<div class='card span2'><div class='ctitle'>Nhat Ky Su Kien</div>"
            + "<div class='logbox' id='logbox'>Dang ket noi...</div></div>"
            // Card 5: History
            + "<div class='card'><div class='ctitle'>Lich Su Gan Nhat</div>"
            + "<table class='htbl'><thead><tr><th>Vong</th><th>Tram</th><th>Len</th><th>Xuong</th><th>Doanh Thu</th></tr></thead>"
            + "<tbody id='hbody'><tr><td colspan='5' style='color:var(--dim);text-align:center'>Dang tai...</td></tr></tbody></table></div>"
            + "</div>"
            + "<script>"
            + "const ST=[{id:'server1-ngan',name:'Ngan',nid:'n1',cx:130,cy:40},{id:'server2-nhi',name:'Nhi',nid:'n2',cx:218,cy:93},{id:'server3-my',name:'My',nid:'n3',cx:187,cy:197},{id:'server4-suong',name:'Suong',nid:'n4',cx:73,cy:197},{id:'server5-hang',name:'Hang',nid:'n5',cx:42,cy:93}];"
            + "function clk(){const n=new Date();document.getElementById('clk').textContent=[n.getHours(),n.getMinutes(),n.getSeconds()].map(x=>x.toString().padStart(2,'0')).join(':');}"
            + "setInterval(clk,1000);clk();"
            + "function fmt(n){return Number(n||0).toLocaleString('vi-VN');}"
            + "function lcls(l){if(l.includes('\\u2705')||l.includes('hoan thanh')||l.includes('thanh cong'))return 'ok';if(l.includes('\\u274c')||l.includes('LOI')||l.includes('CANH BAO'))return 'er';if(l.includes('\\ud83d\\udce4')||l.includes('Token')||l.includes('TRAM'))return 'in';if(l.includes('\\u26a0')||l.includes('INACTIVE'))return 'wn';return 'dm';}"
            + "async function refresh(){try{"
            + "const d=await fetch('/api/status').then(r=>r.json());"
            + "const ss=d.serverStatus||{};const tk=d.currentToken||{};const run=d.isRunning;"
            + "const dot=document.getElementById('mdot');const txt=document.getElementById('mtxt');"
            + "dot.className='dot'+(run?'':' off');"
            + "txt.textContent=run?'He thong dang hoat dong':'He thong da dung';"
            + "txt.style.color=run?'#00ff88':'#ff3d5a';"
            + "document.getElementById('s-rounds').textContent=fmt(tk.totalRounds);"
            + "document.getElementById('s-pass').textContent=fmt(tk.currentPassengers);"
            + "document.getElementById('s-rev').textContent=fmt(tk.totalRevenue)+'d';"
            + "const ac=Object.values(ss).filter(Boolean).length+1;"
            + "document.getElementById('s-act').textContent=ac+'/5 tram';"
            + "const tp=document.getElementById('tpath');"
            + "tp.innerHTML=ST.map((s,i)=>{const isAct=s.id==='server1-ngan'||(tk.activeServers||[]).includes(s.id);const isCur=s.id===tk.lastStation;const cls=isCur?'cur':(isAct?'':'dead');return '<span class=\"tst '+cls+'\">'+s.name+'</span>'+(i<ST.length-1?'<span class=\"sep\">\\u2192</span>':'<span class=\"sep\">\\u21a9</span>');}).join('');"
            + "ST.forEach(s=>{const g=document.getElementById(s.nid);if(!g)return;const c=g.querySelector('circle');const ok=s.id==='server1-ngan'||ss[s.id]===true;c.setAttribute('stroke',ok?'#00ff88':'#ff3d5a');c.setAttribute('stroke-width',ok?'2':'1.5');g.style.opacity=ok?'1':'0.35';});"
            + "const cur=ST.find(s=>s.id===tk.lastStation)||ST[0];const td=document.getElementById('tdot');td.setAttribute('cx',cur.cx);td.setAttribute('cy',cur.cy);"
            + "const sl=document.getElementById('srvlist');sl.innerHTML=ST.map((s,i)=>{const ok=s.id==='server1-ngan'||ss[s.id]===true;return '<div class=\"srv-row '+(ok?'ok':'err')+'\"><span class=\"srv-name\">'+s.name+' (server-'+(i+1)+')</span><span class=\"badges\">'+(s.id==='server1-ngan'?'<span class=\"badge st\">Starter</span>':'')+'<span class=\"badge '+(ok?'ok':'err')+'\">'+(ok?'ACTIVE':'LOI')+'</span><span class=\"badge db\">DB rieng</span></span></div>';}).join('');"
            + "const logs=d.systemLogs||[];document.getElementById('logbox').innerHTML=logs.slice(-60).reverse().map(l=>'<div class=\"ll '+lcls(l)+'\">'+l+'</div>').join('');"
            + "const hist=d.recentDBLogs||[];document.getElementById('hbody').innerHTML=hist.length===0?'<tr><td colspan=\"5\" style=\"color:var(--dim);text-align:center\">Chua co du lieu</td></tr>':hist.map(h=>'<tr><td class=\"c\">'+h.roundNumber+'</td><td style=\"font-size:.75rem\">'+(h.stationName||'--')+'</td><td class=\"g\">+'+h.passengersBoarded+'</td><td class=\"r\">-'+h.passengersAlighted+'</td><td class=\"y\">'+fmt(h.revenue)+'d</td></tr>').join('');"
            + "}catch(e){document.getElementById('mtxt').textContent='Mat ket noi...';}"
            + "}refresh();setInterval(refresh,2000);"
            + "<\/script></body></html>";
    }
}
