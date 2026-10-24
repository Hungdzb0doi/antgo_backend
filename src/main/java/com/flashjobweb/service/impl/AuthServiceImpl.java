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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:satan22072005@gmail.com}")
    private String brevoSenderEmail;
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

    private boolean sendOtpViaBrevoHttp(String apiKey, String recipientEmail, String otp) {
        try {
            String senderEmail = (brevoSenderEmail != null && !brevoSenderEmail.isBlank()) ? brevoSenderEmail : System.getenv("BREVO_SENDER_EMAIL");
            if (senderEmail == null || senderEmail.isBlank()) {
                senderEmail = "satan22072005@gmail.com";
            }
            String jsonPayload = String.format(
                "{\"sender\":{\"name\":\"AntGo Support\",\"email\":\"%s\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"Mã OTP đặt lại mật khẩu AntGo\",\"htmlContent\":\"<p>Mã OTP đặt lại mật khẩu của bạn là: <strong style='font-size:20px;color:#ea580c;'>%s</strong> (Hiệu lực 5 phút).</p>\"}",
                senderEmail, recipientEmail, otp
            );

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
                .header("api-key", apiKey.trim())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(java.time.Duration.ofSeconds(5))
                .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                System.out.println("[AntGo Brevo] Gửi OTP thành công qua HTTP tới: " + recipientEmail);
                return true;
            } else {
                System.err.println("[AntGo Brevo] Gửi OTP thất bại, status: " + response.statusCode() + ", body: " + response.body());
            }
        } catch (Exception ex) {
            System.err.println("[AntGo Brevo] Lỗi gọi Brevo API: " + ex.getMessage());
        }
        return false;
    }

    @Override
    public void generateAndSendOtp(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setResetPasswordOtp(otp);
        user.setOtpTime(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        boolean sent = false;
        String brevoKey = (brevoApiKey != null && !brevoApiKey.isBlank()) ? brevoApiKey : System.getenv("BREVO_API_KEY");
        if (brevoKey != null && !brevoKey.isBlank()) {
            sent = sendOtpViaBrevoHttp(brevoKey, email, otp);
        }

        if (!sent) {
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setTo(email);
                mailMessage.setSubject("Mã OTP đặt lại mật khẩu AntGo");
                mailMessage.setText("Mã xác nhận đặt lại mật khẩu của bạn là: " + otp + "\nMã có hiệu lực trong 5 phút.");
                javaMailSender.send(mailMessage);
                sent = true;
            } catch (Exception e) {
                System.err.println("[AntGo Mail] Không thể gửi email qua SMTP: " + e.getMessage());
            }
        }

        if (!sent) {
            throw new AppException(ErrorCode.CANNOT_SEND_EMAIL);
        }
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
