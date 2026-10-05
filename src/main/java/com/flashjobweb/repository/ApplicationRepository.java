package com.flashjobweb.repository;

import com.flashjobweb.entity.ApplicationEntity;
import com.flashjobweb.util.ApplicationStatus;
import com.flashjobweb.util.ApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<ApplicationEntity, UUID> {
    @Query("SELECT COUNT(a) FROM ApplicationEntity a JOIN a.job j " +
            "WHERE a.worker.userId = :workerId " +
            "AND a.status = :activeStatuses " +
            "AND j.startTime < :newEndTime " +
            "AND j.endTime > :newStartTime")
    Long existsOverlappingJob(
            @Param("workerId") UUID workerId,
            @Param("newStartTime") LocalDateTime newStartTime,
            @Param("newEndTime") LocalDateTime newEndTime,
            @Param("activeStatuses") ApplicationStatus activeStatuses
    );
    @Query("SELECT a FROM ApplicationEntity a " +
            "JOIN a.job j " +
            "LEFT JOIN j.category c " +
            "LEFT JOIN j.employer e " +
            "WHERE a.worker.userId = :workerId " +
            "AND (:status IS NULL OR a.status = :status) " +
            "AND (:type IS NULL OR a.type = :type) " +
            "AND (:keyword IS NULL OR :keyword = '' OR " +
            "     LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "     (c IS NOT NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) OR " +
            "     (e IS NOT NULL AND LOWER(e.employerName) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "ORDER BY j.startTime DESC")
    List<ApplicationEntity> findApplicationsForWorker(
            @Param("workerId") UUID workerId,
            @Param("status") ApplicationStatus status,
            @Param("type") ApplicationType type,
            @Param("keyword") String keyword
    );

    default List<ApplicationEntity> findApplicationsForWorker(UUID workerId, ApplicationStatus status, ApplicationType type) {
        return findApplicationsForWorker(workerId, status, type, null);
    }

    @Query("SELECT a FROM ApplicationEntity a WHERE a.job.id = :jobId AND a.type = 'APPLICATION' ORDER BY a.createdAt DESC")
    List<ApplicationEntity> findApplicationsByJobId(@Param("jobId") UUID jobId);
    boolean existsByworker_userIdAndJob_id(UUID workerId, UUID jobId);

    @Query("SELECT a FROM ApplicationEntity a WHERE a.job.id = :jobId AND a.worker.userId = :workerId")
    Optional<ApplicationEntity> findByJobIdAndWorkerId(@Param("jobId") UUID jobId, @Param("workerId") UUID workerId);
    Long countByJob_IdAndStatus(UUID jobId, ApplicationStatus status);
    List<ApplicationEntity> findByJob_IdAndStatus(UUID jobId, ApplicationStatus status);
    @Query("SELECT a.worker.userId FROM ApplicationEntity a WHERE a.job.id = :jobId AND a.status = :status")
    List<ApplicationEntity> findWorkerByJobIdAndStatus(@Param("jobId") UUID jobId, @Param("status") String status);

    @Query("SELECT a FROM ApplicationEntity a WHERE a.worker.userId = :userId OR a.job.employer.userId = :userId")
    List<ApplicationEntity> findAllByWorkerOrEmployer(@Param("userId") UUID userId);
}
