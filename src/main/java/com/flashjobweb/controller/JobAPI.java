package com.flashjobweb.controller;

import com.flashjobweb.dto.request.RequestJobDTO;
import com.flashjobweb.service.JobService;
import com.flashjobweb.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/job")
public class JobAPI {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Object>> createJob(@RequestBody RequestJobDTO requestJobDTO){
        jobService.createJob(requestJobDTO);
        return ResponseEntity.ok(ApiResponse.created());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getAllJobs(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword){
        return ResponseEntity.ok(ApiResponse.success(jobService.getAllJobs(status, keyword)));
    }

    @GetMapping("/nearby")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Object>> getNearbyWorkers(@RequestParam UUID jobId , @RequestParam(defaultValue = "5.0") double radiusInKm){
        return ResponseEntity.ok(ApiResponse.success(jobService.findAllWorkerNearbyJob(jobId,radiusInKm)));
    }

    @PostMapping("/invitation")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Object>> sendInvitation(@RequestParam UUID workerId, @RequestParam UUID jobId){
        jobService.inviteWorker( jobId,workerId);
        return ResponseEntity.ok(ApiResponse.created());
    }

    @GetMapping("/nearbyjob")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<ApiResponse<Object>> findNearbyJobs(@RequestParam double lat, @RequestParam double lng, @RequestParam(defaultValue = "5.0") double radiusInKm){
        return ResponseEntity.ok(ApiResponse.success(jobService.findNearbyJobs(lat, lng, radiusInKm)));
    }

    @PostMapping("/application")
    @PreAuthorize("hasRole('WORKER')")
    public ResponseEntity<ApiResponse<Object>> applyForJob(@RequestParam UUID jobId){
        jobService.applyForJob(jobId);
        return ResponseEntity.ok(ApiResponse.created());
    }

    @PutMapping("/cancellation")
    @PreAuthorize("hasRole('EMPLOYER')")
    public ResponseEntity<ApiResponse<Object>> cancelJob(@RequestParam UUID jobId) {
        jobService.cancelJob(jobId);
        return ResponseEntity.ok(ApiResponse.success("Hủy công việc thành công"));
    }
}
