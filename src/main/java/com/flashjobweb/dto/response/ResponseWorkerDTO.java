package com.flashjobweb.dto.response;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.util.UUID;
@Getter @Setter
public class ResponseWorkerDTO {
    private UUID userId;
    private String fullName;
    private String avatar;
    private String skills;
    private Integer workerScore;
    private Point currentLocation;
    private String phone;
    private Boolean identityVerified;
    private String email;
    private Double distance;
    private Double longitude;
    private Double latitude;
    private Double averageRating;
    private Long reviewCount;
    private Double priorityScore;
    private Boolean hasApplied;
}
