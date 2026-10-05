package com.flashjobweb.controller;

import com.flashjobweb.dto.request.RequestProfileDTO;
import com.flashjobweb.service.ProfileService;
import com.flashjobweb.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileAPI {
    private final ProfileService profileService;
    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getProfile(){
        return ResponseEntity.ok(ApiResponse.success(profileService.getProfile()));
    }
    @PatchMapping("/mode")
    public ResponseEntity<ApiResponse<Object>> changeMode(@RequestParam String mode){
        profileService.changeMode(mode);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    @PatchMapping("/availability")
    public ResponseEntity<ApiResponse<Object>> changeAvailability(@RequestParam boolean availability){
        profileService.isAvailability(availability, null, null);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    @PutMapping
    public ResponseEntity<ApiResponse<Object>> updateProfile(@RequestBody RequestProfileDTO requestProfileDTO){
        profileService.updateProfile(requestProfileDTO);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping(value = "/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String fileId = profileService.uploadAvatar(file);
        return ResponseEntity.ok(ApiResponse.success(fileId));
    }
}
