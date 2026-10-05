package com.flashjobweb.repository;

import com.flashjobweb.entity.WorkerSkillEntity;
import com.flashjobweb.entity.WorkerSkillIdEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface WorkerSkillRepository extends JpaRepository<WorkerSkillEntity, WorkerSkillIdEntity> {
    List<WorkerSkillEntity> findByWorkerId(UUID workerId);
    List<WorkerSkillEntity> findByCategoryId(UUID categoryId);
}
