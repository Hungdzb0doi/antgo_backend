package com.flashjobweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "worker_profiles")
@Getter @Setter
public class WorkerProfileEntity {

    @Id
    private UUID userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity userEntity;

    @Column(length = 255)
    private String skills;

    @Column(name = "worker_score", nullable = false)
    private Integer workerScore = 100;

    @Column(name = "current_location", columnDefinition = "GEOMETRY(Point, 4326)")
    private Point currentLocation;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = false;

    @OneToMany(mappedBy = "worker")
    private List<ApplicationEntity> applicationEntities;

    @OneToMany(mappedBy = "worker", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkerSkillEntity> workerSkillEntities;
}
