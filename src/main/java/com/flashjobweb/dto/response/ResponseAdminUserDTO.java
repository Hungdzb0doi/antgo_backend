package com.flashjobweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseAdminUserDTO {
    private UUID id;
    private String fullName;
    private String phone;
    private String email;
    private String currentMode;
    private String role;
    private String status;
    private String identityCard;
    private Boolean identityVerified;
    private Integer workerScore;
    private Integer employerScore;
    private LocalDateTime createdAt;
}
