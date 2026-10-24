package com.flashjobweb.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ResponseConversationDTO {
    private UUID applicationId;
    private UUID jobId;
    private String jobTitle;
    private UUID counterpartyId;
    private String counterpartyName;
    private String counterpartyAvatar;
    private String counterpartyRole;
    private String myRole;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
}
