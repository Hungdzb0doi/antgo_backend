package com.flashjobweb.dto.request;

import jakarta.persistence.Column;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
@Data
public class RequestEkycDTO {
    private MultipartFile front;
    private MultipartFile back;
    private MultipartFile face;
}
