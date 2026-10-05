package com.flashjobweb.controller;

import com.flashjobweb.dto.request.RequestApplicationDTO;
import com.flashjobweb.dto.request.RequestInvitationDTO;
import com.flashjobweb.dto.request.RequestScanQrDTO;
import com.flashjobweb.service.ApplicationService;
import com.flashjobweb.util.ApiResponse;
import com.flashjobweb.util.ApplicationStatus;
import com.flashjobweb.util.ApplicationType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/application")
@RequiredArgsConstructor
public class ApplicationAPI {
    private final ApplicationService applicationService;

    @GetMapping
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<ApiResponse<Object>> getApplicationForCurrentUser(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) ApplicationType type,
            @RequestParam(required = false) String keyword){

        return ResponseEntity.ok(ApiResponse.<Object>builder()
                .code(200)
                .message("success")
                .data(applicationService.getApplicationsForCurrentUser(status, type, keyword))
                .build());

    }

    @PutMapping("/isacceptinvitation")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<ApiResponse<Object>> isAcceptInvitation(@RequestBody RequestInvitationDTO requestInvitationDTO){
        applicationService.respondToInvitation(requestInvitationDTO);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/job")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Object>> getApplicationForJob(@RequestParam UUID jobId){
        return ResponseEntity.ok(ApiResponse.success(applicationService.getApplicationsForJob(jobId)));
    }

    @PutMapping("/isacceptapplication")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Object>> isAcceptApplication(@RequestBody RequestApplicationDTO requestApplicationDTO){
        applicationService.respondToApplication(requestApplicationDTO);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/qrscanner")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<ApiResponse<Object>> scanQr(@RequestBody RequestScanQrDTO requestScanQrDTO){
        return ResponseEntity.ok(ApiResponse.success(applicationService.scanQr(requestScanQrDTO)));
    }

    @PutMapping("/payment")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<ApiResponse<Object>> confirmPayment(@RequestParam UUID applicationId){
        applicationService.confirmPayment(applicationId);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận đã nhận tiền thành công"));
    }

    @PutMapping("/satisfaction")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Object>> confirmSatisfaction(@RequestParam UUID applicationId){
        applicationService.confirmSatisfaction(applicationId);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận hài lòng thành công"));
    }

    @GetMapping("/qrdata")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Object>> generateQrData(
            @RequestParam UUID jobId,
            @RequestParam String action) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.generateQrData(jobId, action)));
    }
}
