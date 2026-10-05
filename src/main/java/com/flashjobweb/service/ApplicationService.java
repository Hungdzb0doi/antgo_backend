package com.flashjobweb.service;

import com.flashjobweb.dto.request.RequestApplicationDTO;
import com.flashjobweb.dto.request.RequestInvitationDTO;
import com.flashjobweb.dto.request.RequestScanQrDTO;
import com.flashjobweb.dto.response.ResponseApplicationEmployerDTO;
import com.flashjobweb.dto.response.ResponseApplicationWorkerDTO;
import com.flashjobweb.dto.response.ResponseScanQrDTO;
import com.flashjobweb.util.ApplicationStatus;
import com.flashjobweb.util.ApplicationType;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {
    default List<ResponseApplicationWorkerDTO> getApplicationsForCurrentUser(
            ApplicationStatus status,
            ApplicationType type) {
        return getApplicationsForCurrentUser(status, type, null);
    }
    List<ResponseApplicationWorkerDTO> getApplicationsForCurrentUser(
            ApplicationStatus status,
            ApplicationType type,
            String keyword);
    void respondToInvitation(RequestInvitationDTO request);
    List<ResponseApplicationEmployerDTO> getApplicationsForJob(UUID jobId);
    void respondToApplication(RequestApplicationDTO request);
    ResponseScanQrDTO scanQr(RequestScanQrDTO request);
    String generateQrData(UUID jobId, String action);
    void confirmPayment(UUID applicationId);
    void confirmSatisfaction(UUID applicationId);
}
