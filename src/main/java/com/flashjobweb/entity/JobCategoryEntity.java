package com.flashjobweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_categories")
@Getter @Setter
public class JobCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "category")
    private List<JobEntity> jobEntities;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkerSkillEntity> workerSkillEntities;
}
