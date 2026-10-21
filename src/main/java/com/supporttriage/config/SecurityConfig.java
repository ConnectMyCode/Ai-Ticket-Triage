package com.supporttriage.config;

import com.supporttriage.security.JwtFilter; 
import com.supporttriage.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtUtil jwtUtil,
                                           UserDetailsService userDetailsService,
                                           CorsConfigurationSource corsConfigurationSource) throws Exception {

        JwtFilter jwtFilter = new JwtFilter(jwtUtil, userDetailsService);

        http
        // Enable CORS using the configuration defined in CorsConfig.
        .cors(cors ->
            cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/health/**").permitAll()      
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}




/**
 * Spring Security configuration.
 *
 * Responsibilities:
 *
 * 1. Disable CSRF:
 * - Not needed for stateless REST APIs
 *
 * 2. Stateless session:
 * - No HTTP session is stored
 * - Authentication is handled via JWT
 *
 * 3. Authorization rules:
 * - /api/auth/** → public (signup & login)
 * - All other endpoints → require authentication 
 * 
 * 4. JWT Filter:
 * - Added before UsernamePasswordAuthenticationFilter
 * - Ensures JWT is processed before request reaches controllers
 *
 * 5. Password Encoder:
 * - BCrypt used for hashing passwords securely
 *
 * Flow:
 * Request → JwtFilter → Authentication → Controller
 */