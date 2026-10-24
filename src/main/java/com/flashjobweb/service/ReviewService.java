package com.flashjobweb.service;

import com.flashjobweb.dto.request.RequestReviewDTO;
import com.flashjobweb.dto.response.ResponseReviewDTO;

import java.util.List;
import java.util.UUID;

public interface ReviewService {
    ResponseReviewDTO createReview(RequestReviewDTO requestReviewDTO);
    ResponseReviewDTO getMyReviewForApplication(UUID applicationId);
    List<ResponseReviewDTO> getReviewsByReviewee(UUID revieweeId);
    void evaluateMonthlyReputationScores();
}
