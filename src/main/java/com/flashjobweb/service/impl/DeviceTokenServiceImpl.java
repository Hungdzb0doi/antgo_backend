package com.flashjobweb.service.impl;

import com.flashjobweb.dto.request.RequestDeviceTokenDTO;
import com.flashjobweb.entity.DeviceTokenEntity;
import com.flashjobweb.entity.UserEntity;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.DeviceTokenRepository;
import com.flashjobweb.repository.UserRepository;
import com.flashjobweb.service.DeviceTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceTokenServiceImpl implements DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Override
    public void registerToken(RequestDeviceTokenDTO request) {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        String token = request.getToken().trim();
        String platform = (request.getPlatform() != null && !request.getPlatform().isBlank())
                ? request.getPlatform().trim().toUpperCase()
                : "WEB";
        if (platform.length() > 10) {
            platform = platform.substring(0, 10);
        }

        Optional<DeviceTokenEntity> existingOpt = deviceTokenRepository.findByToken(token);
        if (existingOpt.isPresent()) {
            DeviceTokenEntity existing = existingOpt.get();
            existing.setUser(user);
            existing.setPlatform(platform);
            existing.setCreatedAt(LocalDateTime.now());
            deviceTokenRepository.save(existing);
            log.info("Cập nhật FCM device token cho người dùng {}: {}", user.getId(), token);
        } else {
            DeviceTokenEntity entity = new DeviceTokenEntity();
            entity.setUser(user);
            entity.setToken(token);
            entity.setPlatform(platform);
            entity.setCreatedAt(LocalDateTime.now());
            deviceTokenRepository.save(entity);
            log.info("Đăng ký FCM device token mới cho người dùng {}: {}", user.getId(), token);
        }
    }

    @Override
    public void removeToken(String token) {
        if (token == null || token.isBlank()) return;
        try {
            deviceTokenRepository.deleteByToken(token.trim());
            log.info("Đã xóa FCM device token: {}", token);
        } catch (Exception e) {
            log.warn("Lỗi khi xóa device token: {}", e.getMessage());
        }
    }
}
