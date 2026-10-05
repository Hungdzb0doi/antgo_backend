package com.flashjobweb.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flashjobweb.dto.request.RequestReportDTO;
import com.flashjobweb.entity.ApplicationEntity;
import com.flashjobweb.entity.ReportEntity;
import com.flashjobweb.entity.UserEntity;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.ApplicationRepository;
import com.flashjobweb.repository.ReportRepository;
import com.flashjobweb.repository.UserRepository;
import com.flashjobweb.service.NotificationService;
import com.flashjobweb.service.ReportService;
import com.flashjobweb.service.impl.outside.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GoogleDriveService googleDriveService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void createReport(RequestReportDTO requestReportDTO) {
        if (requestReportDTO == null || requestReportDTO.getApplicationId() == null ||
                requestReportDTO.getReason() == null || requestReportDTO.getReason().trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity reporter = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        ApplicationEntity application = applicationRepository.findById(requestReportDTO.getApplicationId())
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        UserEntity reported;
        boolean isWorker = application.getWorker().getUserId().equals(reporter.getId());
        boolean isEmployer = application.getJob().getEmployer().getUserId().equals(reporter.getId());

        if (isWorker) {
            reported = application.getJob().getEmployer().getUserEntity();
        } else if (isEmployer) {
            reported = application.getWorker().getUserEntity();
        } else {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ReportEntity report = new ReportEntity();
        report.setReporter(reporter);
        report.setReported(reported);
        report.setApplication(application);
        report.setReason(requestReportDTO.getReason().trim());

        // Xử lý upload danh sách ảnh và video bằng chứng lên Google Drive
        if (requestReportDTO.getFiles() != null && !requestReportDTO.getFiles().isEmpty()) {
            List<Map<String, String>> evidenceList = new ArrayList<>();
            for (MultipartFile file : requestReportDTO.getFiles()) {
                if (file != null && !file.isEmpty()) {
                    try {
                        String fileId = googleDriveService.uploadReportFile(file);
                        String contentType = file.getContentType();
                        boolean isVideo = (contentType != null && contentType.toLowerCase().startsWith("video"))
                                || (file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().matches(".*\\.(mp4|mov|avi|mkv|webm)$"));

                        Map<String, String> item = new HashMap<>();
                        item.put("type", isVideo ? "VIDEO" : "IMAGE");
                        item.put("name", file.getOriginalFilename());
                        item.put("fileId", fileId);
                        item.put("url", "https://drive.google.com/file/d/" + fileId + "/view");
                        evidenceList.add(item);
                        log.info("Đã tải bằng chứng ({}) lên Google Drive: fileId={}", isVideo ? "VIDEO" : "IMAGE", fileId);
                    } catch (Exception e) {
                        log.error("Lỗi khi tải file bằng chứng lên Google Drive: {}", e.getMessage(), e);
                        throw new RuntimeException("Lỗi tải tệp bằng chứng lên Google Drive: " + e.getMessage());
                    }
                }
            }
            if (!evidenceList.isEmpty()) {
                try {
                    report.setEvidenceUrls(objectMapper.writeValueAsString(evidenceList));
                } catch (Exception e) {
                    log.error("Lỗi parse JSON evidence: {}", e.getMessage());
                }
            }
        }

        reportRepository.save(report);

        // Đánh dấu tranh chấp trực tiếp lên đơn ứng tuyển
        application.setIsDisputed(true);
        applicationRepository.save(application);

        log.info("Người dùng {} đã gửi khiếu nại cho đơn {} đối với {}", reporter.getId(), application.getId(), reported.getId());

        // Thông báo xác nhận cho người khiếu nại
        notificationService.sendNotification(
                reporter,
                "Khiếu nại đã được ghi nhận",
                "Hệ thống đã tiếp nhận khiếu nại của bạn về công việc '" + application.getJob().getTitle() + "'. Quản trị viên sẽ xử lý trong thời gian sớm nhất.",
                "APPLICATION",
                application.getJob().getId()
        );

        // Thông báo cảnh báo cho người bị khiếu nại
        notificationService.sendNotification(
                reported,
                "Cảnh báo khiếu nại phát sinh",
                "Đối tác đã gửi báo cáo/khiếu nại về công việc '" + application.getJob().getTitle() + "' với lý do: " + requestReportDTO.getReason().trim(),
                "APPLICATION",
                application.getJob().getId()
        );
    }
}
