package com.flashjobweb.service.impl.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtWebSocketInterceptor implements ChannelInterceptor {


    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // Chỉ kiểm tra khi Frontend bắt đầu kết nối (CONNECT)
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {

            // Lấy Token từ Header
            List<String> authorization = accessor.getNativeHeader("Authorization");

            if (authorization != null && !authorization.isEmpty()) {
                String bearerToken = authorization.get(0);
                if (bearerToken.startsWith("Bearer ")) {
                    String token = bearerToken.substring(7);
                    try{
                        String userName=jwtService.extractUsername(token);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(userName);
                        if(jwtService.validateToken(token,userDetails)){
                            UsernamePasswordAuthenticationToken authenticationToken =
                                    new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());


                            accessor.setUser(authenticationToken);
                        }

                    }catch (Exception e){
                        System.out.println("Token không hợp lệ: " + e.getMessage());
                    }


                    System.out.println("Đã bắt được Token qua WebSocket: " + token);
                }
            }
        }
        return message;
    }
}