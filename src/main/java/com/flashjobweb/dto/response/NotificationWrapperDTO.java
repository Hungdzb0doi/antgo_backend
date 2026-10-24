package com.flashjobweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationWrapperDTO {
    private UUID userId;
    private long unreadCount;
    private List<ResponseNotificationDTO> notifications;
}
