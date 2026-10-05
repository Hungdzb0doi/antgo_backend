package com.flashjobweb.dto.request;
import lombok.Data;
@Data
public class RequestRegisterDTO {
    private String phone;
    private String passwordHash;
    private String fullName;
    private String email;
}
