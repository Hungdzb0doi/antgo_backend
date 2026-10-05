package com.flashjobweb.repository;

import com.flashjobweb.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {
    List<ReportEntity> findByStatus(String status);
    List<ReportEntity> findByReportedId(UUID reportedId);
    boolean existsByApplicationId(UUID applicationId);
    boolean existsByApplicationIdAndReporterId(UUID applicationId, UUID reporterId);
}
