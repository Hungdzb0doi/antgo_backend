package com.flashjobweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "employer_profiles")
@Getter @Setter
public class EmployerProfileEntity {

    @Id
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    @Column(name = "employer_name", length = 150)
    private String employerName;

    @Column(name = "tax_id", length = 20)
    private String taxId;

    @Column(name = "employer_score", nullable = false)
    private Integer employerScore = 100;

    @OneToMany(mappedBy = "employer")
    private List<JobEntity> jobEntities;
}
