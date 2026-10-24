package com.flashjobweb.service;

import com.flashjobweb.dto.request.RequestInvitationDTO;
import com.flashjobweb.dto.request.RequestJobDTO;
import com.flashjobweb.dto.response.ResponseJobDTO;
import com.flashjobweb.dto.response.ResponseWorkerDTO;

import java.util.List;
import java.util.UUID;

public interface JobService {
    void createJob(RequestJobDTO requestJobDTO);
    default List<ResponseJobDTO> getAllJobs(String status) {
        return getAllJobs(status, null);
    }
    List<ResponseJobDTO> getAllJobs(String status, String keyword);
    List<ResponseWorkerDTO> findAllWorkerNearbyJob(UUID jobId, double radiusInKm);
    void inviteWorker(UUID jobId, UUID workerId);
    List<ResponseJobDTO> findNearbyJobs(double lat, double lng, double radiusInKm);
    void applyForJob(UUID jobId);
    void handleJobStart(UUID jobId);
    void handleJobEndTime(UUID jobId);
    void handleJobComplete(UUID jobId);
    void cancelJob(UUID jobId);
}
