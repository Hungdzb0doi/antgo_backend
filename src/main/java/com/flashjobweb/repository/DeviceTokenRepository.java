package com.flashjobweb.repository;

import com.flashjobweb.entity.DeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, UUID> {
    List<DeviceTokenEntity> findByUserId(UUID userId);
    Optional<DeviceTokenEntity> findByToken(String token);
    void deleteByUserIdAndToken(UUID userId, String token);
    void deleteByToken(String token);
}
