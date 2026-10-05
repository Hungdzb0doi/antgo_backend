package com.flashjobweb.repository;

import com.flashjobweb.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByPhone(String phone);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
}
