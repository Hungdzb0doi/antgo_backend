package com.flashjobweb.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RequestJobDTO {
    private UUID categoryId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer requiredWorkers;
    private BigDecimal hourlyRate;

    private Double longitude;
    private Double latitude;
}