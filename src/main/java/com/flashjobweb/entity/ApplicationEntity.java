package com.flashjobweb.entity;

import com.flashjobweb.util.ApplicationStatus;
import com.flashjobweb.util.ApplicationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications",
        uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "worker_id"}))
@Getter @Setter
public class ApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private JobEntity job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private WorkerProfileEntity worker;

    @Column(name = "check_in_at")
    private LocalDateTime checkInAt;
    @Column(name = "check_out_at")
    private LocalDateTime checkOutAt;
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "employer_confirmed", nullable = false)
    private Boolean employerConfirmed = false;

    @Column(name = "worker_confirmed", nullable = false)
    private Boolean workerConfirmed = false;

    @Column(name = "is_disputed", nullable = false)
    private Boolean isDisputed = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ApplicationType type ;
    @Column(name = "earned_amount")
    private BigDecimal earnedAmount ;
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageEntity> messageEntities;

    @OneToMany(mappedBy = "application")
    private List<ReviewEntity> reviewEntities;

    @OneToMany(mappedBy = "application")
    private List<ReportEntity> reportEntities;
}
