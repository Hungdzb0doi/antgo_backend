package com.flashjobweb.service.impl;

import com.flashjobweb.dto.request.RequestReviewDTO;
import com.flashjobweb.dto.response.ResponseReviewDTO;
import com.flashjobweb.entity.*;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.*;
import com.flashjobweb.service.NotificationService;
import com.flashjobweb.service.ReviewService;
import com.flashjobweb.util.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final NotificationService notificationService;

    @Override
    public ResponseReviewDTO createReview(RequestReviewDTO requestReviewDTO) {
        if (requestReviewDTO == null || requestReviewDTO.getApplicationId() == null || requestReviewDTO.getRating() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        int rating = requestReviewDTO.getRating();
        if (rating < 1 || rating > 5) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity reviewer = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ApplicationEntity application = applicationRepository.findById(requestReviewDTO.getApplicationId())
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        // Kiểm tra điều kiện hoàn thành công việc
        boolean isAppCompleted = application.getStatus() == ApplicationStatus.COMPLETED;
        boolean isJobCompleted = "COMPLETED".equalsIgnoreCase(application.getJob().getStatus());
        if (!isAppCompleted && !isJobCompleted) {
            throw new AppException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        // Kiểm tra đã đánh giá trước đó chưa
        if (reviewRepository.existsByApplicationIdAndReviewerId(application.getId(), reviewer.getId())) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        boolean isWorker = application.getWorker().getUserId().equals(reviewer.getId());
        boolean isEmployer = application.getJob().getEmployer().getUserId().equals(reviewer.getId());

        if (!isWorker && !isEmployer) {
            throw new AppException(ErrorCode.REVIEW_NOT_PARTICIPANT);
        }

        UserEntity reviewee;
        if (isWorker) {
            reviewee = application.getJob().getEmployer().getUserEntity();
        } else {
            reviewee = application.getWorker().getUserEntity();
        }

        if (reviewer.getId().equals(reviewee.getId())) {
            throw new AppException(ErrorCode.CANNOT_REVIEW_YOURSELF);
        }

        String comment = requestReviewDTO.getComment() != null ? requestReviewDTO.getComment().trim() : null;

        ReviewEntity review = new ReviewEntity();
        review.setApplication(application);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);
        log.info("Người dùng {} đã đánh giá {} sao cho {} trong đơn {}", reviewer.getId(), rating, reviewee.getId(), application.getId());

        // Gửi thông báo đến người được đánh giá
        String roleTitle = isWorker ? "Thợ làm việc" : "Nhà tuyển dụng";
        String commentSummary = (comment != null && !comment.isEmpty()) ? ": \"" + comment + "\"" : "";
        notificationService.sendNotification(
                reviewee,
                "Đánh giá mới từ " + roleTitle,
                reviewer.getFullName() + " đã đánh giá bạn " + rating + " sao" + commentSummary,
                "REVIEW",
                application.getJob().getId()
        );

        return mapToDTO(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseReviewDTO getMyReviewForApplication(UUID applicationId) {
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity reviewer = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return reviewRepository.findByApplicationIdAndReviewerId(applicationId, reviewer.getId())
                .map(this::mapToDTO)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseReviewDTO> getReviewsByReviewee(UUID revieweeId) {
        return reviewRepository.findByRevieweeIdWithDetails(revieweeId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Quét và tổng kết điểm uy tín tự động định kỳ vào 00:00 ngày 1 hàng tháng
     * Điều kiện:
     * - Tối thiểu 10 lượt đánh giá trong tháng
     * - Tỷ lệ 1 sao < 10%: +2 điểm uy tín (tối đa 100)
     * - Tỷ lệ 1 sao >= 20%: -10 điểm uy tín (tối thiểu 0, < 50 khóa nhận việc)
     * - Từ 10% đến < 20%: giữ nguyên
     */
    @Override
    @Scheduled(cron = "0 0 0 1 * ?")
    public void evaluateMonthlyReputationScores() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        LocalDateTime startOfMonth = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = lastMonth.atEndOfMonth().atTime(23, 59, 59, 999999999);

        log.info("Bắt đầu tổng kết điểm uy tín tháng {}/{} (từ {} đến {})",
                lastMonth.getMonthValue(), lastMonth.getYear(), startOfMonth, endOfMonth);

        List<UUID> revieweeIds = reviewRepository.findDistinctRevieweeIdsByCreatedAtBetween(startOfMonth, endOfMonth);
        if (revieweeIds == null || revieweeIds.isEmpty()) {
            log.info("Không có người dùng nào nhận đánh giá trong tháng {}/{}", lastMonth.getMonthValue(), lastMonth.getYear());
            return;
        }

        for (UUID revieweeId : revieweeIds) {
            try {
                processUserMonthlyScore(revieweeId, startOfMonth, endOfMonth, lastMonth);
            } catch (Exception e) {
                log.error("Lỗi khi tổng kết điểm uy tín cho người dùng {}: {}", revieweeId, e.getMessage(), e);
            }
        }

        log.info("Hoàn thành tổng kết điểm uy tín tháng {}/{}", lastMonth.getMonthValue(), lastMonth.getYear());
    }

    private void processUserMonthlyScore(UUID revieweeId, LocalDateTime startOfMonth, LocalDateTime endOfMonth, YearMonth lastMonth) {
        long totalReviews = reviewRepository.countByRevieweeIdAndCreatedAtBetween(revieweeId, startOfMonth, endOfMonth);
        if (totalReviews < 10) {
            log.debug("Người dùng {} chỉ có {} đánh giá trong tháng, không đủ điều kiện tối thiểu 10 lượt.", revieweeId, totalReviews);
            return;
        }

        long oneStarCount = reviewRepository.countByRevieweeIdAndRatingAndCreatedAtBetween(revieweeId, 1, startOfMonth, endOfMonth);
        double oneStarPercentage = ((double) oneStarCount / totalReviews) * 100.0;

        UserEntity reviewee = userRepository.findById(revieweeId).orElse(null);
        if (reviewee == null) return;

        Optional<WorkerProfileEntity> workerOpt = workerProfileRepository.findById(revieweeId);
        Optional<EmployerProfileEntity> employerOpt = employerProfileRepository.findById(revieweeId);

        int scoreDelta = 0;
        String messageType = null;

        if (oneStarPercentage < 10.0) {
            scoreDelta = 2; // +2 điểm uy tín
            messageType = "REWARD";
        } else if (oneStarPercentage >= 20.0) {
            scoreDelta = -10; // -10 điểm uy tín
            messageType = "PENALTY";
        }

        if (scoreDelta == 0) {
            return; // 10% đến dưới 20%: giữ nguyên
        }

        int newScore = 100;
        if (workerOpt.isPresent()) {
            WorkerProfileEntity worker = workerOpt.get();
            int current = worker.getWorkerScore() != null ? worker.getWorkerScore() : 100;
            newScore = Math.max(0, Math.min(100, current + scoreDelta));
            worker.setWorkerScore(newScore);
            if (newScore < 50) {
                worker.setIsAvailable(false);
            }
            workerProfileRepository.save(worker);
        } else if (employerOpt.isPresent()) {
            EmployerProfileEntity employer = employerOpt.get();
            int current = employer.getEmployerScore() != null ? employer.getEmployerScore() : 100;
            newScore = Math.max(0, Math.min(100, current + scoreDelta));
            employer.setEmployerScore(newScore);
            employerProfileRepository.save(employer);
        }

        String formattedPercent = String.format("%.1f", oneStarPercentage);
        String monthLabel = lastMonth.getMonthValue() + "/" + lastMonth.getYear();

        if ("REWARD".equals(messageType)) {
            notificationService.sendNotification(
                    reviewee,
                    "🎉 Thưởng uy tín tháng " + monthLabel,
                    "Chúc mừng bạn! Trong tháng qua bạn duy trì chất lượng xuất sắc với tỷ lệ 1 sao chỉ " + formattedPercent + "% (< 10% trên " + totalReviews + " đánh giá). Hệ thống đã cộng thưởng +2 điểm uy tín (Điểm hiện tại: " + newScore + "/100).",
                    "SYSTEM",
                    null
            );
        } else if ("PENALTY".equals(messageType)) {
            String penaltyDetail = (newScore < 50) ? " Do điểm uy tín dưới 50, tài khoản của bạn đã tạm thời bị tắt chế độ nhận việc." : "";
            notificationService.sendNotification(
                    reviewee,
                    "⚠️ Cảnh báo kỷ luật tháng " + monthLabel,
                    "Trong tháng qua tỷ lệ đánh giá 1 sao của bạn là " + formattedPercent + "% (>= 20% trên " + totalReviews + " đánh giá). Bạn bị trừ 10 điểm uy tín (Điểm hiện tại: " + newScore + "/100)." + penaltyDetail + " Vui lòng cải thiện chất lượng phục vụ!",
                    "SYSTEM",
                    null
            );
        }
    }

    private ResponseReviewDTO mapToDTO(ReviewEntity entity) {
        String targetRole = "WORKER";
        String jobTitle = null;
        if (entity.getApplication() != null && entity.getApplication().getJob() != null) {
            jobTitle = entity.getApplication().getJob().getTitle();
            if (entity.getApplication().getJob().getEmployer() != null &&
                    entity.getReviewee() != null &&
                    entity.getReviewee().getId().equals(entity.getApplication().getJob().getEmployer().getUserId())) {
                targetRole = "EMPLOYER";
            }
        }
        return ResponseReviewDTO.builder()
                .id(entity.getId())
                .applicationId(entity.getApplication() != null ? entity.getApplication().getId() : null)
                .reviewerId(entity.getReviewer() != null ? entity.getReviewer().getId() : null)
                .reviewerName(entity.getReviewer() != null ? entity.getReviewer().getFullName() : null)
                .reviewerAvatarUrl(entity.getReviewer() != null ? entity.getReviewer().getAvatarUrl() : null)
                .revieweeId(entity.getReviewee() != null ? entity.getReviewee().getId() : null)
                .revieweeName(entity.getReviewee() != null ? entity.getReviewee().getFullName() : null)
                .targetRole(targetRole)
                .jobTitle(jobTitle)
                .rating(entity.getRating())
                .comment(entity.getComment())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
