package com.flashjobweb.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;
@Data
public class RequestApplicationDTO {
    private UUID applicationId;
    @JsonProperty("isAccepted")
    private boolean isAccepted;
}
