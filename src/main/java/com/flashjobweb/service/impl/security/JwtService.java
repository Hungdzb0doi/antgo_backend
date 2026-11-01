package com.flashjobweb.service.impl.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {
    private static final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_TIME = 86400000;

    public String generateToken(UserDetails userDetails, String sessionId) {
        var builder = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new java.util.Date(System.currentTimeMillis()))
                .setExpiration(new java.util.Date(System.currentTimeMillis() + EXPIRATION_TIME));
        if (sessionId != null && !sessionId.isBlank()) {
            builder.claim("sid", sessionId);
        }
        return builder.signWith(io.jsonwebtoken.SignatureAlgorithm.HS256, getSecretKey()).compact();
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, null);
    }

    public String extractSessionId(String token) {
        try {
            return extractClaim(token, claims -> claims.get("sid", String.class));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isSessionSuperseded(String token, UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails cud && cud.getUser() != null) {
            String activeSid = cud.getUser().getActiveSessionId();
            if (activeSid != null && !activeSid.trim().isEmpty()) {
                String tokenSid = extractSessionId(token);
                return tokenSid == null || !activeSid.equals(tokenSid);
            }
        }
        return false;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        if (username == null || !username.equals(userDetails.getUsername()) || isTokenExpired(token)) {
            return false;
        }
        if (isSessionSuperseded(token, userDetails)) {
            return false;
        }
        return true;
    }

    public boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token){
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
