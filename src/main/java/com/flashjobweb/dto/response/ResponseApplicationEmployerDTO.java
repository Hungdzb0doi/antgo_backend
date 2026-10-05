package com.flashjobweb.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;
@Getter@Setter
public class ResponseApplicationEmployerDTO {
        private UUID applicationId;
        private UUID workerId;
        private String workerName;
        private String workerAvatar;
        private Integer workerScore;
        private String phone;
        private String status;
        private String type;
        private boolean identityVerified;
        private LocalDateTime createdAt;
        private String jobTitle;
        private String jobStatus;
        private Boolean isReviewed;
        private Integer reviewRating;
        private String reviewComment;
        private Boolean isDisputed;
        private Boolean employerConfirmed;
        private Boolean workerConfirmed;
        private java.math.BigDecimal earnedAmount;
        private LocalDateTime checkOutAt;
}
