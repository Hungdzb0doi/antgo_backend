package com.flashjobweb.controller;

import com.flashjobweb.dto.request.RequestEkycDTO;
import com.flashjobweb.service.EkycService;
import com.flashjobweb.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identify")
@RequiredArgsConstructor
public class IdentifyAPI {
    private final EkycService ekycService;
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Object>> submitEkyc(@ModelAttribute RequestEkycDTO requestEkycDTO) {
        ekycService.submitEkyc(requestEkycDTO);
        return ResponseEntity.ok(ApiResponse.created());
    }
}
