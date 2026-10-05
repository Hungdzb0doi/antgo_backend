package com.flashjobweb.service.impl;

import com.flashjobweb.dto.response.NotificationWrapperDTO;
import com.flashjobweb.dto.response.ResponseNotificationDTO;
import com.flashjobweb.entity.NotificationEntity;
import com.flashjobweb.entity.UserEntity;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.NotificationRepository;
import com.flashjobweb.repository.UserRepository;
import com.flashjobweb.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void sendNotification(UserEntity user, String title, String content, String type) {
        sendNotification(user, title, content, type, null);
    }

    @Override
    public void sendNotification(UserEntity user, String title, String content, String type, UUID jobId) {
        NotificationEntity notif = new NotificationEntity();
        notif.setUser(user);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setType(type);
        notif.setJobId(jobId);
        notif.setIsRead(false);
        notif.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notif);

        ResponseNotificationDTO notifSocket = ResponseNotificationDTO.builder()
                .id(notif.getId())
                .userId(user.getId())
                .title(title)
                .content(content)
                .type(type)
                .jobId(jobId)
                .isRead(false)
                .createdAt(notif.getCreatedAt())
                .build();

        messagingTemplate.convertAndSend("/topic/notifications/" + user.getId().toString(), notifSocket);
    }

    @Override
    public NotificationWrapperDTO getMyNotifications() {
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<NotificationEntity> entities = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        long unreadCount = notificationRepository.countByUserIdAndIsReadFalse(user.getId());

        List<ResponseNotificationDTO> dtoList = entities.stream().map(e -> ResponseNotificationDTO.builder()
                .id(e.getId())
                .userId(user.getId())
                .title(e.getTitle())
                .content(e.getContent())
                .type(e.getType())
                .jobId(e.getJobId())
                .isRead(e.getIsRead())
                .createdAt(e.getCreatedAt())
                .build()
        ).collect(Collectors.toList());

        return NotificationWrapperDTO.builder()
                .userId(user.getId())
                .unreadCount(unreadCount)
                .notifications(dtoList)
                .build();
    }

    @Override
    public void markAsRead(UUID notificationId) {
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        NotificationEntity notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notif.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.NOTIFICATION_NOT_BELONG_TO_USER);
        }

        notif.setIsRead(true);
        notificationRepository.save(notif);
    }

    @Override
    public void markAllAsRead() {
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        List<NotificationEntity> unreadList = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        unreadList.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unreadList);
    }
}
