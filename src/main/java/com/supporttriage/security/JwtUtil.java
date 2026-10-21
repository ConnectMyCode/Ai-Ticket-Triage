package com.supporttriage.security;

import io.jsonwebtoken.*; 
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;


import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    
    //Validation of Token
    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            System.out.println("Validation Successfull");     //🚩🚩
            return true;
        } catch (JwtException e) {
        	System.out.println("Error: "+e.getMessage());     //🚩🚩
            return false;
        }
    }
}



/**
 * Utility class for handling JWT operations.
 *
 * Responsibilities:
 * - Generate JWT token during login
 * - Extract email (subject) from token
 * - Validate token integrity and expiration
 *
 * Uses:
 * - Secret key from application.properties
 * - HMAC-SHA256 signing algorithm (via jjwt)
 *
 * Token contains:
 * - subject (email)
 * - issued time
 * - expiration time
 *
 * Important:
 * This class does NOT store anything — it only creates and validates tokens.
 */