package com.flashjobweb.service;

import com.flashjobweb.dto.request.RequestProfileDTO;
import com.flashjobweb.dto.response.ResponseProfileDTO;

public interface ProfileService {
    ResponseProfileDTO getProfile();
    void changeMode(String mode);
    void isAvailability(boolean availability,Double longitude,Double latitude);
    void updateProfile(RequestProfileDTO requestProfileDTO);
    String uploadAvatar(org.springframework.web.multipart.MultipartFile file);
}
