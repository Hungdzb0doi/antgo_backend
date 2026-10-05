package com.flashjobweb.dto.response;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class ResponseProfileDTO {

    private String fullName;
    private String phone;
    private String email;
    private String avatarUrl;
    private boolean identityVerified;
    private String currentMode;

    private Integer workerScore;
    private String skills;
    private boolean isAvailable;


    private Integer employerScore;
    private String employerName;
    private String taxId;
}
