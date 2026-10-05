package com.flashjobweb.dto.request;

import lombok.Data;

import java.util.UUID;
@Data
public class RequestScanQrDTO {
    private UUID jobId;
    private String action;
    private Long timestamp;
}
