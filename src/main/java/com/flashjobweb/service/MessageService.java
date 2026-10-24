package com.flashjobweb.service;

import com.flashjobweb.dto.request.RequestMessageDTO;
import com.flashjobweb.dto.response.ResponseConversationDTO;
import com.flashjobweb.dto.response.ResponseMessageDTO;

import java.util.List;
import java.util.UUID;

public interface MessageService {
    ResponseMessageDTO sendMessage(UUID senderId, RequestMessageDTO request);
    ResponseMessageDTO sendAttachmentMessage(UUID senderId, UUID applicationId, org.springframework.web.multipart.MultipartFile file, String content);
    List<ResponseMessageDTO> sendAttachmentMessages(UUID senderId, UUID applicationId, List<org.springframework.web.multipart.MultipartFile> files, String content);
    List<ResponseMessageDTO> getChatHistory(UUID applicationId, UUID userId);
    void markAsRead(UUID applicationId, UUID userId);
    long getUnreadCount(UUID applicationId, UUID userId);
    List<ResponseConversationDTO> getConversations(UUID userId);
    com.flashjobweb.service.impl.outside.GoogleDriveService.DriveFileDownload getMedia(String fileId);
}
