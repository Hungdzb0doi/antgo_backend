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
public class ResponseNotificationDTO {
    private UUID id;
    private UUID userId;
    private String title;
    private String content;
    private String type;
    private UUID jobId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
