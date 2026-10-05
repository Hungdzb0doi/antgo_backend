package com.flashjobweb.controller;

import com.flashjobweb.dto.request.RequestLoginDTO;
import com.flashjobweb.dto.request.RequestRegisterDTO;
import com.flashjobweb.dto.request.RequestResetPasswordDTO;
import com.flashjobweb.dto.response.ResponseLoginDTO;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.flashjobweb.util.ApiResponse;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthAPI {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Object>> register(@RequestBody RequestRegisterDTO requestRegisterDTO) {
        authService.register(requestRegisterDTO);
        return ResponseEntity.ok(ApiResponse.created());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Object>> login(@RequestBody RequestLoginDTO requestLoginDTO) {
        ResponseLoginDTO responseLoginDTO = authService.login(requestLoginDTO);
        if (responseLoginDTO != null) {
            return ResponseEntity.ok(ApiResponse.success(responseLoginDTO));
        } else {
            return ResponseEntity.status(ErrorCode.NOT_GENERATE_TOKEN.getHttpStatus()).body(ApiResponse.error(ErrorCode.NOT_GENERATE_TOKEN.getMessage()));
        }
    }

    @PostMapping("/otp")
    public ResponseEntity<ApiResponse<Object>> forgotPassword(@RequestParam String email) {
        authService.generateAndSendOtp(email);
        return ResponseEntity.ok(ApiResponse.created());
    }
    @PostMapping("/ckotp")
    public ResponseEntity<ApiResponse<Object>> checkOtp(@RequestBody RequestResetPasswordDTO requestResetPasswordDTO) {
        authService.checkOtp(requestResetPasswordDTO);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/newpassword")
    public ResponseEntity<ApiResponse<Object>> resetPassword(@RequestBody RequestResetPasswordDTO requestResetPasswordDTO) {
        authService.resetPassword(requestResetPasswordDTO);
        return ResponseEntity.ok(ApiResponse.created());
    }
}
