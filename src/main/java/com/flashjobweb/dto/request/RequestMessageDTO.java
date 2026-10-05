package com.flashjobweb.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class RequestMessageDTO {
    @NotNull(message = "Application ID is required")
    private UUID applicationId;

    @NotBlank(message = "Message content is required")
    private String content;
}
