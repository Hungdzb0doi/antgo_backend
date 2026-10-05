package com.flashjobweb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "current_mode", nullable = false, length = 10)
    private String currentMode = "WORKER";

    @Column(nullable = false, length = 10)
    private String status = "ACTIVE";

    @Column(name = "identity_card", length = 20)
    private String identityCard;

    @Column(name = "identity_verified", nullable = false)
    private Boolean identityVerified = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(unique = true, length = 255)
    private String email;
    @Column(name = "ResetPasswordOtp")
    private String resetPasswordOtp;
    @Column(name = "OtpTime")
    private LocalDateTime otpTime;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<NotificationEntity> notificationEntities;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeviceTokenEntity> deviceTokenEntities;

    @OneToMany(mappedBy = "reporter")
    private List<ReportEntity> reportsMade;

    @OneToMany(mappedBy = "reported")
    private List<ReportEntity> reportsReceived;

    @OneToMany(mappedBy = "reviewer")
    private List<ReviewEntity> reviewsGiven;

    @OneToMany(mappedBy = "reviewee")
    private List<ReviewEntity> reviewsReceived;

    @OneToMany(mappedBy = "sender")
    private List<MessageEntity> messagesSent;
}
