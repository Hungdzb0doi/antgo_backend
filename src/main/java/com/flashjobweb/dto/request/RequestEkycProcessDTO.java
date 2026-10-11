package com.flashjobweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RequestEkycProcessDTO {
    @NotBlank(message = "Decision is required (APPROVED or REJECTED)")
    private String decision;
    private String note;
}
