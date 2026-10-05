package com.flashjobweb.controller;

import com.flashjobweb.service.JobCategoryService;
import com.flashjobweb.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class JobCategoryAPI {
    private final JobCategoryService jobCategoryService;
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getCategories(){
        return ResponseEntity.ok(ApiResponse.success(jobCategoryService.getAllCategories()));
    }
}
