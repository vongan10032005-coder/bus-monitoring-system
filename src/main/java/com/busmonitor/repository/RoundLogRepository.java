package com.busmonitor.repository;

import com.busmonitor.model.RoundLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoundLogRepository extends JpaRepository<RoundLog, Long> {
    List<RoundLog> findTop10ByOrderByTimestampDesc();
    boolean existsByRoundNumberAndStationName(int roundNumber, String stationName);
}
