package com.flashjobweb.dto.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter@Setter
public class ResponseJobDTO {
    private UUID id;
    private String title;
    private Double latitude;
    private Double longitude;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer requiredWorkers;
    private BigDecimal hourlyRate;
    private String status;
    private String employerName;
    private String employerAvatar;
    private Integer employerScore;
    private Double employerRating;
    private Long employerReviewCount;
    private Double priorityScore;
    private String employerPhone;
}
