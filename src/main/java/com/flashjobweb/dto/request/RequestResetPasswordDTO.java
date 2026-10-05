package com.flashjobweb.dto.request;

import lombok.Data;

@Data
public class RequestResetPasswordDTO {
    private String email;
    private String newPassword;
    private String otp;

}
