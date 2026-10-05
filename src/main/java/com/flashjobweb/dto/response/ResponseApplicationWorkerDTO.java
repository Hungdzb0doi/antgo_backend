package com.flashjobweb.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ResponseApplicationWorkerDTO {
    private UUID applicationId;
    private UUID jobId;
    private String jobTitle;
    private String employerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal hourlyRate;
    private String status;
    private String type;
    private BigDecimal earnedAmount;
    private Boolean workerConfirmed;
    private Boolean employerConfirmed;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private Boolean isReviewed;
    private Integer reviewRating;
    private String reviewComment;
    private Boolean isDisputed;
    private Double latitude;
    private Double longitude;
    private String employerPhone;
}
