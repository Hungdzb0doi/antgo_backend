package com.flashjobweb.service.impl;

import com.flashjobweb.dto.request.RequestLoginDTO;
import com.flashjobweb.dto.request.RequestRegisterDTO;
import com.flashjobweb.dto.request.RequestResetPasswordDTO;
import com.flashjobweb.dto.response.ResponseLoginDTO;
import com.flashjobweb.entity.EmployerProfileEntity;
import com.flashjobweb.entity.UserEntity;
import com.flashjobweb.entity.WorkerProfileEntity;
import com.flashjobweb.exception.AppException;
import com.flashjobweb.exception.ErrorCode;
import com.flashjobweb.repository.EmployerProfileRepository;
import com.flashjobweb.repository.UserRepository;
import com.flashjobweb.repository.WorkerProfileRepository;
import com.flashjobweb.service.AuthService;
import com.flashjobweb.service.impl.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final EmployerProfileRepository employerProfileRepository;
    private final WorkerProfileRepository workerProfileRepository;
    @Override
    public void register(RequestRegisterDTO requestRegisterDTO) {
        if (userRepository.existsByPhone(requestRegisterDTO.getPhone())){
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
        UserEntity user = new UserEntity();
        modelMapper.map(requestRegisterDTO,user);
        WorkerProfileEntity workerProfileEntity=new WorkerProfileEntity();
        EmployerProfileEntity employerProfileEntity=new EmployerProfileEntity();
        workerProfileEntity.setUserEntity(user);
        employerProfileEntity.setUserEntity(user);
        workerProfileRepository.save(workerProfileEntity);
        employerProfileRepository.save(employerProfileEntity);
        user.setPasswordHash(passwordEncoder.encode(requestRegisterDTO.getPasswordHash()));
        userRepository.save(user);
    }
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    @Override
    public ResponseLoginDTO login(RequestLoginDTO requestLoginDTO) {
        Authentication authentication=authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        requestLoginDTO.getPhone(),requestLoginDTO.getPassword()));
        UserDetails userDetails=(UserDetails) authentication.getPrincipal();
        String token=jwtService.generateToken(userDetails);
        return ResponseLoginDTO.builder().token(token)
                .roles(userDetails.getAuthorities().stream().map(Object::toString).toList())
                .build();
    }
    private final JavaMailSender javaMailSender;
    @Override
    public void generateAndSendOtp(String email) {
        UserEntity user=userRepository.findByEmail(email)
                .orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        user.setResetPasswordOtp(String.format("%06d",new Random().nextInt(999999)));
        user.setOtpTime(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        SimpleMailMessage mailMessage=new SimpleMailMessage();
        mailMessage.setTo(email);
        mailMessage.setSubject("Your OTP for Password Reset");
        mailMessage.setText("Your OTP for password reset is: " + user.getResetPasswordOtp());
        javaMailSender.send(mailMessage);
    }
    @Override
    public void checkOtp(RequestResetPasswordDTO requestResetPasswordDTO) {
        UserEntity user=userRepository.findByEmail(requestResetPasswordDTO.getEmail())
                .orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        if(user.getResetPasswordOtp()==null || !user.getResetPasswordOtp().equals(requestResetPasswordDTO.getOtp())) {
            throw new AppException(ErrorCode.INVALID_OTP);
        }
    }
    @Override
    public void resetPassword(RequestResetPasswordDTO requestResetPasswordDTO) {
        UserEntity user=userRepository.findByEmail(requestResetPasswordDTO.getEmail())
                .orElseThrow(()->new AppException(ErrorCode.USER_NOT_FOUND));
        user.setPasswordHash(passwordEncoder.encode(requestResetPasswordDTO.getNewPassword()));
        user.setResetPasswordOtp(null);
        user.setOtpTime(null);
        userRepository.save(user);
    }
}
