package com.flashjobweb.service.impl.security;

import com.flashjobweb.entity.UserEntity;
import com.flashjobweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


import java.util.List;
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService{
    private final UserRepository userRepository;
    @Override
    public CustomUserDetails loadUserByUsername(String username) {
        UserEntity user = userRepository.findByPhone(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + username));
        String mode = user.getCurrentMode() != null ? user.getCurrentMode().toUpperCase() : "WORKER";
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + mode);

        return new CustomUserDetails(user, List.of(authority));
    }
}
