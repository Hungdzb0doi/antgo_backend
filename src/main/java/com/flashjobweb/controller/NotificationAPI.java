package com.flashjobweb.controller;

import com.flashjobweb.dto.response.NotificationWrapperDTO;
import com.flashjobweb.service.NotificationService;
import com.flashjobweb.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class NotificationAPI {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationWrapperDTO>> getMyNotifications() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getMyNotifications()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái thông báo"));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Object>> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("Đã cập nhật tất cả thông báo"));
    }
}
