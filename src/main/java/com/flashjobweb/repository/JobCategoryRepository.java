package com.flashjobweb.repository;

import com.flashjobweb.entity.JobCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface JobCategoryRepository extends JpaRepository<JobCategoryEntity, UUID> {
    Optional<JobCategoryEntity> findByName(String name);
}
