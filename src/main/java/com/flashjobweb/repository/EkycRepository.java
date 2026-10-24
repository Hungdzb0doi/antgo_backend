package com.flashjobweb.repository;

import com.flashjobweb.entity.EkycEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EkycRepository extends JpaRepository<EkycEntity, UUID> {
    List<EkycEntity> findByStatusOrderByCreatedAtDesc(String status);
    List<EkycEntity> findAllByOrderByCreatedAtDesc();
    long countByStatus(String status);
}
