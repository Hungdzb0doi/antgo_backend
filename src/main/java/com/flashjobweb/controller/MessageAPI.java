package com.flashjobweb.controller;

import com.flashjobweb.dto.request.RequestMessageDTO;
import com.flashjobweb.dto.response.ResponseConversationDTO;
import com.flashjobweb.dto.response.ResponseMessageDTO;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.UserRepository;
import com.flashjobweb.service.MessageService;
import com.flashjobweb.service.impl.security.CustomUserDetails;
import com.flashjobweb.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageAPI {

    private final MessageService messageService;
    private final UserRepository userRepository;

    private UUID getCurrentUserId(CustomUserDetails userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            return userDetails.getUser().getId();
        }
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND))
                .getId();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResponseMessageDTO>>> getMessages(
            @RequestParam UUID applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        List<ResponseMessageDTO> history = messageService.getChatHistory(applicationId, userId);
        return ResponseEntity.ok(ApiResponse.<List<ResponseMessageDTO>>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy tin nhắn thành công")
                .data(history)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ResponseMessageDTO>> sendMessage(
            @Valid @RequestBody RequestMessageDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        ResponseMessageDTO sentMessage = messageService.sendMessage(userId, request);
        return ResponseEntity.ok(ApiResponse.<ResponseMessageDTO>builder()
                .code(HttpStatus.OK.value())
                .message("Gửi tin nhắn thành công")
                .data(sentMessage)
                .build());
    }

    @PostMapping(value = "/attachment", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<ResponseMessageDTO>>> sendAttachment(
            @RequestParam("applicationId") UUID applicationId,
            @RequestParam(value = "files", required = false) List<org.springframework.web.multipart.MultipartFile> files,
            @RequestParam(value = "file", required = false) org.springframework.web.multipart.MultipartFile file,
            @RequestParam(value = "content", required = false) String content,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        List<org.springframework.web.multipart.MultipartFile> fileList = new java.util.ArrayList<>();
        if (files != null && !files.isEmpty()) {
            fileList.addAll(files);
        } else if (file != null && !file.isEmpty()) {
            fileList.add(file);
        }
        List<ResponseMessageDTO> sentMessages = messageService.sendAttachmentMessages(userId, applicationId, fileList, content);
        return ResponseEntity.ok(ApiResponse.<List<ResponseMessageDTO>>builder()
                .code(HttpStatus.OK.value())
                .message("Gửi tệp đính kèm thành công")
                .data(sentMessages)
                .build());
    }

    @GetMapping("/media/{fileId}")
    public ResponseEntity<byte[]> getMedia(@PathVariable String fileId) {
        try {
            com.flashjobweb.service.impl.outside.GoogleDriveService.DriveFileDownload media = messageService.getMedia(fileId);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_TYPE, media.mimeType())
                    .header(org.springframework.http.HttpHeaders.CACHE_CONTROL, "public, max-age=604800")
                    .body(media.content());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<Void>> updateMessageStatus(
            @RequestParam UUID applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        messageService.markAsRead(applicationId, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Đã đánh dấu đã đọc")
                .build());
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<ResponseConversationDTO>>> getConversations(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        List<ResponseConversationDTO> conversations = messageService.getConversations(userId);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @RequestParam UUID applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        long count = messageService.getUnreadCount(applicationId, userId);
        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .code(HttpStatus.OK.value())
                .message("Lấy số tin nhắn chưa đọc")
                .data(count)
                .build());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UUID>> getMyUserId(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID userId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(userId));
    }
}
