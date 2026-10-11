package com.flashjobweb.controller;

import com.flashjobweb.dto.request.RequestCategoryDTO;
import com.flashjobweb.dto.request.RequestEkycProcessDTO;
import com.flashjobweb.dto.request.RequestReportResolveDTO;
import com.flashjobweb.dto.response.*;
import com.flashjobweb.service.AdminService;
import com.flashjobweb.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAPI {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<ResponseAdminDashboardDTO>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<ResponseAdminUserDTO>>> getUsers(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers(search)));
    }

    @PutMapping("/users/{userId}/toggle-status")
    public ResponseEntity<ApiResponse<String>> toggleUserStatus(@PathVariable UUID userId) {
        adminService.toggleUserStatus(userId);
        return ResponseEntity.ok(ApiResponse.success("Trạng thái tài khoản đã được cập nhật thành công"));
    }

    @GetMapping("/ekyc")
    public ResponseEntity<ApiResponse<List<ResponseAdminEkycDTO>>> getEkycRecords(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getEkycRecords(status)));
    }

    @PostMapping("/ekyc/{ekycId}/process")
    public ResponseEntity<ApiResponse<String>> processEkyc(
            @PathVariable UUID ekycId,
            @Valid @RequestBody RequestEkycProcessDTO dto
    ) {
        adminService.processEkyc(ekycId, dto.getDecision(), dto.getNote());
        return ResponseEntity.ok(ApiResponse.success("Hồ sơ eKYC đã được xử lý thành công"));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<ResponseAdminReportDTO>>> getReports(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getReports(status)));
    }

    @PostMapping("/reports/{reportId}/resolve")
    public ResponseEntity<ApiResponse<String>> resolveReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody RequestReportResolveDTO dto
    ) {
        adminService.resolveReport(reportId, dto.getDecision(), dto.getPenaltyPoints(), dto.getNote());
        return ResponseEntity.ok(ApiResponse.success("Báo cáo vi phạm đã được xử lý thành công"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<ResponseAdminCategoryDTO>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllCategories()));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<ResponseJobCategoryDTO>> createCategory(
            @Valid @RequestBody RequestCategoryDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.createCategory(dto)));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<ResponseJobCategoryDTO>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody RequestCategoryDTO dto
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminService.updateCategory(id, dto)));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<String>> deleteCategory(@PathVariable UUID id) {
        adminService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa danh mục thành công"));
    }
}
