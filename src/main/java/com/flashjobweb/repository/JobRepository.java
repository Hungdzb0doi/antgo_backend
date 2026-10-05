package com.flashjobweb.repository;

import com.flashjobweb.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    List<JobEntity> findByEmployerUserId(UUID employerId);
    List<JobEntity> findByStatus(String status);
    List<JobEntity> findByemployer_userIdAndStatus(UUID employerId, String status);

    @Query("SELECT j FROM JobEntity j LEFT JOIN j.category c WHERE j.employer.userId = :employerId " +
            "AND (:status IS NULL OR :status = '' OR j.status = :status) " +
            "AND (:keyword IS NULL OR :keyword = '' OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR (c IS NOT NULL AND LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "ORDER BY j.startTime DESC")
    List<JobEntity> searchEmployerJobs(
            @Param("employerId") UUID employerId,
            @Param("status") String status,
            @Param("keyword") String keyword
    );
    @Query("SELECT j FROM JobEntity j WHERE j.status = 'OPEN' AND j.startTime <= :currentTime")
    List<JobEntity> findOverdueOpenJobs(@Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT j FROM JobEntity j WHERE j.status = 'IN_PROGRESS' AND j.endTime <= :currentTime")
    List<JobEntity> findOverdueInProgressJobs(@Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Query("update JobEntity j set j.status='IN_PROGRESS' where j.status='OPEN' and j.startTime <= :currentTime")
    int updateJobStatusToInProgress( LocalDateTime currentTime);
    @Modifying
    @Query("UPDATE JobEntity j SET j.status = 'COMPLETED' WHERE j.status = 'IN_PROGRESS' AND j.endTime <= :currentTime")
    int updateJobsToCompleted(LocalDateTime currentTime);

    @Query(value = "SELECT * FROM jobs j " +
            "WHERE ST_DWithin(j.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusInMeters) " +
            "AND j.status = :status " +
            "ORDER BY ST_Distance(j.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography) ASC",
            nativeQuery = true)
    List<JobEntity> findNearbyJobs(
            @Param("longitude") double longitude,
            @Param("latitude") double latitude,
            @Param("radiusInMeters") double radiusInMeters,
            @Param("status") String status // Lọc theo trạng thái, ví dụ: "OPEN"
    );

}
