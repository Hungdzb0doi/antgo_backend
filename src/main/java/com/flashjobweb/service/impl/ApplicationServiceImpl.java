package com.flashjobweb.service.impl;

import com.flashjobweb.dto.request.RequestApplicationDTO;
import com.flashjobweb.dto.request.RequestInvitationDTO;
import com.flashjobweb.dto.request.RequestScanQrDTO;
import com.flashjobweb.dto.response.ResponseApplicationEmployerDTO;
import com.flashjobweb.dto.response.ResponseApplicationWorkerDTO;
import com.flashjobweb.dto.response.ResponseNotificationDTO;
import com.flashjobweb.dto.response.ResponseScanQrDTO;
import com.flashjobweb.entity.ApplicationEntity;
import com.flashjobweb.entity.JobEntity;
import com.flashjobweb.entity.NotificationEntity;
import com.flashjobweb.entity.ReviewEntity;
import com.flashjobweb.entity.UserEntity;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.ApplicationRepository;
import com.flashjobweb.repository.JobRepository;
import com.flashjobweb.repository.ReportRepository;
import com.flashjobweb.repository.ReviewRepository;
import com.flashjobweb.repository.UserRepository;
import com.flashjobweb.service.ApplicationService;
import com.flashjobweb.service.NotificationService;
import com.flashjobweb.util.ApplicationStatus;
import com.flashjobweb.util.ApplicationType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ReviewRepository reviewRepository;
    private final ReportRepository reportRepository;
    @Override
    public List<ResponseApplicationWorkerDTO> getApplicationsForCurrentUser(ApplicationStatus status, ApplicationType type, String keyword) {
        String curentPhone= SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user=userRepository.findByPhone(curentPhone).orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        List<ApplicationEntity> applications = applicationRepository.findApplicationsForWorker(user.getId(), status, type, cleanKeyword);

        return applications.stream().map(app -> {
            ResponseApplicationWorkerDTO dto = new ResponseApplicationWorkerDTO();

            dto.setApplicationId(app.getId());
            dto.setJobId(app.getJob().getId());
            dto.setJobTitle(app.getJob().getTitle());
            dto.setEmployerName(app.getJob().getEmployer().getEmployerName());
            dto.setStartTime(app.getJob().getStartTime());
            dto.setEndTime(app.getJob().getEndTime());
            dto.setHourlyRate(app.getJob().getHourlyRate());
            dto.setStatus(app.getStatus().name());
            dto.setType(app.getType().name());
            dto.setEarnedAmount(app.getEarnedAmount());
            dto.setWorkerConfirmed(app.getWorkerConfirmed());
            dto.setEmployerConfirmed(app.getEmployerConfirmed());
            dto.setCheckInAt(app.getCheckInAt());
            dto.setCheckOutAt(app.getCheckOutAt());

            Optional<ReviewEntity> myReview = reviewRepository.findByApplicationIdAndReviewerId(app.getId(), user.getId());
            dto.setIsReviewed(myReview.isPresent());
            dto.setReviewRating(myReview.map(ReviewEntity::getRating).orElse(null));
            dto.setReviewComment(myReview.map(ReviewEntity::getComment).orElse(null));
            dto.setIsDisputed(Boolean.TRUE.equals(app.getIsDisputed()) || reportRepository.existsByApplicationId(app.getId()));

            if (app.getJob() != null && app.getJob().getLocation() != null) {
                dto.setLatitude(app.getJob().getLocation().getY());
                dto.setLongitude(app.getJob().getLocation().getX());
            }

            if (app.getJob() != null && app.getJob().getEmployer() != null && app.getJob().getEmployer().getUserEntity() != null) {
                dto.setEmployerPhone(app.getJob().getEmployer().getUserEntity().getPhone());
            }

            return dto;
        }).collect(Collectors.toList());
    }
    @Override
    public void respondToInvitation( RequestInvitationDTO request) {


        ApplicationEntity application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        String currentPhone= SecurityContextHolder.getContext().getAuthentication().getName();
        UUID workerId = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)).getId();
        if (!application.getWorker().getUserId().equals(workerId)) {
            throw new AppException(ErrorCode.APPLICATION_NOT_BELONG_TO_WORKER);
        }


        if (application.getStatus() != ApplicationStatus.PENDING ||
                application.getType() != ApplicationType.INVITATION) {
            throw new AppException(ErrorCode.APPLICATION_IN_DIFFERENT_STATUS);
        }

        JobEntity job = application.getJob();

        String actionText;
        if (request.isAccepted()) {
            if (application.getWorker().getWorkerScore() != null && application.getWorker().getWorkerScore() < 50) {
                throw new AppException(ErrorCode.REPUTATION_SCORE_TOO_LOW);
            }

            if (job.getEmployer() != null && job.getEmployer().getEmployerScore() != null && job.getEmployer().getEmployerScore() < 50) {
                throw new AppException(ErrorCode.REPUTATION_SCORE_TOO_LOW);
            }

            if ("CANCELLED".equalsIgnoreCase(job.getStatus())) {
                application.setStatus(ApplicationStatus.CANCELLED);
                applicationRepository.save(application);
                throw new AppException(ErrorCode.JOB_ALREADY_CANCELLED);
            }

            if ("COMPLETED".equalsIgnoreCase(job.getStatus())) {
                application.setStatus(ApplicationStatus.CANCELLED);
                applicationRepository.save(application);
                throw new AppException(ErrorCode.JOB_ALREADY_COMPLETED);
            }

            if (!"OPEN".equalsIgnoreCase(job.getStatus())) {
                application.setStatus(ApplicationStatus.CANCELLED);
                applicationRepository.save(application);
                throw new AppException(ErrorCode.JOB_NOT_OPEN);
            }

            Long currentBooked = applicationRepository.countByJob_IdAndStatus(job.getId(), ApplicationStatus.BOOKED);
            if (currentBooked != null && currentBooked >= job.getRequiredWorkers()) {
                application.setStatus(ApplicationStatus.CANCELLED);
                applicationRepository.save(application);
                throw new AppException(ErrorCode.JOB_FULL);
            }

            Long overlappingCount = applicationRepository.existsOverlappingJob(
                    workerId,
                    job.getStartTime(),
                    job.getEndTime(),
                    ApplicationStatus.BOOKED
            );

            if (overlappingCount != null && overlappingCount > 0) {
                throw new AppException(ErrorCode.HAVING_APPLICATION_BOOKED);
            }

            application.setStatus(ApplicationStatus.BOOKED);
            actionText = "đã ĐỒNG Ý";
        } else {
            application.setStatus(ApplicationStatus.CANCELLED);
            actionText = "đã TỪ CHỐI";
        }

        applicationRepository.save(application);


        UUID employerId = application.getJob().getEmployer().getUserId();
        String workerName = application.getWorker().getUserEntity().getFullName();
        String jobTitle = application.getJob().getTitle();
        String notifTitle = "Phản hồi lời mời làm việc";
        String notifContent = "Ứng viên " + workerName + " " + actionText + " lời mời cho công việc: " + jobTitle;


        notificationService.sendNotification(application.getJob().getEmployer().getUserEntity(), notifTitle, notifContent, "APPLICATION", application.getJob().getId());
    }
    private final JobRepository jobRepository;
    @Override
    public List<ResponseApplicationEmployerDTO> getApplicationsForJob(UUID jobId) {

        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity currentUser = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        if (job.getEmployer() == null || !job.getEmployer().getUserId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.JOB_NOT_BELONG_TO_EMPLOYER);
        }

        List<ApplicationEntity> applications = applicationRepository.findApplicationsByJobId(jobId);

        return applications.stream().map(app -> {
            ResponseApplicationEmployerDTO dto = new ResponseApplicationEmployerDTO();
            dto.setApplicationId(app.getId());
            dto.setWorkerId(app.getWorker().getUserId());
            dto.setWorkerName(app.getWorker().getUserEntity().getFullName());
            dto.setWorkerAvatar(app.getWorker().getUserEntity().getAvatarUrl());
            dto.setWorkerScore(app.getWorker().getWorkerScore());
            dto.setPhone(app.getWorker().getUserEntity().getPhone());
            dto.setStatus(app.getStatus().name());
            dto.setType(app.getType().name());
            dto.setCreatedAt(app.getCreatedAt());
            dto.setIdentityVerified(app.getWorker().getUserEntity().getIdentityVerified());
            dto.setJobTitle(job.getTitle());
            dto.setJobStatus(job.getStatus());

            Optional<ReviewEntity> myReview = reviewRepository.findByApplicationIdAndReviewerId(app.getId(), currentUser.getId());
            dto.setIsReviewed(myReview.isPresent());
            dto.setReviewRating(myReview.map(ReviewEntity::getRating).orElse(null));
            dto.setReviewComment(myReview.map(ReviewEntity::getComment).orElse(null));
            dto.setIsDisputed(Boolean.TRUE.equals(app.getIsDisputed()) || reportRepository.existsByApplicationId(app.getId()));
            dto.setEmployerConfirmed(app.getEmployerConfirmed());
            dto.setWorkerConfirmed(app.getWorkerConfirmed());
            dto.setEarnedAmount(app.getEarnedAmount());
            dto.setCheckOutAt(app.getCheckOutAt());

            return dto;
        }).toList();
    }
    @Override
    public void respondToApplication(RequestApplicationDTO request) {

        ApplicationEntity application = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UUID employerId = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND)).getId();

        if (!application.getJob().getEmployer().getUserId().equals(employerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        JobEntity job = application.getJob();

        if ("CANCELLED".equalsIgnoreCase(job.getStatus())) {
            application.setStatus(ApplicationStatus.CANCELLED);
            applicationRepository.save(application);
            throw new AppException(ErrorCode.JOB_ALREADY_CANCELLED);
        }

        if ("COMPLETED".equalsIgnoreCase(job.getStatus())) {
            application.setStatus(ApplicationStatus.CANCELLED);
            applicationRepository.save(application);
            throw new AppException(ErrorCode.JOB_ALREADY_COMPLETED);
        }

        if (!"OPEN".equalsIgnoreCase(job.getStatus())) {
            application.setStatus(ApplicationStatus.CANCELLED);
            applicationRepository.save(application);
            throw new AppException(ErrorCode.JOB_NOT_OPEN);
        }

        if (application.getStatus() != ApplicationStatus.PENDING ||
                application.getType() != ApplicationType.APPLICATION) {
            throw new AppException(ErrorCode.APPLICATION_IN_DIFFERENT_STATUS);
        }


        String actionText;
        if (request.isAccepted()) {
            if (job.getEmployer() != null && job.getEmployer().getEmployerScore() != null && job.getEmployer().getEmployerScore() < 50) {
                throw new AppException(ErrorCode.REPUTATION_SCORE_TOO_LOW);
            }

            if (application.getWorker().getWorkerScore() != null && application.getWorker().getWorkerScore() < 50) {
                throw new AppException(ErrorCode.REPUTATION_SCORE_TOO_LOW);
            }

            application.setStatus(ApplicationStatus.BOOKED);
            actionText = "đã CHẤP NHẬN";
        } else {
            application.setStatus(ApplicationStatus.CANCELLED);
            actionText = "đã TỪ CHỐI";
        }

        applicationRepository.save(application);


        UUID workerId = application.getWorker().getUserId();
        String jobTitle = application.getJob().getTitle();
        String notifTitle = "Kết quả ứng tuyển";
        String notifContent = "Nhà tuyển dụng " + actionText + " đơn ứng tuyển của bạn cho công việc: " + jobTitle;

        notificationService.sendNotification(application.getWorker().getUserEntity(), notifTitle, notifContent, "APPLICATION", application.getJob().getId());
    }

    @Override
    public ResponseScanQrDTO scanQr(RequestScanQrDTO request) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - request.getTimestamp() > 30000) {
            throw new AppException(ErrorCode.QR_CODE_EXPIRED);
        }

        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity currentUser = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        JobEntity job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        if (job.getEmployer() != null && job.getEmployer().getUserId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.CANNOT_APPLY_OWN_JOB);
        }

        ApplicationEntity application = applicationRepository.findByJobIdAndWorkerId(request.getJobId(), currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_BELONG_TO_WORKER));

        String workerName = currentUser.getFullName();
        String timeString = "";
        Map<String, Object> socketPayload = new HashMap<>();

        Double hoursWorked = null;
        BigDecimal totalEarned = null;

        if ("CHECKIN".equalsIgnoreCase(request.getAction())) {
            if (application.getStatus() != ApplicationStatus.BOOKED) {
                throw new AppException(ErrorCode.APPLICATION_IN_DIFFERENT_STATUS);
            }

            application.setCheckInAt(LocalDateTime.now());
            application.setStatus(ApplicationStatus.IN_PROGRESS);
            timeString = application.getCheckInAt().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            if ("OPEN".equals(job.getStatus())) {
                job.setStatus("IN_PROGRESS");
                jobRepository.save(job);
            }

        } else if ("CHECKOUT".equalsIgnoreCase(request.getAction())) {
            if (application.getStatus() != ApplicationStatus.IN_PROGRESS) {
                throw new AppException(ErrorCode.APPLICATION_IN_DIFFERENT_STATUS);
            }

            LocalDateTime now = LocalDateTime.now();
            application.setCheckOutAt(now);
            timeString = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            long minutesWorked = Duration.between(application.getCheckInAt(), now).toMinutes();
            hoursWorked = Math.round(((double) minutesWorked / 60.0) * 100.0) / 100.0;

            BigDecimal hoursDecimal = BigDecimal.valueOf(hoursWorked);
            totalEarned = job.getHourlyRate().multiply(hoursDecimal).setScale(0, RoundingMode.HALF_UP);

            application.setEarnedAmount(totalEarned);

            socketPayload.put("applicationId", application.getId().toString());
            socketPayload.put("hourlyRate", job.getHourlyRate().toString());
            socketPayload.put("hoursWorked", String.valueOf(hoursWorked));
            socketPayload.put("totalEarned", String.valueOf(totalEarned));

        } else {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        applicationRepository.save(application);

        socketPayload.put("workerName", workerName);
        socketPayload.put("action", request.getAction());
        socketPayload.put("time", timeString);

        messagingTemplate.convertAndSend("/topic/job/" + request.getJobId(), socketPayload);

        return ResponseScanQrDTO.builder()
                .applicationId(application.getId())
                .jobId(job.getId())
                .workerName(workerName)
                .action(request.getAction())
                .time(timeString)
                .hoursWorked(hoursWorked)
                .hourlyRate(job.getHourlyRate())
                .totalEarned(totalEarned)
                .workerConfirmed(application.getWorkerConfirmed())
                .employerConfirmed(application.getEmployerConfirmed())
                .build();
    }

    @Override
    public void confirmPayment(UUID applicationId) {
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity currentUser = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ApplicationEntity application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        if (!application.getWorker().getUserId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        application.setWorkerConfirmed(true);

        // Chỉ khi CẢ 2 BÊN xác nhận (Thợ nhận đủ tiền + Chủ hài lòng) thì mới KẾT THÚC (COMPLETED)
        boolean bothConfirmed = Boolean.TRUE.equals(application.getEmployerConfirmed());
        if (bothConfirmed) {
            application.setStatus(ApplicationStatus.COMPLETED);
        }

        applicationRepository.save(application);

        if (bothConfirmed) {
            checkAndCompleteJob(application.getJob());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "WORKER_CONFIRMED_PAYMENT");
        payload.put("applicationId", applicationId.toString());
        payload.put("workerName", currentUser.getFullName());
        payload.put("isCompleted", bothConfirmed);
        messagingTemplate.convertAndSend("/topic/job/" + application.getJob().getId(), payload);
    }

    @Override
    public void confirmSatisfaction(UUID applicationId) {
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity currentUser = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ApplicationEntity application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        if (!application.getJob().getEmployer().getUserId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        application.setEmployerConfirmed(true);

        // Chỉ khi CẢ 2 BÊN xác nhận (Thợ nhận đủ tiền + Chủ hài lòng) thì mới KẾT THÚC (COMPLETED)
        boolean bothConfirmed = Boolean.TRUE.equals(application.getWorkerConfirmed());
        if (bothConfirmed) {
            application.setStatus(ApplicationStatus.COMPLETED);
        }

        applicationRepository.save(application);

        if (bothConfirmed) {
            checkAndCompleteJob(application.getJob());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("action", "EMPLOYER_CONFIRMED_SATISFACTION");
        payload.put("applicationId", applicationId.toString());
        payload.put("isCompleted", bothConfirmed);
        messagingTemplate.convertAndSend("/topic/job/" + application.getJob().getId(), payload);

        notificationService.sendNotification(
                application.getWorker().getUserEntity(),
                "Nhà tuyển dụng đã xác nhận hài lòng!",
                "Nhà tuyển dụng công việc " + application.getJob().getTitle() + " đã xác nhận hài lòng về ca làm của bạn.",
                "APPLICATION",
                application.getJob().getId()
        );
    }

    private void checkAndCompleteJob(JobEntity job) {
        Long inProgressCount = applicationRepository.countByJob_IdAndStatus(job.getId(), ApplicationStatus.IN_PROGRESS);
        Long bookedCount = applicationRepository.countByJob_IdAndStatus(job.getId(), ApplicationStatus.BOOKED);
        if ((inProgressCount == null || inProgressCount <= 0) && (bookedCount == null || bookedCount <= 0)) {
            job.setStatus("COMPLETED");
            jobRepository.save(job);
        }
    }

    @Override
    public String generateQrData(UUID jobId, String action) {
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity currentUser = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_NOT_FOUND));

        // Chỉ Nhà tuyển dụng tạo công việc này mới có quyền tạo mã QR điểm danh
        if (job.getEmployer() == null || !job.getEmployer().getUserId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.JOB_NOT_BELONG_TO_EMPLOYER);
        }

        if ("CANCELLED".equalsIgnoreCase(job.getStatus())) {
            throw new AppException(ErrorCode.JOB_ALREADY_CANCELLED);
        }

        long serverTime = System.currentTimeMillis();
        return String.format("{\"jobId\":\"%s\",\"action\":\"%s\",\"timestamp\":%d}",
                jobId.toString(), action, serverTime);
    }
}
