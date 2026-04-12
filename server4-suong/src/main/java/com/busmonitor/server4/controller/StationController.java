package com.busmonitor.server4.controller;

import com.busmonitor.server4.model.BusToken;
import com.busmonitor.server4.service.TokenService;
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
        res.put("server", tokenService.getMyId());
        res.put("isLeader", tokenService.isLeader());
        res.put("currentLeader", tokenService.getCurrentLeaderId());
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

    @PostMapping("/api/receive-token")
    public ResponseEntity<String> receiveToken(@RequestBody BusToken token) {
        new Thread(() -> tokenService.receiveToken(token)).start();
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/api/receive-token-back")
    public ResponseEntity<String> receiveTokenBack(@RequestBody BusToken token) {
        new Thread(() -> tokenService.receiveTokenBack(token)).start();
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("myId", tokenService.getMyId());
        d.put("isLeader", tokenService.isLeader());
        d.put("currentLeader", tokenService.getCurrentLeaderId());
        d.put("isRunning", tokenService.isRunning());
        d.put("tokenInTransit", tokenService.isTokenInTransit());
        d.put("currentToken", tokenService.getCurrentToken());
        d.put("serverStatus", tokenService.getServerStatus());
        d.put("recentDBLogs", tokenService.getRecentLogs());
        d.put("systemLogs", tokenService.getLogs());
        return ResponseEntity.ok(d);
    }

    @GetMapping("/")
    public String dashboard() {
        String myId = tokenService.getMyId();
        return getDashboard(myId);
    }

    private String getDashboard(String myId) {
        return "<!DOCTYPE html><html lang='vi'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1.0'>"
            + "<title>Giam Sat - " + myId + "</title>"
            + "<link href='https://fonts.googleapis.com/css2?family=Share+Tech+Mono&family=Exo+2:wght@400;600;700&display=swap' rel='stylesheet'>"
            + "<style>"
            + ":root{--bg:#080c14;--bg2:#0d1520;--bg3:#111d2e;--bd:#1a3050;--cy:#00d4ff;--gn:#00ff88;--rd:#ff3d5a;--yw:#ffd24d;--pu:#a259ff;--tx:#c8dff0;--dm:#4a6880;}"
            + "*{box-sizing:border-box;margin:0;padding:0;}"
            + "body{background:var(--bg);color:var(--tx);font-family:'Exo 2',sans-serif;min-height:100vh;}"
            + ".hdr{background:var(--bg2);border-bottom:1px solid var(--bd);padding:12px 24px;display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:8px;}"
            + ".htl{font-size:.95rem;font-weight:700;color:var(--cy);letter-spacing:2px;text-transform:uppercase;}"
            + ".hsb{font-size:.7rem;color:var(--dm);margin-top:2px;font-family:'Share Tech Mono',monospace;}"
            + ".hrg{display:flex;align-items:center;gap:10px;flex-wrap:wrap;}"
            + ".pill{font-size:.7rem;padding:3px 10px;border-radius:20px;font-family:'Share Tech Mono',monospace;}"
            + ".pill.me{background:#00d4ff18;border:1px solid #00d4ff35;color:var(--cy);}"
            + ".pill.ldr{background:#ffd24d18;border:1px solid #ffd24d35;color:var(--yw);}"
            + ".pill.run{background:#00ff8818;border:1px solid #00ff8835;color:var(--gn);}"
            + ".pill.stp{background:#ff3d5a18;border:1px solid #ff3d5a35;color:var(--rd);}"
            + "@keyframes bk{0%,100%{opacity:1}50%{opacity:.3}}"
            + ".dot{width:8px;height:8px;border-radius:50%;display:inline-block;margin-right:5px;}"
            + ".dot.on{background:var(--gn);box-shadow:0 0 6px var(--gn);animation:bk 1.4s infinite;}"
            + ".dot.off{background:var(--rd);box-shadow:0 0 6px var(--rd);}"
            + ".main{padding:12px 16px;display:grid;grid-template-columns:1fr 1fr 1fr;gap:11px;}"
            + ".card{background:var(--bg2);border:1px solid var(--bd);border-radius:10px;padding:13px;}"
            + ".ct{font-size:.62rem;font-weight:600;letter-spacing:2px;text-transform:uppercase;color:var(--dm);margin-bottom:11px;display:flex;align-items:center;gap:5px;}"
            + ".ct::before{content:'';display:inline-block;width:3px;height:10px;background:var(--cy);border-radius:2px;}"
            + ".sg{display:grid;grid-template-columns:1fr 1fr;gap:7px;}"
            + ".sb{background:var(--bg3);border:1px solid var(--bd);border-radius:7px;padding:9px 11px;}"
            + ".sl{font-size:.62rem;color:var(--dm);margin-bottom:3px;font-family:'Share Tech Mono',monospace;}"
            + ".sv{font-size:1.4rem;font-weight:700;font-family:'Share Tech Mono',monospace;line-height:1.1;}"
            + ".c{color:var(--cy);} .g{color:var(--gn);} .y{color:var(--yw);} .p{color:var(--pu);} .r{color:var(--rd);}"
            + ".rw{display:flex;justify-content:center;padding:5px 0;}"
            + ".sl2{display:flex;flex-direction:column;gap:6px;}"
            + ".sr{display:flex;align-items:center;justify-content:space-between;background:var(--bg3);border:1px solid var(--bd);border-radius:7px;padding:8px 11px;transition:border-color .3s;}"
            + ".sr.ok{border-color:#00ff8830;} .sr.er{border-color:#ff3d5a30;} .sr.me{border-color:#00d4ff40;} .sr.ldr{border-color:#ffd24d40;}"
            + ".sn{font-size:.8rem;font-weight:600;}"
            + ".bgs{display:flex;gap:4px;align-items:center;}"
            + ".bg{font-size:.58rem;font-weight:700;letter-spacing:1px;padding:2px 6px;border-radius:3px;text-transform:uppercase;}"
            + ".bg.ok{background:#00ff8815;color:var(--gn);border:1px solid #00ff8835;}"
            + ".bg.er{background:#ff3d5a15;color:var(--rd);border:1px solid #ff3d5a35;}"
            + ".bg.me{background:#00d4ff15;color:var(--cy);border:1px solid #00d4ff35;}"
            + ".bg.ld{background:#ffd24d15;color:var(--yw);border:1px solid #ffd24d35;}"
            + ".bg.db{background:#a259ff15;color:var(--pu);border:1px solid #a259ff35;}"
            + ".lb{background:var(--bg);border:1px solid var(--bd);border-radius:6px;padding:8px 10px;height:185px;overflow-y:auto;font-family:'Share Tech Mono',monospace;font-size:.68rem;line-height:1.75;}"
            + ".lb::-webkit-scrollbar{width:3px;} .lb::-webkit-scrollbar-thumb{background:var(--bd);border-radius:2px;}"
            + ".ll{padding:1px 0;border-bottom:1px solid #ffffff06;}"
            + ".ll.ok{color:var(--gn);} .ll.er{color:var(--rd);} .ll.in{color:var(--cy);} .ll.wn{color:var(--yw);} .ll.dm{color:var(--dm);}"
            + ".s2{grid-column:1/3;}"
            + ".tb{width:100%;border-collapse:collapse;font-size:.76rem;}"
            + ".tb th{text-align:left;padding:6px 9px;font-size:.6rem;letter-spacing:1px;text-transform:uppercase;color:var(--dm);border-bottom:1px solid var(--bd);font-weight:600;}"
            + ".tb td{padding:6px 9px;border-bottom:1px solid #ffffff05;font-family:'Share Tech Mono',monospace;}"
            + ".tb tr:hover td{background:var(--bg3);}"
            + ".tp{display:flex;align-items:center;gap:4px;flex-wrap:wrap;margin-top:9px;}"
            + ".ts{font-size:.66rem;padding:3px 8px;border-radius:20px;border:1px solid var(--bd);color:var(--dm);font-family:'Share Tech Mono',monospace;}"
            + ".ts.cur{background:#00d4ff18;border-color:var(--cy);color:var(--cy);}"
            + ".ts.me{border-color:#ffd24d50;color:var(--yw);}"
            + ".ts.dead{opacity:.3;text-decoration:line-through;}"
            + ".sp{color:var(--bd);font-size:.72rem;}"
            + "@media(max-width:800px){.main{grid-template-columns:1fr 1fr;} .s2{grid-column:1/-1;}}"
            + "</style></head><body>"
            + "<div class='hdr'>"
            + "<div><div class='htl'>He Thong Giam Sat Vong Tron Ao</div>"
            + "<div class='hsb'>Phan Tan &mdash; Dien Toan Dam May &mdash; <span id='clk'>--:--:--</span></div></div>"
            + "<div class='hrg'>"
            + "<span class='pill me' id='my-pill'>TOI: " + myId.toUpperCase() + "</span>"
            + "<span class='pill ldr' id='ldr-pill'>LEADER: ...</span>"
            + "<span class='pill stp' id='run-pill'><span class='dot off' id='dot'></span><span id='rtxt'>Dang ket noi</span></span>"
            + "</div></div>"
            + "<div class='main'>"
            // Stats card
            + "<div class='card'><div class='ct'>Thong Ke</div><div class='sg'>"
            + "<div class='sb'><div class='sl'>So Vong</div><div class='sv c' id='rnd'>--</div></div>"
            + "<div class='sb'><div class='sl'>Hanh Khach</div><div class='sv g' id='pax'>--</div></div>"
            + "<div class='sb'><div class='sl'>Doanh Thu</div><div class='sv y' id='rev'>--</div></div>"
            + "<div class='sb'><div class='sl'>Tram Active</div><div class='sv p' id='act'>--</div></div>"
            + "</div>"
            + "<div style='margin-top:10px'><div class='sl' style='margin-bottom:5px'>Vi Tri Token</div>"
            + "<div class='tp' id='tpath'>--</div></div></div>"
            // Ring card
            + "<div class='card'><div class='ct'>Vong Tron Ao</div><div class='rw'>"
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
            + "</circle>"
            + "<text id='ldr-ring' x='130' y='140' text-anchor='middle' font-size='9' fill='#ffd24d' font-family='Share Tech Mono'>leader:...</text>"
            + "</svg></div></div>"
            // Server status card
            + "<div class='card'><div class='ct'>Trang Thai Server</div>"
            + "<div class='sl2' id='slist'>loading...</div></div>"
            // Logs card
            + "<div class='card s2'><div class='ct'>Nhat Ky Su Kien</div>"
            + "<div class='lb' id='logb'>Dang ket noi...</div></div>"
            // History card
            + "<div class='card'><div class='ct'>Lich Su DB</div>"
            + "<table class='tb'><thead><tr><th>Vong</th><th>Tram</th><th>Len</th><th>Xuong</th><th>Thu</th></tr></thead>"
            + "<tbody id='hbd'><tr><td colspan='5' style='color:var(--dm);text-align:center'>Dang tai...</td></tr></tbody></table></div>"
            + "</div>"
            + "<script>"
            + "const MY='" + myId + "';"
            + "const ST=[{id:'server1-ngan',name:'Ngan',nid:'n1',cx:130,cy:40},{id:'server2-nhi',name:'Nhi',nid:'n2',cx:218,cy:93},{id:'server3-my',name:'My',nid:'n3',cx:187,cy:197},{id:'server4-suong',name:'Suong',nid:'n4',cx:73,cy:197},{id:'server5-hang',name:'Hang',nid:'n5',cx:42,cy:93}];"
            + "function clk(){const n=new Date();document.getElementById('clk').textContent=[n.getHours(),n.getMinutes(),n.getSeconds()].map(x=>x.toString().padStart(2,'0')).join(':');}"
            + "setInterval(clk,1000);clk();"
            + "function fmt(n){return Number(n||0).toLocaleString('vi-VN');}"
            + "function lc(l){if(l.includes('hoan thanh')||l.includes('hoi phuc')||l.includes('thanh cong'))return 'ok';if(l.includes('that bai')||l.includes('mat ket noi')||l.includes('khong the'))return 'er';if(l.includes('Token')||l.includes('Gui')||l.includes('Nhan'))return 'in';if(l.includes('LEADER')||l.includes('leader')||l.includes('bau'))return 'wn';return 'dm';}"
            + "async function rf(){try{"
            + "const d=await fetch('/api/status').then(r=>r.json());"
            + "const ss=d.serverStatus||{};const tk=d.currentToken||{};const run=d.isRunning;const ldr=d.currentLeader||'?';const isLdr=d.isLeader;"
            // Header pills
            + "document.getElementById('ldr-pill').textContent='LEADER: '+ldr.replace('server','s').toUpperCase();"
            + "const rp=document.getElementById('run-pill');"
            + "const dot=document.getElementById('dot');"
            + "const rtxt=document.getElementById('rtxt');"
            + "dot.className='dot '+(run?'on':'off');"
            + "rtxt.textContent=run?'Dang chay':'Da dung';"
            + "rp.className='pill '+(run?'run':'stp');"
            // Stats
            + "document.getElementById('rnd').textContent=fmt(tk.totalRounds);"
            + "document.getElementById('pax').textContent=fmt(tk.currentPassengers);"
            + "document.getElementById('rev').textContent=fmt(tk.totalRevenue)+'d';"
            + "const ac=Object.values(ss).filter(Boolean).length;"
            + "document.getElementById('act').textContent=ac+'/5';"
            // Token path
            + "document.getElementById('tpath').innerHTML=ST.map((s,i)=>{const ok=ss[s.id]!==false;const cur=s.id===tk.lastStation;const me=s.id===MY;const cls=cur?'cur':(me?'me':(ok?'':'dead'));return '<span class=\"ts '+cls+'\">'+s.name+(me?' *':'')+' '+(cur?'<-':'')+' </span>'+(i<4?'<span class=\"sp\">-&gt;</span>':'<span class=\"sp\">&lt;-</span>');}).join('');"
            // Ring
            + "ST.forEach(s=>{const g=document.getElementById(s.nid);if(!g)return;const c=g.querySelector('circle');const ok=ss[s.id]!==false;const me=s.id===MY;const isLdrNode=s.id===ldr;c.setAttribute('stroke',me?'#ffd24d':(isLdrNode?'#ffd24d':(ok?'#00ff88':'#ff3d5a')));c.setAttribute('stroke-width',me||isLdrNode?'3':'1.5');g.style.opacity=ok?'1':'0.3';});"
            + "const cur=ST.find(s=>s.id===tk.lastStation)||ST[0];document.getElementById('tdot').setAttribute('cx',cur.cx);document.getElementById('tdot').setAttribute('cy',cur.cy);"
            + "document.getElementById('ldr-ring').textContent='leader: '+ldr.replace('server','s');"
            // Server list
            + "document.getElementById('slist').innerHTML=ST.map((s,i)=>{const ok=ss[s.id]!==false;const me=s.id===MY;const isLd=s.id===ldr;return '<div class=\"sr '+(me?'me':(isLd?'ldr':(ok?'ok':'er')))+'\"><span class=\"sn\">'+s.name+(me?' (toi)':'')+'</span><span class=\"bgs\">'+(isLd?'<span class=\"bg ld\">LEADER</span>':'')+(me?'<span class=\"bg me\">TOI</span>':'')+'<span class=\"bg '+(ok?'ok':'er')+'\">'+(ok?'ACTIVE':'LOI')+'</span><span class=\"bg db\">DB</span></span></div>';}).join('');"
            // Logs
            + "const logs=d.systemLogs||[];document.getElementById('logb').innerHTML=logs.slice(-60).reverse().map(l=>'<div class=\"ll '+lc(l)+'\">'+l+'</div>').join('');"
            // History
            + "const h=d.recentDBLogs||[];document.getElementById('hbd').innerHTML=h.length?h.map(x=>'<tr><td class=\"c\">'+x.roundNumber+'</td><td style=\"font-size:.72rem\">'+(x.stationName||'')+'</td><td class=\"g\">+'+x.passengersBoarded+'</td><td class=\"r\">-'+x.passengersAlighted+'</td><td class=\"y\">'+fmt(x.revenue)+'</td></tr>').join(''):'<tr><td colspan=\"5\" style=\"color:var(--dm);text-align:center\">Chua co</td></tr>';"
            + "}catch(e){document.getElementById('rtxt').textContent='Mat ket noi';document.getElementById('dot').className='dot off';}}"
            + "rf();setInterval(rf,2000);"
            + "</script></body></html>";
    }
}
