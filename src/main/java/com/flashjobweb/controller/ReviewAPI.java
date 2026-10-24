package com.flashjobweb.controller;

import com.flashjobweb.dto.request.RequestReviewDTO;
import com.flashjobweb.dto.response.ResponseReviewDTO;
import com.flashjobweb.service.ReviewService;
import com.flashjobweb.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/review")
@RequiredArgsConstructor
public class ReviewAPI {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<ResponseReviewDTO>> createReview(@Valid @RequestBody RequestReviewDTO requestReviewDTO) {
        ResponseReviewDTO result = reviewService.createReview(requestReviewDTO);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/application/{applicationId}/me")
    public ResponseEntity<ApiResponse<ResponseReviewDTO>> getMyReviewForApplication(@PathVariable UUID applicationId) {
        ResponseReviewDTO result = reviewService.getMyReviewForApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ResponseReviewDTO>>> getReviewsByReviewee(@PathVariable UUID userId) {
        List<ResponseReviewDTO> result = reviewService.getReviewsByReviewee(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
