package com.flashjobweb.repository;

import com.flashjobweb.entity.DeviceTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceTokenEntity, UUID> {
    List<DeviceTokenEntity> findByUserId(UUID userId);
    void deleteByUserIdAndToken(UUID userId, String token);
}
