package com.supporttriage.security;

import jakarta.servlet.*; 
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwtUtil.isValid(token)) {
                String email = jwtUtil.extractEmail(token);

                var userDetails = userDetailsService.loadUserByUsername(email);

                var auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}


/**
 * Custom filter that runs on every incoming request.
 *
 * Purpose:
 * - Intercepts HTTP requests
 * - Extracts JWT token from Authorization header
 * - Validates the token
 * - Sets authentication in Spring Security context
 *
 * Flow:
 * 1. Read "Authorization" header
 * 2. Check if it starts with "Bearer "
 * 3. Extract token
 * 4. Validate token using JwtUtil
 * 5. Load user details from database
 * 6. Set authentication in SecurityContext
 *
 * If token is valid:
 * → user is authenticated
 *
 * If missing/invalid:
 * → request continues but will be blocked by SecurityConfig
 */