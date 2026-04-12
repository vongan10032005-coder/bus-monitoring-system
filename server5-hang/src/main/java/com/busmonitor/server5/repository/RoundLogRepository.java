package com.busmonitor.server5.repository;
import com.busmonitor.server5.model.RoundLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface RoundLogRepository extends JpaRepository<RoundLog, Long> {
    List<RoundLog> findTop10ByOrderByTimestampDesc();
}
