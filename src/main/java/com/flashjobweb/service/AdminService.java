package com.flashjobweb.service;

import com.flashjobweb.dto.request.RequestCategoryDTO;
import com.flashjobweb.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface AdminService {
    ResponseAdminDashboardDTO getDashboardStats();

    List<ResponseAdminUserDTO> getAllUsers(String search);

    void toggleUserStatus(UUID userId);

    List<ResponseAdminEkycDTO> getEkycRecords(String status);

    void processEkyc(UUID ekycId, String decision, String note);

    List<ResponseAdminReportDTO> getReports(String status);

    void resolveReport(UUID reportId, String decision, Integer penaltyPoints, String note);

    List<ResponseAdminCategoryDTO> getAllCategories();

    ResponseJobCategoryDTO createCategory(RequestCategoryDTO dto);

    ResponseJobCategoryDTO updateCategory(UUID id, RequestCategoryDTO dto);

    void deleteCategory(UUID id);
}
