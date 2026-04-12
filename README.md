# 🚌 BUS MONITORING SYSTEM — HUONG DAN DEPLOY

## CAU TRUC DU AN
```
bus-monitoring-system/
├── server1-ngan/    ← Tram 1 - Ngan (Dieu phoi trung tam + Dashboard)
├── server2-nhi/     ← Tram 2 - Nhi
├── server3-my/      ← Tram 3 - My
├── server4-suong/   ← Tram 4 - Suong
├── server5-hang/    ← Tram 5 - Hang (Tram cuoi)
├── database_schema.sql
└── README.md
```

## PHAN CONG
| Nguoi     | Server       | Nhiem vu                              |
|-----------|-------------|---------------------------------------|
| Ngan      | server1-ngan| Dieu phoi, kiem tra suc khoe, dashboard|
| Nhi       | server2-nhi | Nhan token, xu ly hanh khach, chuyen tiep|
| My        | server3-my  | Nhan token, xu ly hanh khach, chuyen tiep|
| Suong     | server4-suong| Nhan token, xu ly hanh khach, chuyen tiep|
| Hang      | server5-hang| Nhan token, xu ly hanh khach, tra ve S1|

## BUOC 1: NGAN TAO GITHUB REPO

1. Vao https://github.com → Dang nhap
2. Click "New" → Dat ten: bus-monitoring-system → Public → Create
3. Mo terminal, vao thu muc du an:
   git init
   git add .
   git commit -m "feat: khoi tao toan bo 5 server"
   git branch -M main
   git remote add origin https://github.com/TEN_NGAN/bus-monitoring-system.git
   git push -u origin main

4. Moi thanh vien: Settings → Collaborators → Add people (nhap email/username)
5. Tung nguoi nhan email moi → Accept invitation

## BUOC 2: TUNG NGUOI TAO DATABASE TREN RENDER

1. Vao https://render.com → Sign up bang GitHub
2. Click "New +" → "PostgreSQL"
3. Dien:
   - Name: busdb1 (Ngan) / busdb2 (Nhi) / busdb3 (My) / busdb4 (Suong) / busdb5 (Hang)
   - Region: Singapore
   - Plan: Free
4. Click "Create Database"
5. Cho 1-2 phut → Copy "External Database URL" → LUU LAI!

## BUOC 3: TUNG NGUOI DEPLOY WEB SERVICE

1. Click "New +" → "Web Service"
2. Chon "Build and deploy from a Git repository"
3. Connect GitHub → Chon repo bus-monitoring-system → Connect
4. Cau hinh:

   Ngan:
   - Name: server1-ngan
   - Root Directory: server1-ngan
   - Runtime: Docker  ← QUAN TRONG
   - Plan: Free

   Nhi:
   - Name: server2-nhi
   - Root Directory: server2-nhi
   - Runtime: Docker
   - Plan: Free

   My:
   - Name: server3-my
   - Root Directory: server3-my
   - Runtime: Docker
   - Plan: Free

   Suong:
   - Name: server4-suong
   - Root Directory: server4-suong
   - Runtime: Docker
   - Plan: Free

   Hang:
   - Name: server5-hang
   - Root Directory: server5-hang
   - Runtime: Docker
   - Plan: Free

5. Environment Variables → Add:
   DATABASE_URL = (paste External Database URL)

6. Click "Create Web Service" → Cho build 5-10 phut

## BUOC 4: SAU KHI DEPLOY XONG - CAP NHAT URL

Moi nguoi copy URL cua minh (vd: https://server2-nhi.onrender.com) gui cho Ngan.

Ngan vao tung service → Environment → Them:

SERVER 1 (Ngan) them:
  SERVER2_URL = https://server2-nhi.onrender.com
  SERVER3_URL = https://server3-my.onrender.com
  SERVER4_URL = https://server4-suong.onrender.com
  SERVER5_URL = https://server5-hang.onrender.com

SERVER 2 (Nhi) them:
  SERVER1_URL = https://server1-ngan.onrender.com
  SERVER3_URL = https://server3-my.onrender.com

SERVER 3 (My) them:
  SERVER1_URL = https://server1-ngan.onrender.com
  SERVER4_URL = https://server4-suong.onrender.com

SERVER 4 (Suong) them:
  SERVER1_URL = https://server1-ngan.onrender.com
  SERVER5_URL = https://server5-hang.onrender.com

SERVER 5 (Hang) them:
  SERVER1_URL = https://server1-ngan.onrender.com

Sau do Render tu dong redeploy.

## BUOC 5: KIEM TRA

Kiem tra suc khoe tung server (dan vao trinh duyet):
  https://server1-ngan.onrender.com/api/health
  https://server2-nhi.onrender.com/api/health
  https://server3-my.onrender.com/api/health
  https://server4-suong.onrender.com/api/health
  https://server5-hang.onrender.com/api/health

Khoi dong he thong (dung Postman hoac curl):
  POST https://server1-ngan.onrender.com/api/start

Xem Dashboard:
  https://server1-ngan.onrender.com/

## LUONG HOAT DONG
Token xuat phat tu Server 1 (Ngan)
  → Server 2 (Nhi) → Server 3 (My) → Server 4 (Suong) → Server 5 (Hang)
  → Quay ve Server 1 = 1 vong hoan thanh

Neu 1 server chet:
  → Server 1 phat hien qua health check (15 giay/lan)
  → Loai server do khoi danh sach active
  → Token di qua cac server con lai
  → Khi server hoi phuc: tu dong them lai vao vong

## NOTE QUAN TRONG - FREE TIER RENDER
- Server free se ngu sau 15 phut khong hoat dong
- Lan dau goi se mat 30-60 giay de "wake up"
- Khi bao cao: nen khoi dong truoc 5 phut
