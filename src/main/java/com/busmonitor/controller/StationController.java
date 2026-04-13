package com.busmonitor.controller;

import com.busmonitor.model.BusToken;
import com.busmonitor.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@CrossOrigin(origins = "*")
public class StationController {

    @Autowired
    private NodeService node;

    // ========== API ==========

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("status",   "UP");
        r.put("server",   node.getMyId());
        r.put("isLeader", node.isLeader());
        r.put("leader",   node.getLeaderId());
        r.put("epoch",    node.getCurrentEpoch());
        r.put("time",     new Date().toString());
        return ResponseEntity.ok(r);
    }

    @PostMapping("/api/start")
    public ResponseEntity<String> start() {
        return ResponseEntity.ok(node.startSystem());
    }

    @PostMapping("/api/stop")
    public ResponseEntity<String> stop() {
        return ResponseEntity.ok(node.stopSystem());
    }

    @PostMapping("/api/receive-token")
    public ResponseEntity<String> receiveToken(@RequestBody BusToken t) {
        new Thread(() -> node.receiveToken(t)).start();
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/api/receive-token-back")
    public ResponseEntity<String> receiveTokenBack(@RequestBody BusToken t) {
        new Thread(() -> node.receiveTokenBack(t)).start();
        return ResponseEntity.ok("OK");
    }

    // === DONG BO DU LIEU ===

    @PostMapping("/api/sync-round")
    public ResponseEntity<String> syncRound(@RequestBody List<Map<String, Object>> entries) {
        new Thread(() -> node.receiveSyncData(entries)).start();
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/api/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("myId",        node.getMyId());
        d.put("isLeader",    node.isLeader());
        d.put("leaderId",    node.getLeaderId());
        d.put("isRunning",   node.isRunning());
        d.put("inTransit",   node.isInTransit());
        d.put("epoch",       node.getCurrentEpoch());
        d.put("token",       node.getToken());
        d.put("serverStatus",node.getServerStatus());
        d.put("dbLogs",      node.getDbLogs());
        d.put("logs",        node.getLogs());
        return ResponseEntity.ok(d);
    }

    // ========== DASHBOARD ==========

    @GetMapping("/")
    public String dashboard() {
        String myId = node.getMyId();
        return html(myId);
    }

    private String html(String myId) {
        return "<!DOCTYPE html><html lang='vi'><head><meta charset='UTF-8'>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1.0'>"
            + "<title>Bus Monitor - " + myId + "</title>"
            + "<link href='https://fonts.googleapis.com/css2?family=Share+Tech+Mono&family=Exo+2:wght@400;600;700&display=swap' rel='stylesheet'>"
            + "<style>"
            + ":root{--bg:#080c14;--bg2:#0d1520;--bg3:#111d2e;--bd:#1a3050;--cy:#00d4ff;--gn:#00ff88;--rd:#ff3d5a;--yw:#ffd24d;--pu:#a259ff;--tx:#c8dff0;--dm:#4a6880;}"
            + "*{box-sizing:border-box;margin:0;padding:0;}"
            + "body{background:var(--bg);color:var(--tx);font-family:'Exo 2',sans-serif;min-height:100vh;}"
            + ".hdr{background:var(--bg2);border-bottom:1px solid var(--bd);padding:12px 24px;display:flex;align-items:center;justify-content:space-between;flex-wrap:wrap;gap:8px;}"
            + ".htl{font-size:.95rem;font-weight:700;color:var(--cy);letter-spacing:2px;text-transform:uppercase;}"
            + ".hsb{font-size:.7rem;color:var(--dm);margin-top:2px;font-family:'Share Tech Mono',monospace;}"
            + ".hrg{display:flex;align-items:center;gap:8px;flex-wrap:wrap;}"
            + ".pill{font-size:.7rem;padding:3px 10px;border-radius:20px;font-family:'Share Tech Mono',monospace;font-weight:600;}"
            + ".me{background:#00d4ff18;border:1px solid #00d4ff40;color:var(--cy);}"
            + ".ld{background:#ffd24d18;border:1px solid #ffd24d40;color:var(--yw);}"
            + ".ep{background:#a259ff18;border:1px solid #a259ff40;color:var(--pu);}"
            + ".run{background:#00ff8818;border:1px solid #00ff8840;color:var(--gn);}"
            + ".stp{background:#ff3d5a18;border:1px solid #ff3d5a40;color:var(--rd);}"
            + "@keyframes bk{0%,100%{opacity:1}50%{opacity:.3}}"
            + ".dot{width:8px;height:8px;border-radius:50%;display:inline-block;margin-right:4px;}"
            + ".don{background:var(--gn);box-shadow:0 0 6px var(--gn);animation:bk 1.4s infinite;}"
            + ".dof{background:var(--rd);box-shadow:0 0 6px var(--rd);}"
            + ".main{padding:12px 16px;display:grid;grid-template-columns:1fr 1fr 1fr;gap:11px;}"
            + ".card{background:var(--bg2);border:1px solid var(--bd);border-radius:10px;padding:13px;}"
            + ".ct{font-size:.62rem;font-weight:600;letter-spacing:2px;text-transform:uppercase;color:var(--dm);margin-bottom:11px;display:flex;align-items:center;gap:5px;}"
            + ".ct::before{content:'';display:inline-block;width:3px;height:10px;background:var(--cy);border-radius:2px;}"
            + ".sg{display:grid;grid-template-columns:1fr 1fr;gap:7px;}"
            + ".sb{background:var(--bg3);border:1px solid var(--bd);border-radius:7px;padding:9px 11px;}"
            + ".sl{font-size:.62rem;color:var(--dm);margin-bottom:3px;font-family:'Share Tech Mono',monospace;}"
            + ".sv{font-size:1.4rem;font-weight:700;font-family:'Share Tech Mono',monospace;line-height:1.1;}"
            + ".c{color:var(--cy);} .g{color:var(--gn);} .y{color:var(--yw);} .p{color:var(--pu);} .r{color:var(--rd);}"
            + ".rw{display:flex;justify-content:center;padding:4px 0;}"
            + ".sl2{display:flex;flex-direction:column;gap:6px;}"
            + ".sr{display:flex;align-items:center;justify-content:space-between;background:var(--bg3);border:1px solid var(--bd);border-radius:7px;padding:8px 11px;transition:border-color .3s;}"
            + ".sok{border-color:#00ff8830;} .ser{border-color:#ff3d5a30;} .sme{border-color:#00d4ff40;} .sld{border-color:#ffd24d40;}"
            + ".sn{font-size:.8rem;font-weight:600;}"
            + ".bgs{display:flex;gap:4px;align-items:center;}"
            + ".bg{font-size:.58rem;font-weight:700;letter-spacing:1px;padding:2px 6px;border-radius:3px;text-transform:uppercase;}"
            + ".bok{background:#00ff8815;color:var(--gn);border:1px solid #00ff8835;}"
            + ".ber{background:#ff3d5a15;color:var(--rd);border:1px solid #ff3d5a35;}"
            + ".bme{background:#00d4ff15;color:var(--cy);border:1px solid #00d4ff35;}"
            + ".bld{background:#ffd24d15;color:var(--yw);border:1px solid #ffd24d35;}"
            + ".bdb{background:#a259ff15;color:var(--pu);border:1px solid #a259ff35;}"
            + ".lb{background:var(--bg);border:1px solid var(--bd);border-radius:6px;padding:8px 10px;height:190px;overflow-y:auto;font-family:'Share Tech Mono',monospace;font-size:.68rem;line-height:1.75;}"
            + ".lb::-webkit-scrollbar{width:3px;} .lb::-webkit-scrollbar-thumb{background:var(--bd);border-radius:2px;}"
            + ".ll{padding:1px 0;border-bottom:1px solid #ffffff06;}"
            + ".lok{color:var(--gn);} .ler{color:var(--rd);} .lin{color:var(--cy);} .lwn{color:var(--yw);} .ldm{color:var(--dm);} .lsn{color:var(--pu);}"
            + ".s2{grid-column:1/3;}"
            + ".tb{width:100%;border-collapse:collapse;font-size:.76rem;}"
            + ".tb th{text-align:left;padding:6px 9px;font-size:.6rem;letter-spacing:1px;text-transform:uppercase;color:var(--dm);border-bottom:1px solid var(--bd);font-weight:600;}"
            + ".tb td{padding:6px 9px;border-bottom:1px solid #ffffff05;font-family:'Share Tech Mono',monospace;}"
            + ".tb tr:hover td{background:var(--bg3);}"
            + ".tp{display:flex;align-items:center;gap:4px;flex-wrap:wrap;margin-top:9px;}"
            + ".ts{font-size:.66rem;padding:3px 8px;border-radius:20px;border:1px solid var(--bd);color:var(--dm);font-family:'Share Tech Mono',monospace;}"
            + ".tcur{background:#00d4ff18;border-color:var(--cy);color:var(--cy);}"
            + ".tme{border-color:#ffd24d50;color:var(--yw);background:#ffd24d10;}"
            + ".tdead{opacity:.3;text-decoration:line-through;}"
            + ".sp{color:var(--bd);font-size:.72rem;}"
            + "@media(max-width:800px){.main{grid-template-columns:1fr 1fr;} .s2{grid-column:1/-1;}}"
            + "</style></head><body>"
            // HEADER
            + "<div class='hdr'>"
            + "<div><div class='htl'>He Thong Giam Sat Vong Tron Ao</div>"
            + "<div class='hsb'>He Phan Tan - Dien Toan Dam May - <span id='clk'>--:--:--</span></div></div>"
            + "<div class='hrg'>"
            + "<span class='pill me'>TOI: " + myId.toUpperCase() + "</span>"
            + "<span class='pill ld' id='lpill'>LEADER: ...</span>"
            + "<span class='pill ep' id='epill'>EPOCH: 0</span>"
            + "<span class='pill stp' id='rpill'><span class='dot dof' id='dot'></span><span id='rtxt'>Dang ket noi</span></span>"
            + "</div></div>"
            // MAIN GRID
            + "<div class='main'>"
            // Card 1: Thong ke
            + "<div class='card'><div class='ct'>Thong Ke He Thong</div><div class='sg'>"
            + "<div class='sb'><div class='sl'>So Vong</div><div class='sv c' id='rnd'>--</div></div>"
            + "<div class='sb'><div class='sl'>Hanh Khach</div><div class='sv g' id='pax'>--</div></div>"
            + "<div class='sb'><div class='sl'>Doanh Thu</div><div class='sv y' id='rev'>--</div></div>"
            + "<div class='sb'><div class='sl'>Server Active</div><div class='sv p' id='act'>--</div></div>"
            + "</div>"
            + "<div style='margin-top:10px'><div class='sl' style='margin-bottom:5px'>Vi Tri Token</div><div class='tp' id='tp'>--</div></div></div>"
            // Card 2: Vong tron
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
            + "<g id='tdot' transform='translate(130,40)'>"
            + "<rect x='-12' y='-8' width='24' height='16' rx='3' fill='#ffd24d' stroke='#e6b800' stroke-width='0.5'>"
            + "<animate attributeName='opacity' values='1;0.7;1' dur='1.2s' repeatCount='indefinite'/></rect>"
            + "<rect x='-9' y='-6' width='5' height='5' rx='1' fill='#080c14' opacity='0.85'/>"
            + "<rect x='-2' y='-6' width='5' height='5' rx='1' fill='#080c14' opacity='0.85'/>"
            + "<rect x='5' y='-6' width='4' height='5' rx='1' fill='#e6b800'/>"
            + "<circle cx='-6' cy='9' r='2.5' fill='#333' stroke='#666' stroke-width='0.5'/>"
            + "<circle cx='6' cy='9' r='2.5' fill='#333' stroke='#666' stroke-width='0.5'/>"
            + "<circle cx='10' cy='-3' r='1.2' fill='#fff'>"
            + "<animate attributeName='opacity' values='1;0.2;1' dur='0.6s' repeatCount='indefinite'/></circle>"
            + "</g>"
            + "<text id='lring' x='130' y='130' text-anchor='middle' font-size='8' fill='#ffd24d88' font-family='Share Tech Mono'>leader:...</text>"
            + "<text id='ering' x='130' y='142' text-anchor='middle' font-size='7' fill='#a259ff88' font-family='Share Tech Mono'>epoch:0</text>"
            + "</svg></div></div>"
            // Card 3: Server status
            + "<div class='card'><div class='ct'>Trang Thai Server</div><div class='sl2' id='slist'>loading...</div></div>"
            // Card 4: Logs
            + "<div class='card s2'><div class='ct'>Nhat Ky Su Kien</div><div class='lb' id='logb'>Dang ket noi...</div></div>"
            // Card 5: DB history (dong bo)
            + "<div class='card'><div class='ct'>Lich Su DB (Dong Bo)</div>"
            + "<table class='tb'><thead><tr><th>Vong</th><th>Tram</th><th>Len</th><th>Xuong</th><th>Thu</th></tr></thead>"
            + "<tbody id='hbd'><tr><td colspan='5' style='color:var(--dm);text-align:center'>Dang tai...</td></tr></tbody></table></div>"
            + "</div>"
            // SCRIPT
            + "<script>"
            + "const MY='" + myId + "';"
            + "const ST=[{id:'server1-ngan',name:'Ngan',nid:'n1',cx:130,cy:40},{id:'server2-nhi',name:'Nhi',nid:'n2',cx:218,cy:93},{id:'server3-my',name:'My',nid:'n3',cx:187,cy:197},{id:'server4-suong',name:'Suong',nid:'n4',cx:73,cy:197},{id:'server5-hang',name:'Hang',nid:'n5',cx:42,cy:93}];"
            + "function clk(){const n=new Date();document.getElementById('clk').textContent=[n.getHours(),n.getMinutes(),n.getSeconds()].map(x=>x.toString().padStart(2,'0')).join(':');} setInterval(clk,1000);clk();"
            + "function fmt(n){return Number(n||0).toLocaleString('vi-VN');}"
            + "function lc(l){if(l.includes('hoan thanh')||l.includes('hoi phuc')||l.includes('Khoi dong'))return 'lok';if(l.includes('mat ket noi')||l.includes('that bai')||l.includes('khong the'))return 'ler';if(l.includes('Gui token')||l.includes('Nhan token')||l.includes('Tram'))return 'lin';if(l.includes('LEADER')||l.includes('leader')||l.includes('bau'))return 'lwn';if(l.includes('Dong bo')||l.includes('dong bo')||l.includes('sync'))return 'lsn';return 'ldm';}"
            + "async function rf(){try{"
            + "const d=await fetch('/api/status').then(r=>r.json());"
            + "const ss=d.serverStatus||{};const tk=d.token||{};const run=d.isRunning;const ldr=d.leaderId||'?';const ep=d.epoch||0;"
            // Header
            + "document.getElementById('lpill').textContent='LEADER: '+ldr.toUpperCase();"
            + "document.getElementById('epill').textContent='EPOCH: '+ep;"
            + "const rp=document.getElementById('rpill'),dot=document.getElementById('dot'),rtxt=document.getElementById('rtxt');"
            + "dot.className='dot '+(run?'don':'dof'); rtxt.textContent=run?'Dang chay':'Da dung'; rp.className='pill '+(run?'run':'stp');"
            // Stats
            + "document.getElementById('rnd').textContent=fmt(tk.totalRounds);"
            + "document.getElementById('pax').textContent=fmt(tk.currentPassengers);"
            + "document.getElementById('rev').textContent=fmt(tk.totalRevenue)+'d';"
            + "const ac=Object.values(ss).filter(Boolean).length; document.getElementById('act').textContent=ac+'/5';"
            // Token path
            + "document.getElementById('tp').innerHTML=ST.map((s,i)=>{const ok=ss[s.id]===true;const cur=s.id===tk.lastStation;const me=s.id===MY;const cls=cur?'ts tcur':(me?'ts tme':(ok?'ts':'ts tdead'));return '<span class=\"'+cls+'\">'+s.name+(me?' *':'')+(cur?' <':'')+' </span>'+(i<4?'<span class=\"sp\">-></span>':'<span class=\"sp\"><-</span>');}).join('');"
            // Ring
            + "ST.forEach(s=>{const g=document.getElementById(s.nid);if(!g)return;const c=g.querySelector('circle');const ok=ss[s.id]===true;const me=s.id===MY;const isLd=s.id===ldr;c.setAttribute('stroke',me?'#ffd24d':(isLd?'#ffd24d':(ok?'#00ff88':'#ff3d5a')));c.setAttribute('stroke-width',(me||isLd)?'3':'1.5');g.style.opacity=ok||me?'1':'0.3';});"
            + "const cur=ST.find(s=>s.id===tk.lastStation)||ST[0];document.getElementById('tdot').setAttribute('transform','translate('+cur.cx+','+cur.cy+')');"
            + "document.getElementById('lring').textContent='leader: '+ldr;"
            + "document.getElementById('ering').textContent='epoch: '+ep;"
            // Server list
            + "document.getElementById('slist').innerHTML=ST.map((s,i)=>{const ok=ss[s.id]===true;const me=s.id===MY;const isLd=s.id===ldr;const cls='sr '+(me?'sme':(isLd?'sld':(ok?'sok':'ser')));return '<div class=\"'+cls+'\"><span class=\"sn\">'+s.name+(me?' (toi)':'')+' </span><span class=\"bgs\">'+(isLd?'<span class=\"bg bld\">LEADER</span>':'')+(me?'<span class=\"bg bme\">TOI</span>':'')+' <span class=\"bg '+(ok||me?'bok':'ber')+'\">'+(ok||me?'ACTIVE':'LOI')+' </span><span class=\"bg bdb\">DB</span><span class=\"bg bdb\">SYNC</span></span></div>';}).join('');"
            // Logs
            + "const logs=d.logs||[]; document.getElementById('logb').innerHTML=logs.slice(-80).reverse().map(l=>'<div class=\"ll '+lc(l)+'\">'+l+'</div>').join('');"
            // DB History
            + "const h=d.dbLogs||[]; document.getElementById('hbd').innerHTML=h.length?h.map(x=>'<tr><td class=\"c\">'+x.roundNumber+'</td><td style=\"font-size:.72rem\">'+(x.stationName||'')+'</td><td class=\"g\">+'+x.passengersBoarded+'</td><td class=\"r\">-'+x.passengersAlighted+'</td><td class=\"y\">'+fmt(x.revenue)+'</td></tr>').join(''):'<tr><td colspan=\"5\" style=\"color:var(--dm);text-align:center\">Chua co du lieu</td></tr>';"
            + "}catch(e){document.getElementById('rtxt').textContent='Mat ket noi';document.getElementById('dot').className='dot dof';}}"
            + "rf();setInterval(rf,2000);"
            + "</script></body></html>";
    }
}
