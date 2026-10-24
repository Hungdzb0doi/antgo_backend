package com.flashjobweb.controller;

import com.flashjobweb.dto.request.RequestDeviceTokenDTO;
import com.flashjobweb.service.DeviceTokenService;
import com.flashjobweb.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceTokenService deviceTokenService;

    @PostMapping("/token")
    public ResponseEntity<ApiResponse<Object>> registerToken(@Valid @RequestBody RequestDeviceTokenDTO request) {
        deviceTokenService.registerToken(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký token thiết bị thành công"));
    }

    @DeleteMapping("/token")
    public ResponseEntity<ApiResponse<Object>> removeToken(@RequestParam String token) {
        deviceTokenService.removeToken(token);
        return ResponseEntity.ok(ApiResponse.success("Xóa token thiết bị thành công"));
    }
}
