package com.flashjobweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseAdminEkycDTO {
    private UUID id;
    private UUID userId;
    private String userName;
    private String userPhone;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String faceImageUrl;
    private String status;
    private LocalDateTime createdAt;
}
