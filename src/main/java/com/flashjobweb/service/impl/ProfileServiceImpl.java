package com.flashjobweb.service.impl;

import com.flashjobweb.dto.request.RequestProfileDTO;
import com.flashjobweb.dto.response.ResponseProfileDTO;
import com.flashjobweb.entity.EmployerProfileEntity;
import com.flashjobweb.entity.UserEntity;
import com.flashjobweb.entity.WorkerProfileEntity;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.EmployerProfileRepository;
import com.flashjobweb.repository.UserRepository;
import com.flashjobweb.repository.WorkerProfileRepository;
import com.flashjobweb.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileServiceImpl implements ProfileService {
    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final com.flashjobweb.service.impl.outside.GoogleDriveService googleDriveService;

    @Override
    public ResponseProfileDTO getProfile() {
        String curentPhone= SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user=userRepository.findByPhone(curentPhone).orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        WorkerProfileEntity workerProfile=workerProfileRepository.findById(user.getId()).orElseThrow(()->new AppException(ErrorCode.WORKER_PROFILE_NOT_FOUND));
        EmployerProfileEntity employerProfile=employerProfileRepository.findById(user.getId()).orElseThrow(()->new AppException(ErrorCode.EMPLOYER_PROFILE_NOT_FOUND));

        return ResponseProfileDTO.builder()
                .identityVerified(user.getIdentityVerified())
                .currentMode(user.getCurrentMode())
                .phone(user.getPhone())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .workerScore(workerProfile.getWorkerScore())
                .skills(workerProfile.getSkills())
                .isAvailable(workerProfile.getIsAvailable())
                .employerScore(employerProfile.getEmployerScore())
                .taxId(employerProfile.getTaxId())
                .employerName(employerProfile.getEmployerName())
                .build();
    }
    @Override
    public void changeMode(String mode) {
        String curentPhone= SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user=userRepository.findByPhone(curentPhone).orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        String upperMode = (mode != null && "EMPLOYER".equalsIgnoreCase(mode.trim())) ? "EMPLOYER" : "WORKER";
        user.setCurrentMode(upperMode);
        userRepository.save(user);
    }
    private final StringRedisTemplate redisTemplate;
    private static final String GEO_KEY = "worker:locations";

    private static final String ACTIVE_KEY_PREFIX = "worker:active:";
    private static final long TIME_TO_LIVE_MINUTES = 30;
    @Override
    public void isAvailability(boolean availability,Double longitude,Double latitude) {
        String curentPhone= SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user=userRepository.findByPhone(curentPhone).orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        WorkerProfileEntity workerProfile=workerProfileRepository.findById(user.getId()).orElseThrow(()->new AppException(ErrorCode.WORKER_PROFILE_NOT_FOUND));

        if (availability && workerProfile.getWorkerScore() != null && workerProfile.getWorkerScore() < 50) {
            throw new AppException(ErrorCode.REPUTATION_SCORE_TOO_LOW);
        }

        workerProfile.setIsAvailable(availability);
        workerProfileRepository.save(workerProfile);
        if (availability) {
            if (longitude != null && latitude != null && longitude != 0.0 && latitude != 0.0) {

                Point location = new Point(longitude, latitude);
                redisTemplate.opsForGeo().add(GEO_KEY, location, user.getId().toString());


                redisTemplate.opsForValue().set(
                        ACTIVE_KEY_PREFIX + user.getId(),
                        "online",
                        Duration.ofMinutes(TIME_TO_LIVE_MINUTES)
                );
            }
        } else {
            redisTemplate.opsForGeo().remove(GEO_KEY, user.getId().toString());
            redisTemplate.delete(ACTIVE_KEY_PREFIX + user.getId().toString());
        }
    }


    @Override
    public void updateProfile(RequestProfileDTO requestProfileDTO) {
        String curentPhone= SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user=userRepository.findByPhone(curentPhone).orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        
        if (requestProfileDTO.getEmail() != null && !requestProfileDTO.getEmail().trim().isEmpty()) {
            user.setEmail(requestProfileDTO.getEmail().trim());
        }
        if (requestProfileDTO.getFullName() != null && !requestProfileDTO.getFullName().trim().isEmpty()) {
            user.setFullName(requestProfileDTO.getFullName().trim());
        }
        if (requestProfileDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(requestProfileDTO.getAvatarUrl().trim());
        }
        userRepository.save(user);

        WorkerProfileEntity workerProfile=workerProfileRepository.findById(user.getId()).orElseThrow(()->new AppException(ErrorCode.WORKER_PROFILE_NOT_FOUND));
        EmployerProfileEntity employerProfile=employerProfileRepository.findById(user.getId()).orElseThrow(()-> new AppException(ErrorCode.EMPLOYER_PROFILE_NOT_FOUND));
        if(requestProfileDTO.getSkills()!=null){
            workerProfile.setSkills(requestProfileDTO.getSkills());
            workerProfileRepository.save(workerProfile);
        }
        if(requestProfileDTO.getEmployerName()!=null && requestProfileDTO.getTaxId()!=null){
            employerProfile.setEmployerName(requestProfileDTO.getEmployerName());
            employerProfile.setTaxId(requestProfileDTO.getTaxId());
            employerProfileRepository.save(employerProfile);
        }
    }

    @Override
    public String uploadAvatar(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        String currentPhone = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByPhone(currentPhone)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        try {
            String fileId = googleDriveService.uploadChatFile(file);
            user.setAvatarUrl(fileId);
            userRepository.save(user);
            return fileId;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tải ảnh đại diện lên Google Drive: " + e.getMessage(), e);
        }
    }
}
