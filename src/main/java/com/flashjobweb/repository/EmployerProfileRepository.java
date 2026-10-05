package com.flashjobweb.repository;

import com.flashjobweb.entity.EmployerProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EmployerProfileRepository extends JpaRepository<EmployerProfileEntity, UUID> {
}
