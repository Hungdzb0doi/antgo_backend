package com.flashjobweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseReviewDTO {
    private UUID id;
    private UUID applicationId;
    private UUID reviewerId;
    private String reviewerName;
    private String reviewerAvatarUrl;
    private UUID revieweeId;
    private String revieweeName;
    private String targetRole; // "WORKER" hoặc "EMPLOYER"
    private String jobTitle;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
