package com.flashjobweb.service.impl;

import com.flashjobweb.dto.request.RequestMessageDTO;
import com.flashjobweb.dto.response.ResponseMessageDTO;
import com.flashjobweb.entity.ApplicationEntity;
import com.flashjobweb.entity.MessageEntity;
import com.flashjobweb.entity.UserEntity;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.ApplicationRepository;
import com.flashjobweb.repository.MessageRepository;
import com.flashjobweb.repository.UserRepository;
import com.flashjobweb.service.MessageService;
import com.flashjobweb.service.impl.outside.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final GoogleDriveService googleDriveService;

    @Override
    @Transactional
    public ResponseMessageDTO sendMessage(UUID senderId, RequestMessageDTO request) {
        ApplicationEntity app = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if sender is either the worker or the employer of this application
        UUID workerId = app.getWorker().getUserId();
        UUID employerId = app.getJob().getEmployer().getUserId();

        if (!senderId.equals(workerId) && !senderId.equals(employerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        MessageEntity message = new MessageEntity();
        message.setApplication(app);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setMessageType("TEXT");
        message.setSentAt(LocalDateTime.now());
        message.setIsRead(false);

        MessageEntity savedMsg = messageRepository.save(message);

        ResponseMessageDTO responseDTO = mapToDTO(savedMsg);

        // Broadcast message to WebSocket channel
        messagingTemplate.convertAndSend("/topic/chat/" + app.getId(), responseDTO);

        return responseDTO;
    }

    private record UploadedChatMedia(String messageType, String mediaUrl, boolean isVideo) {}

    private final java.util.Map<String, GoogleDriveService.DriveFileDownload> mediaCache = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public GoogleDriveService.DriveFileDownload getMedia(String fileId) {
        if (mediaCache.containsKey(fileId)) {
            return mediaCache.get(fileId);
        }
        try {
            GoogleDriveService.DriveFileDownload download = googleDriveService.downloadFile(fileId);
            if (mediaCache.size() > 200) {
                mediaCache.clear();
            }
            mediaCache.put(fileId, download);
            return download;
        } catch (Exception e) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public ResponseMessageDTO sendAttachmentMessage(UUID senderId, UUID applicationId, MultipartFile file, String content) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        List<ResponseMessageDTO> list = sendAttachmentMessages(senderId, applicationId, List.of(file), content);
        return list.get(0);
    }

    @Override
    @Transactional
    public List<ResponseMessageDTO> sendAttachmentMessages(UUID senderId, UUID applicationId, List<MultipartFile> files, String content) {
        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        ApplicationEntity app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if sender is either the worker or the employer of this application
        UUID workerId = app.getWorker().getUserId();
        UUID employerId = app.getJob().getEmployer().getUserId();

        if (!senderId.equals(workerId) && !senderId.equals(employerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        List<MultipartFile> validFiles = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toList());

        if (validFiles.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Upload in parallel using CompletableFuture
        List<java.util.concurrent.CompletableFuture<UploadedChatMedia>> uploadFutures = validFiles.stream()
                .map(file -> java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    String contentType = file.getContentType();
                    String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
                    boolean isVideo = (contentType != null && contentType.toLowerCase().startsWith("video"))
                            || originalFilename.matches(".*\\.(mp4|mov|avi|mkv|webm)$");
                    boolean isImage = (contentType != null && contentType.toLowerCase().startsWith("image"))
                            || originalFilename.matches(".*\\.(jpg|jpeg|png|gif|webp|bmp)$");

                    if (!isVideo && !isImage) {
                        throw new AppException(ErrorCode.INVALID_REQUEST);
                    }

                    try {
                        byte[] fileBytes = file.getBytes();
                        String fileId = googleDriveService.uploadChatFile(file);
                        String messageType = isVideo ? "VIDEO" : "IMAGE";
                        String mediaUrl = isVideo 
                                ? "https://drive.google.com/file/d/" + fileId + "/preview"
                                : "/api/v1/messages/media/" + fileId;
                        mediaCache.put(fileId, new GoogleDriveService.DriveFileDownload(fileBytes, contentType != null ? contentType : "image/jpeg"));
                        return new UploadedChatMedia(messageType, mediaUrl, isVideo);
                    } catch (Exception e) {
                        throw new RuntimeException("Lỗi tải tệp lên Google Drive: " + e.getMessage(), e);
                    }
                }))
                .collect(Collectors.toList());

        // Wait for all uploads to complete
        java.util.concurrent.CompletableFuture.allOf(uploadFutures.toArray(new java.util.concurrent.CompletableFuture[0])).join();

        List<UploadedChatMedia> uploadedMedias = uploadFutures.stream()
                .map(java.util.concurrent.CompletableFuture::join)
                .collect(Collectors.toList());

        List<MessageEntity> messagesToSave = new java.util.ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < uploadedMedias.size(); i++) {
            UploadedChatMedia media = uploadedMedias.get(i);
            String messageContent;
            if (i == 0 && content != null && !content.trim().isEmpty()) {
                messageContent = content.trim();
            } else {
                messageContent = media.isVideo() ? "[Video]" : "[Hình ảnh]";
            }

            MessageEntity message = new MessageEntity();
            message.setApplication(app);
            message.setSender(sender);
            message.setContent(messageContent);
            message.setMessageType(media.messageType());
            message.setMediaUrl(media.mediaUrl());
            message.setSentAt(now.plusNanos(i * 1000000L));
            message.setIsRead(false);
            messagesToSave.add(message);
        }

        List<MessageEntity> savedMessages = messageRepository.saveAll(messagesToSave);
        List<ResponseMessageDTO> result = new java.util.ArrayList<>();

        for (MessageEntity savedMsg : savedMessages) {
            ResponseMessageDTO dto = mapToDTO(savedMsg);
            result.add(dto);
            // Broadcast message to WebSocket channel
            messagingTemplate.convertAndSend("/topic/chat/" + app.getId(), dto);
        }

        return result;
    }

    @Override
    @Transactional
    public List<ResponseMessageDTO> getChatHistory(UUID applicationId, UUID userId) {
        ApplicationEntity app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.APPLICATION_NOT_FOUND));

        UUID workerId = app.getWorker().getUserId();
        UUID employerId = app.getJob().getEmployer().getUserId();

        if (!userId.equals(workerId) && !userId.equals(employerId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Mark unread messages as read
        List<MessageEntity> messages = messageRepository.findByApplicationIdOrderBySentAtAsc(applicationId);
        boolean changed = false;
        for (MessageEntity msg : messages) {
            if (!msg.getSender().getId().equals(userId) && !msg.getIsRead()) {
                msg.setIsRead(true);
                changed = true;
            }
        }
        if (changed) {
            messageRepository.saveAll(messages);
        }

        return messages.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(UUID applicationId, UUID userId) {
        List<MessageEntity> messages = messageRepository.findByApplicationIdOrderBySentAtAsc(applicationId);
        boolean changed = false;
        for (MessageEntity msg : messages) {
            if (!msg.getSender().getId().equals(userId) && !msg.getIsRead()) {
                msg.setIsRead(true);
                changed = true;
            }
        }
        if (changed) {
            messageRepository.saveAll(messages);
        }
    }

    @Override
    public long getUnreadCount(UUID applicationId, UUID userId) {
        return messageRepository.countByApplicationIdAndIsReadFalseAndSenderIdNot(applicationId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.flashjobweb.dto.response.ResponseConversationDTO> getConversations(UUID userId) {
        List<ApplicationEntity> apps = applicationRepository.findAllByWorkerOrEmployer(userId);
        List<com.flashjobweb.dto.response.ResponseConversationDTO> result = new java.util.ArrayList<>();

        for (ApplicationEntity app : apps) {
            List<MessageEntity> msgs = messageRepository.findByApplicationIdOrderBySentAtAsc(app.getId());
            
            String lastMsgContent = "Bắt đầu trò chuyện";
            java.time.LocalDateTime lastMsgTime = app.getCreatedAt() != null ? app.getCreatedAt() : java.time.LocalDateTime.now();
            long unread = 0;

            if (!msgs.isEmpty()) {
                MessageEntity lastMsg = msgs.get(msgs.size() - 1);
                if ("IMAGE".equalsIgnoreCase(lastMsg.getMessageType())) {
                    String text = (lastMsg.getContent() != null && !lastMsg.getContent().equals("[Hình ảnh]")) ? ": " + lastMsg.getContent() : "";
                    lastMsgContent = "[Hình ảnh]" + text;
                } else if ("VIDEO".equalsIgnoreCase(lastMsg.getMessageType())) {
                    String text = (lastMsg.getContent() != null && !lastMsg.getContent().equals("[Video]")) ? ": " + lastMsg.getContent() : "";
                    lastMsgContent = "[Video]" + text;
                } else {
                    lastMsgContent = lastMsg.getContent();
                }
                lastMsgTime = lastMsg.getSentAt() != null ? lastMsg.getSentAt() : lastMsgTime;
                unread = messageRepository.countByApplicationIdAndIsReadFalseAndSenderIdNot(app.getId(), userId);
            }

            boolean isEmployer = app.getJob() != null && app.getJob().getEmployer() != null && app.getJob().getEmployer().getUserId().equals(userId);
            UserEntity counterUser = isEmployer 
                    ? (app.getWorker() != null ? app.getWorker().getUserEntity() : null) 
                    : (app.getJob() != null && app.getJob().getEmployer() != null ? app.getJob().getEmployer().getUserEntity() : null);

            if (counterUser == null) continue;

            String counterpartyRole = isEmployer ? "WORKER" : "EMPLOYER";
            String myRole = isEmployer ? "EMPLOYER" : "WORKER";

            result.add(com.flashjobweb.dto.response.ResponseConversationDTO.builder()
                    .applicationId(app.getId())
                    .jobId(app.getJob().getId())
                    .jobTitle(app.getJob().getTitle())
                    .counterpartyId(counterUser.getId())
                    .counterpartyName(counterUser.getFullName())
                    .counterpartyAvatar(counterUser.getAvatarUrl())
                    .counterpartyRole(counterpartyRole)
                    .myRole(myRole)
                    .lastMessage(lastMsgContent)
                    .lastMessageTime(lastMsgTime)
                    .unreadCount(unread)
                    .build());
        }

        result.sort((a, b) -> {
            if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
            if (a.getLastMessageTime() == null) return 1;
            if (b.getLastMessageTime() == null) return -1;
            return b.getLastMessageTime().compareTo(a.getLastMessageTime());
        });
        return result;
    }

    private ResponseMessageDTO mapToDTO(MessageEntity msg) {
        String senderRole = "WORKER";
        try {
            if (msg.getApplication() != null 
                    && msg.getApplication().getJob() != null 
                    && msg.getApplication().getJob().getEmployer() != null 
                    && msg.getSender() != null 
                    && msg.getApplication().getJob().getEmployer().getUserId().equals(msg.getSender().getId())) {
                senderRole = "EMPLOYER";
            }
        } catch (Exception ignored) {}

        return ResponseMessageDTO.builder()
                .id(msg.getId())
                .applicationId(msg.getApplication() != null ? msg.getApplication().getId() : null)
                .senderId(msg.getSender() != null ? msg.getSender().getId() : null)
                .senderName(msg.getSender() != null ? msg.getSender().getFullName() : null)
                .senderAvatar(msg.getSender() != null ? msg.getSender().getAvatarUrl() : null)
                .content(msg.getContent())
                .messageType(msg.getMessageType() != null ? msg.getMessageType() : "TEXT")
                .mediaUrl(msg.getMediaUrl())
                .sentAt(msg.getSentAt())
                .isRead(msg.getIsRead())
                .senderRole(senderRole)
                .build();
    }
}
