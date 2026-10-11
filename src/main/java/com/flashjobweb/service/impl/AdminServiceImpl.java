package com.flashjobweb.service.impl;

import com.flashjobweb.dto.request.RequestCategoryDTO;
import com.flashjobweb.dto.response.*;
import com.flashjobweb.entity.*;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.*;
import com.flashjobweb.service.AdminService;
import com.flashjobweb.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final EkycRepository ekycRepository;
    private final ReportRepository reportRepository;
    private final JobCategoryRepository jobCategoryRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    public ResponseAdminDashboardDTO getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalWorkers = userRepository.countByCurrentMode("WORKER");
        long totalEmployers = userRepository.countByCurrentMode("EMPLOYER");
        long verifiedUsers = userRepository.countByIdentityVerified(true);
        long totalJobs = jobRepository.count();
        long activeJobs = jobRepository.countByStatus("OPEN") + jobRepository.countByStatus("IN_PROGRESS");
        long pendingEkycCount = ekycRepository.countByStatus("PENDING");
        long pendingReportsCount = reportRepository.countByStatus("PENDING");

        return ResponseAdminDashboardDTO.builder()
                .totalUsers(totalUsers)
                .totalWorkers(totalWorkers)
                .totalEmployers(totalEmployers)
                .verifiedUsers(verifiedUsers)
                .totalJobs(totalJobs)
                .activeJobs(activeJobs)
                .pendingEkycCount(pendingEkycCount)
                .pendingReportsCount(pendingReportsCount)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseAdminUserDTO> getAllUsers(String search) {
        List<UserEntity> users;
        if (StringUtils.hasText(search)) {
            String query = search.trim();
            users = userRepository.findByFullNameContainingIgnoreCaseOrPhoneContaining(query, query);
        } else {
            users = userRepository.findAllByOrderByCreatedAtDesc();
        }

        return users.stream().map(user -> {
            Integer workerScore = workerProfileRepository.findById(user.getId())
                    .map(WorkerProfileEntity::getWorkerScore)
                    .orElse(null);
            Integer employerScore = employerProfileRepository.findById(user.getId())
                    .map(EmployerProfileEntity::getEmployerScore)
                    .orElse(null);

            return ResponseAdminUserDTO.builder()
                    .id(user.getId())
                    .fullName(user.getFullName())
                    .phone(user.getPhone())
                    .email(user.getEmail())
                    .currentMode(user.getCurrentMode())
                    .role(user.getRole())
                    .status(user.getStatus())
                    .identityCard(user.getIdentityCard())
                    .identityVerified(user.getIdentityVerified())
                    .workerScore(workerScore)
                    .employerScore(employerScore)
                    .createdAt(user.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public void toggleUserStatus(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new AppException(ErrorCode.CANNOT_BAN_ADMIN);
        }

        if ("ACTIVE".equalsIgnoreCase(user.getStatus())) {
            user.setStatus("BANNED");
        } else {
            user.setStatus("ACTIVE");
        }
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseAdminEkycDTO> getEkycRecords(String status) {
        List<EkycEntity> list;
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            list = ekycRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
        } else {
            list = ekycRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(record -> ResponseAdminEkycDTO.builder()
                .id(record.getId())
                .userId(record.getUser() != null ? record.getUser().getId() : null)
                .userName(record.getUser() != null ? record.getUser().getFullName() : "N/A")
                .userPhone(record.getUser() != null ? record.getUser().getPhone() : "N/A")
                .idCardFrontUrl(normalizeMediaUrl(record.getIdCardFrontUrl()))
                .idCardBackUrl(normalizeMediaUrl(record.getIdCardBackUrl()))
                .faceImageUrl(normalizeMediaUrl(record.getFaceImageUrl()))
                .status(record.getStatus())
                .createdAt(record.getCreatedAt())
                .build()
        ).collect(Collectors.toList());
    }

    private String normalizeMediaUrl(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("/")) {
            return trimmed;
        }
        return "/api/v1/messages/media/" + trimmed;
    }

    @Override
    public void processEkyc(UUID ekycId, String decision, String note) {
        EkycEntity ekyc = ekycRepository.findById(ekycId)
                .orElseThrow(() -> new AppException(ErrorCode.EKYC_NOT_FOUND));

        String finalStatus = "APPROVED".equalsIgnoreCase(decision) ? "APPROVED" : "REJECTED";
        ekyc.setStatus(finalStatus);
        ekycRepository.save(ekyc);

        UserEntity user = ekyc.getUser();
        if (user != null) {
            if ("APPROVED".equals(finalStatus)) {
                user.setIdentityVerified(true);
                userRepository.save(user);
                notificationService.sendNotification(user,
                        "Xác thực eKYC thành công",
                        "Hồ sơ eKYC của bạn đã được Quản trị viên duyệt thành công. Tài khoản của bạn đã được xác minh tích xanh!",
                        "EKYC_APPROVED");
            } else {
                user.setIdentityVerified(false);
                userRepository.save(user);
                String reason = StringUtils.hasText(note) ? note : "Hình ảnh giấy tờ hoặc khuôn mặt không đạt yêu cầu.";
                notificationService.sendNotification(user,
                        "Hồ sơ eKYC không được phê duyệt",
                        "Hồ sơ eKYC của bạn đã bị từ chối. Lý do: " + reason + ". Vui lòng chụp lại rõ nét và gửi lại yêu cầu.",
                        "EKYC_REJECTED");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseAdminReportDTO> getReports(String status) {
        List<ReportEntity> list;
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            list = reportRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase());
        } else {
            list = reportRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream().map(r -> {
            ApplicationEntity app = r.getApplication();
            JobEntity job = app != null ? app.getJob() : null;

            String jobTitle = job != null ? job.getTitle() : "N/A";
            LocalDateTime startTime = job != null ? job.getStartTime() : null;
            LocalDateTime endTime = job != null ? job.getEndTime() : null;
            BigDecimal hourlyRate = job != null ? job.getHourlyRate() : null;

            LocalDateTime checkInAt = app != null ? app.getCheckInAt() : null;
            LocalDateTime checkOutAt = app != null ? app.getCheckOutAt() : null;

            BigDecimal paymentAmount = null;
            if (app != null && app.getEarnedAmount() != null) {
                paymentAmount = app.getEarnedAmount();
            } else if (checkInAt != null && checkOutAt != null && hourlyRate != null) {
                double hours = (double) Duration.between(checkInAt, checkOutAt).toMinutes() / 60.0;
                paymentAmount = hourlyRate.multiply(BigDecimal.valueOf(hours)).setScale(0, java.math.RoundingMode.HALF_UP);
            } else if (startTime != null && endTime != null && hourlyRate != null) {
                double hours = (double) Duration.between(startTime, endTime).toMinutes() / 60.0;
                paymentAmount = hourlyRate.multiply(BigDecimal.valueOf(hours)).setScale(0, java.math.RoundingMode.HALF_UP);
            }

            return ResponseAdminReportDTO.builder()
                    .id(r.getId())
                    .reporterId(r.getReporter() != null ? r.getReporter().getId() : null)
                    .reporterName(r.getReporter() != null ? r.getReporter().getFullName() : "N/A")
                    .reporterPhone(r.getReporter() != null ? r.getReporter().getPhone() : "N/A")
                    .reportedId(r.getReported() != null ? r.getReported().getId() : null)
                    .reportedName(r.getReported() != null ? r.getReported().getFullName() : "N/A")
                    .reportedPhone(r.getReported() != null ? r.getReported().getPhone() : "N/A")
                    .applicationId(app != null ? app.getId() : null)
                    .jobTitle(jobTitle)
                    .reason(r.getReason())
                    .evidenceUrls(r.getEvidenceUrls())
                    .status(r.getStatus())
                    .createdAt(r.getCreatedAt())
                    .startTime(startTime)
                    .endTime(endTime)
                    .checkInAt(checkInAt)
                    .checkOutAt(checkOutAt)
                    .hourlyRate(hourlyRate)
                    .paymentAmount(paymentAmount)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public void resolveReport(UUID reportId, String decision, Integer penaltyPoints, String note) {
        ReportEntity report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));

        if ("RESOLVED".equalsIgnoreCase(report.getStatus()) || "REJECTED".equalsIgnoreCase(report.getStatus())) {
            throw new AppException(ErrorCode.REPORT_ALREADY_RESOLVED);
        }

        String finalStatus = "RESOLVED".equalsIgnoreCase(decision) ? "RESOLVED" : "REJECTED";
        report.setStatus(finalStatus);
        reportRepository.save(report);

        UserEntity reported = report.getReported();
        UserEntity reporter = report.getReporter();

        ApplicationEntity app = report.getApplication();
        JobEntity job = (app != null) ? app.getJob() : null;
        UUID jobId = (job != null) ? job.getId() : null;
        String jobTitle = (job != null && StringUtils.hasText(job.getTitle())) ? job.getTitle() : "Công việc liên quan";

        String shiftInfo = "";
        if (job != null && job.getStartTime() != null && job.getEndTime() != null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
            shiftInfo = " (Ca làm: " + job.getStartTime().format(dtf) + " - " + job.getEndTime().format(dtf) + ")";
        }

        if ("RESOLVED".equalsIgnoreCase(finalStatus)) {
            int penalty = (penaltyPoints != null && penaltyPoints > 0) ? penaltyPoints : 0;
            if (penalty > 0 && reported != null) {
                workerProfileRepository.findById(reported.getId()).ifPresent(wp -> {
                    int current = (wp.getWorkerScore() != null) ? wp.getWorkerScore() : 100;
                    wp.setWorkerScore(Math.max(0, current - penalty));
                    workerProfileRepository.save(wp);
                });
                employerProfileRepository.findById(reported.getId()).ifPresent(ep -> {
                    int current = (ep.getEmployerScore() != null) ? ep.getEmployerScore() : 100;
                    ep.setEmployerScore(Math.max(0, current - penalty));
                    employerProfileRepository.save(ep);
                });

                String reason = StringUtils.hasText(note) ? note : (StringUtils.hasText(report.getReason()) ? report.getReason() : "Vi phạm quy định nền tảng");
                notificationService.sendNotification(reported,
                        "Cảnh báo vi phạm & Khấu trừ điểm uy tín",
                        "Bạn đã bị trừ " + penalty + " điểm uy tín liên quan đến công việc '" + jobTitle + "'" + shiftInfo + ". Lý do: " + reason,
                        "PENALTY",
                        jobId);
            }

            if (reporter != null) {
                String reportedName = (reported != null && StringUtils.hasText(reported.getFullName())) ? reported.getFullName() : "đối tác";
                notificationService.sendNotification(reporter,
                        "Báo cáo khiếu nại đã được giải quyết",
                        "Khiếu nại về đối tác " + reportedName + " tại công việc '" + jobTitle + "'" + shiftInfo + " đã được Ban Quản Trị xử lý vi phạm thành công.",
                        "REPORT_RESULT",
                        jobId);
            }
        } else {
            if (reporter != null) {
                String noteMsg = StringUtils.hasText(note) ? (" Căn cứ: " + note) : "";
                notificationService.sendNotification(reporter,
                        "Báo cáo khiếu nại đã được xem xét",
                        "Khiếu nại của bạn về công việc '" + jobTitle + "'" + shiftInfo + " đã được Ban Quản Trị xem xét và bác bỏ do chưa đủ căn cứ vi phạm." + noteMsg,
                        "REPORT_REJECTED",
                        jobId);
            }
            if (reported != null) {
                notificationService.sendNotification(reported,
                        "Khiếu nại liên quan đã được bác bỏ",
                        "Khiếu nại liên quan đến bạn tại công việc '" + jobTitle + "'" + shiftInfo + " đã được Ban Quản Trị xem xét và bác bỏ (không ghi nhận vi phạm).",
                        "REPORT_REJECTED",
                        jobId);
            }
        }

        // Gỡ cờ tranh chấp nếu không còn khiếu nại nào ở trạng thái PENDING
        if (app != null) {
            List<ReportEntity> remainingPending = reportRepository.findByStatus("PENDING").stream()
                    .filter(rep -> rep.getApplication() != null && rep.getApplication().getId().equals(app.getId()) && !rep.getId().equals(report.getId()))
                    .toList();
            if (remainingPending.isEmpty()) {
                app.setIsDisputed(false);
                applicationRepository.save(app);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResponseAdminCategoryDTO> getAllCategories() {
        return jobCategoryRepository.findAll().stream().map(cat ->
                ResponseAdminCategoryDTO.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .jobCount(cat.getJobEntities() != null ? cat.getJobEntities().size() : 0)
                        .build()
        ).collect(Collectors.toList());
    }

    @Override
    public ResponseJobCategoryDTO createCategory(RequestCategoryDTO dto) {
        String trimmedName = dto.getName().trim();
        if (jobCategoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new AppException(ErrorCode.JOB_CATEGORY_ALREADY_EXISTS);
        }

        JobCategoryEntity category = new JobCategoryEntity();
        category.setName(trimmedName);
        category = jobCategoryRepository.save(category);

        ResponseJobCategoryDTO response = new ResponseJobCategoryDTO();
        response.setId(category.getId());
        response.setName(category.getName());
        return response;
    }

    @Override
    public ResponseJobCategoryDTO updateCategory(UUID id, RequestCategoryDTO dto) {
        JobCategoryEntity category = jobCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_CATEGORY_NOT_FOUND));

        String trimmedName = dto.getName().trim();
        if (jobCategoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw new AppException(ErrorCode.JOB_CATEGORY_ALREADY_EXISTS);
        }

        category.setName(trimmedName);
        category = jobCategoryRepository.save(category);

        ResponseJobCategoryDTO response = new ResponseJobCategoryDTO();
        response.setId(category.getId());
        response.setName(category.getName());
        return response;
    }

    @Override
    public void deleteCategory(UUID id) {
        JobCategoryEntity category = jobCategoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.JOB_CATEGORY_NOT_FOUND));

        if (category.getJobEntities() != null && !category.getJobEntities().isEmpty()) {
            throw new AppException(ErrorCode.CATEGORY_HAS_JOBS);
        }

        jobCategoryRepository.delete(category);
    }
}
