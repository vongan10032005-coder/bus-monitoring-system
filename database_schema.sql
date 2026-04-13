-- ============================================================
-- FILE SQL CHO HE THONG GIAM SAT VONG TRON AO
-- He thong xe buyt/tau dien lien tuyen
-- Moi server (1-5) dung 1 database rieng biet
-- Chay file nay tren TUNG database: busdb1 -> busdb5
-- ============================================================

-- Xoa bang cu neu co (de chay lai tu dau)
DROP TABLE IF EXISTS round_logs;

-- Tao bang chinh luu lich su hanh khach theo vong
-- Du lieu duoc DONG BO tu tat ca cac tram qua co che broadcast
CREATE TABLE round_logs (
    id          BIGSERIAL PRIMARY KEY,
    round_number        INTEGER         NOT NULL,
    passengers_boarded  INTEGER         NOT NULL DEFAULT 0,
    passengers_alighted INTEGER         NOT NULL DEFAULT 0,
    revenue             DECIMAL(15,2)   NOT NULL DEFAULT 0,
    station_name        VARCHAR(100)    NOT NULL,
    timestamp           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Rang buoc duy nhat: moi tram chi co 1 ban ghi cho moi vong
    -- Giup tranh trung lap khi dong bo du lieu giua cac server
    CONSTRAINT uq_round_station UNIQUE (round_number, station_name)
);

-- Index tang toc truy van
CREATE INDEX idx_round_number ON round_logs(round_number);
CREATE INDEX idx_timestamp    ON round_logs(timestamp DESC);
CREATE INDEX idx_station_name ON round_logs(station_name);

-- View thong ke tong hop theo vong
CREATE OR REPLACE VIEW v_round_summary AS
SELECT
    round_number,
    station_name,
    SUM(passengers_boarded)  AS total_boarded,
    SUM(passengers_alighted) AS total_alighted,
    SUM(revenue)             AS total_revenue,
    COUNT(*)                 AS trips,
    MIN(timestamp)           AS started_at,
    MAX(timestamp)           AS finished_at
FROM round_logs
GROUP BY round_number, station_name
ORDER BY round_number DESC, station_name;

-- View doanh thu tung tram
CREATE OR REPLACE VIEW v_station_revenue AS
SELECT
    station_name,
    COUNT(DISTINCT round_number)    AS total_rounds,
    SUM(passengers_boarded)         AS total_boarded,
    SUM(passengers_alighted)        AS total_alighted,
    SUM(revenue)                    AS total_revenue,
    AVG(revenue)                    AS avg_revenue_per_trip
FROM round_logs
GROUP BY station_name
ORDER BY total_revenue DESC;

-- Du lieu khoi tao (tram 1 - Ngan la khoi diem)
INSERT INTO round_logs (round_number, passengers_boarded, passengers_alighted, revenue, station_name, timestamp)
VALUES (0, 0, 0, 0.00, 'SYSTEM_INIT', CURRENT_TIMESTAMP);
