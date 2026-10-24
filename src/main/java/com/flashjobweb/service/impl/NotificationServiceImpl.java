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
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.flashjobweb.entity.DeviceTokenEntity;
import com.flashjobweb.repository.DeviceTokenRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public void sendNotification(UserEntity user, String title, String content, String type) {
        sendNotification(user, title, content, type, null);
    }

    @Override
    public void sendNotification(UserEntity user, String title, String content, String type, UUID jobId) {
        if (user == null) {
            log.warn("Bỏ qua gửi thông báo: userEntity là null");
            return;
        }

        NotificationEntity notif = new NotificationEntity();
        notif.setUser(user);
        notif.setTitle(title != null ? title : "Thông báo");
        notif.setContent(content);
        notif.setType(type != null ? type : "SYSTEM");
        notif.setJobId(jobId);
        notif.setIsRead(false);
        notif.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notif);

        ResponseNotificationDTO notifSocket = ResponseNotificationDTO.builder()
                .id(notif.getId())
                .userId(user.getId())
                .title(notif.getTitle())
                .content(content)
                .type(notif.getType())
                .jobId(jobId)
                .isRead(false)
                .createdAt(notif.getCreatedAt())
                .build();

        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + user.getId().toString(), notifSocket);
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo real-time qua WebSocket cho user {}: {}", user.getId(), e.getMessage());
        }

        // Gửi Firebase Web Push Notification (FCM)
        sendFirebasePushNotification(user, notif.getTitle(), content, notif.getType(), jobId, notif.getId());
    }

    private void sendFirebasePushNotification(UserEntity user, String title, String content, String type, UUID jobId, UUID notifId) {
        if (FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try {
            List<DeviceTokenEntity> deviceTokens = deviceTokenRepository.findByUserId(user.getId());
            if (deviceTokens == null || deviceTokens.isEmpty()) {
                return;
            }

            List<String> tokens = deviceTokens.stream()
                    .map(DeviceTokenEntity::getToken)
                    .filter(t -> t != null && !t.isBlank())
                    .distinct()
                    .collect(Collectors.toList());

            if (tokens.isEmpty()) {
                return;
            }

            String targetUrl = "/interface/detailnotification?title="
                    + URLEncoder.encode(title != null ? title : "", StandardCharsets.UTF_8)
                    + "&id=" + (notifId != null ? notifId.toString() : "")
                    + (jobId != null ? "&jobId=" + jobId.toString() : "");

            Notification fcmNotification = Notification.builder()
                    .setTitle(title != null ? title : "Thông báo AntGo")
                    .setBody(content != null ? content : "")
                    .build();

            WebpushConfig webpushConfig = WebpushConfig.builder()
                    .setNotification(WebpushNotification.builder()
                            .setTitle(title != null ? title : "Thông báo AntGo")
                            .setBody(content != null ? content : "")
                            .setIcon("/asset/antgo-icon.svg")
                            .setBadge("/asset/antgo-icon.svg")
                            .build())
                    .setFcmOptions(WebpushFcmOptions.builder()
                            .setLink(targetUrl)
                            .build())
                    .build();

            MulticastMessage.Builder multicastBuilder = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(fcmNotification)
                    .setWebpushConfig(webpushConfig)
                    .putData("title", title != null ? title : "Thông báo AntGo")
                    .putData("content", content != null ? content : "")
                    .putData("type", type != null ? type : "SYSTEM")
                    .putData("url", targetUrl)
                    .putData("click_action", targetUrl);

            if (jobId != null) {
                multicastBuilder.putData("jobId", jobId.toString());
            }
            if (notifId != null) {
                multicastBuilder.putData("id", notifId.toString());
            }

            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(multicastBuilder.build());
            log.info("FCM push notification gửi thành công: {} token, thất bại: {} token cho người dùng {}",
                    response.getSuccessCount(), response.getFailureCount(), user.getId());

            // Dọn dẹp token không còn hợp lệ (nếu token đã bị hủy đăng ký hoặc lỗi)
            if (response.getFailureCount() > 0) {
                List<SendResponse> responses = response.getResponses();
                List<String> deadTokens = new ArrayList<>();
                for (int i = 0; i < responses.size(); i++) {
                    SendResponse sr = responses.get(i);
                    if (!sr.isSuccessful()) {
                        FirebaseMessagingException ex = sr.getException();
                        if (ex != null && (ex.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                                || ex.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT)) {
                            deadTokens.add(tokens.get(i));
                        }
                    }
                }
                if (!deadTokens.isEmpty()) {
                    deadTokens.forEach(deviceTokenRepository::deleteByToken);
                    log.info("Đã dọn dẹp {} FCM token không còn sử dụng", deadTokens.size());
                }
            }
        } catch (Exception e) {
            log.warn("Không thể gửi thông báo FCM push cho user {}: {}", user.getId(), e.getMessage());
        }
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
