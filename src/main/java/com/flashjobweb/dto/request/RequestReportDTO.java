package com.flashjobweb.dto.request;

import lombok.Getter;
import lombok.Setter;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class RequestReportDTO {
    private UUID applicationId;
    private String reason;
    private List<MultipartFile> files;
}

