package com.flashjobweb.service;

import com.flashjobweb.dto.request.RequestDeviceTokenDTO;

public interface DeviceTokenService {
    void registerToken(RequestDeviceTokenDTO request);
    void removeToken(String token);
}
