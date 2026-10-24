package com.flashjobweb.dto.request;

import lombok.Data;

@Data
public class RequestProfileDTO {

    private String fullName;
    private String email;
    private String avatarUrl;
    private String skills;
    private String employerName;
    private String taxId;
}