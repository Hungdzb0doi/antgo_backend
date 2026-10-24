package com.flashjobweb.controller;

import com.flashjobweb.dto.request.RequestReportDTO;
import com.flashjobweb.service.ReportService;
import com.flashjobweb.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportAPI {

    private final ReportService reportService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> createReportMultipart(
            @RequestParam("applicationId") UUID applicationId,
            @RequestParam("reason") String reason,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        RequestReportDTO requestReportDTO = new RequestReportDTO();
        requestReportDTO.setApplicationId(applicationId);
        requestReportDTO.setReason(reason);
        requestReportDTO.setFiles(files);
        reportService.createReport(requestReportDTO);
        return ResponseEntity.ok(ApiResponse.created());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<Object>> createReportJson(@RequestBody RequestReportDTO requestReportDTO) {
        reportService.createReport(requestReportDTO);
        return ResponseEntity.ok(ApiResponse.created());
    }
}

