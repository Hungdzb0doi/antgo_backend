package com.flashjobweb.repository;

import com.flashjobweb.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {
    List<ReviewEntity> findByRevieweeId(UUID revieweeId);

    @Query("SELECT r FROM ReviewEntity r " +
           "LEFT JOIN FETCH r.reviewer " +
           "LEFT JOIN FETCH r.reviewee " +
           "LEFT JOIN FETCH r.application a " +
           "LEFT JOIN FETCH a.job j " +
           "LEFT JOIN FETCH j.employer " +
           "LEFT JOIN FETCH a.worker " +
           "WHERE r.reviewee.id = :revieweeId " +
           "ORDER BY r.createdAt DESC")
    List<ReviewEntity> findByRevieweeIdWithDetails(@Param("revieweeId") UUID revieweeId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.reviewee.id = :userId")
    Double findAverageRatingByRevieweeId(@Param("userId") UUID userId);

    Optional<ReviewEntity> findByApplicationIdAndReviewerId(UUID applicationId, UUID reviewerId);

    boolean existsByApplicationIdAndReviewerId(UUID applicationId, UUID reviewerId);

    long countByRevieweeId(UUID revieweeId);

    long countByRevieweeIdAndRating(UUID revieweeId, Integer rating);

    @Query("SELECT DISTINCT r.reviewee.id FROM ReviewEntity r WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<UUID> findDistinctRevieweeIdsByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    long countByRevieweeIdAndCreatedAtBetween(UUID revieweeId, LocalDateTime startDate, LocalDateTime endDate);

    long countByRevieweeIdAndRatingAndCreatedAtBetween(UUID revieweeId, Integer rating, LocalDateTime startDate, LocalDateTime endDate);
}
