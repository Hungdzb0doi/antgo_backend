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
public class ResponseAdminReportDTO {
    private UUID id;
    private UUID reporterId;
    private String reporterName;
    private String reporterPhone;
    private UUID reportedId;
    private String reportedName;
    private String reportedPhone;
    private UUID applicationId;
    private String jobTitle;
    private String reason;
    private String evidenceUrls;
    private String status;
    private LocalDateTime createdAt;

    // Chi tiết thời gian ca làm & tiền công cần thanh toán
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private java.math.BigDecimal hourlyRate;
    private java.math.BigDecimal paymentAmount;
}
