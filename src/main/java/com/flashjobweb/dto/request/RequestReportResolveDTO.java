package com.flashjobweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestReportResolveDTO {
    @NotBlank(message = "Decision is required (RESOLVED or REJECTED)")
    private String decision;
    private Integer penaltyPoints;
    private String note;
}
