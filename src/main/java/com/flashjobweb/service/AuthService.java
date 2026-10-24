package com.flashjobweb.service;

import com.flashjobweb.dto.request.RequestLoginDTO;
import com.flashjobweb.dto.request.RequestRegisterDTO;
import com.flashjobweb.dto.request.RequestResetPasswordDTO;
import com.flashjobweb.dto.response.ResponseLoginDTO;

public interface AuthService {
void register(RequestRegisterDTO requestRegisterDTO);
ResponseLoginDTO login(RequestLoginDTO requestLoginDTO);
void generateAndSendOtp(String email);
void checkOtp(RequestResetPasswordDTO requestResetPasswordDTO);
void resetPassword(RequestResetPasswordDTO requestResetPasswordDTO);
}
