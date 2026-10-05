package com.flashjobweb.controller;

import com.flashjobweb.dto.request.LocationPayload;
import com.flashjobweb.dto.response.ResponseLocationWorkerDTO;
import com.flashjobweb.service.ProfileService;
import com.flashjobweb.service.impl.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class LocationWebSocketAPI {
   private final ProfileService profileService;
   private final SimpMessagingTemplate messagingTemplate;
    @MessageMapping("/locationworker")
    public void location(@Payload LocationPayload payload, Principal principal){
        if (principal != null) {
            SecurityContextHolder.getContext().setAuthentication((Authentication) principal);
            CustomUserDetails userDetails= (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            ResponseLocationWorkerDTO responseLocationWorkerDTO=new ResponseLocationWorkerDTO(
                    userDetails.getUser().getId(),
                    userDetails.getUser().getFullName(),
                    payload.getLongitude(),
                    payload.getLatitude()
            );

            profileService.isAvailability(true, payload.getLongitude(), payload.getLatitude());
            messagingTemplate.convertAndSend("/topic/locationworker", responseLocationWorkerDTO);
        }
    }
}
