package com.flashjobweb.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseAdminDashboardDTO {
    private long totalUsers;
    private long totalWorkers;
    private long totalEmployers;
    private long verifiedUsers;
    private long totalJobs;
    private long activeJobs;
    private long pendingEkycCount;
    private long pendingReportsCount;
}
