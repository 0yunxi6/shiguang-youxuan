package com.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private static final int HS512_MIN_SECRET_BYTES = 64;
    private static final String RESET_PASSWORD_PURPOSE = "PWD_RESET";
    private static final long RESET_PASSWORD_EXPIRATION = 10 * 60 * 1000L;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT 密钥未配置");
        }
        byte[] keyBytes = jwtSecret.trim().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < HS512_MIN_SECRET_BYTES) {
            throw new IllegalStateException("JWT 密钥长度不足：HS512 至少需要 64 个字节，请配置更长的 jwt.secret / JWT_SECRET");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return buildToken(userDetails.getUsername(), jwtExpiration);
    }

    public String generateToken(String username) {
        return buildToken(username, jwtExpiration);
    }

    public String generateResetPasswordToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + RESET_PASSWORD_EXPIRATION);

        return Jwts.builder()
                .setSubject(username)
                .claim("purpose", RESET_PASSWORD_PURPOSE)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = getUsernameFromToken(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            // Invalid JWT signature
        } catch (ExpiredJwtException e) {
            // Expired JWT token
        } catch (UnsupportedJwtException e) {
            // Unsupported JWT token
        } catch (IllegalArgumentException e) {
            // JWT claims string is empty
        }
        return false;
    }

    public boolean validateResetPasswordToken(String token) {
        try {
            Claims claims = getClaims(token);
            return RESET_PASSWORD_PURPOSE.equals(claims.get("purpose", String.class)) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private String buildToken(String username, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        try {
            return getClaims(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}
