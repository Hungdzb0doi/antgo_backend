package com.flashjobweb.service.impl;

import com.flashjobweb.dto.request.RequestEkycDTO;
import com.flashjobweb.entity.EkycEntity;
import com.flashjobweb.entity.UserEntity;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.EkycRepository;
import com.flashjobweb.repository.UserRepository;
import com.flashjobweb.service.EkycService;
import com.flashjobweb.service.impl.outside.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EkycServiceImpl implements EkycService {
    private final UserRepository userRepository;
    private final GoogleDriveService googleDriveService;
    private final EkycRepository ekycRepository;
    @Override
    public void submitEkyc(RequestEkycDTO requestEkycDTO) {
        String curentPhone= SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity userEntity=userRepository.findByPhone(curentPhone).orElseThrow(()->new AppException(ErrorCode.PHONE_ALREADY_EXISTS));
        try {
            String frontUrl = googleDriveService.uploadFile(requestEkycDTO.getFront());
            String backUrl = googleDriveService.uploadFile(requestEkycDTO.getBack());
            String faceUrl = googleDriveService.uploadFile(requestEkycDTO.getFace());

            EkycEntity ekycEntity= new EkycEntity();
            ekycEntity.setIdCardFrontUrl(frontUrl);
            ekycEntity.setIdCardBackUrl(backUrl);
            ekycEntity.setFaceImageUrl(faceUrl);
            ekycEntity.setUser(userEntity);
            ekycEntity.setStatus("PENDING");
            ekycRepository.save(ekycEntity);
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Lỗi: " + e.getMessage());
        }
    }
}
