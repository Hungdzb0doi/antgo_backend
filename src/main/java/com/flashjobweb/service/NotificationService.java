package com.flashjobweb.service;

import com.flashjobweb.dto.response.NotificationWrapperDTO;
import com.flashjobweb.entity.UserEntity;

import java.util.UUID;

public interface NotificationService {
    void sendNotification(UserEntity user, String title, String content, String type);
    void sendNotification(UserEntity user, String title, String content, String type, UUID jobId);
    NotificationWrapperDTO getMyNotifications();
    void markAsRead(UUID notificationId);
    void markAllAsRead();
}
