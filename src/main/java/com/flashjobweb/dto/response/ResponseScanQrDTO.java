package com.flashjobweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseScanQrDTO {
    private UUID applicationId;
    private UUID jobId;
    private String workerName;
    private String action; // CHECKIN hoặc CHECKOUT
    private String time;
    private Double hoursWorked;
    private BigDecimal hourlyRate;
    private BigDecimal totalEarned;
    private Boolean workerConfirmed;
    private Boolean employerConfirmed;
}
