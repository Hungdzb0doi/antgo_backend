package com.flashjobweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "worker_skills")
@IdClass(WorkerSkillIdEntity.class)
@Getter @Setter
public class WorkerSkillEntity {

    @Id
    @Column(name = "worker_id")
    private UUID workerId;

    @Id
    @Column(name = "category_id")
    private UUID categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", insertable = false, updatable = false)
    private WorkerProfileEntity worker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    private JobCategoryEntity category;
}
