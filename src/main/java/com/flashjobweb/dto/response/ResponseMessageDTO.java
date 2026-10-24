package com.flashjobweb.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ResponseMessageDTO {
    private UUID id;
    private UUID applicationId;
    private UUID senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private String messageType;
    private String mediaUrl;
    private LocalDateTime sentAt;
    private Boolean isRead;
    private String senderRole;
}
