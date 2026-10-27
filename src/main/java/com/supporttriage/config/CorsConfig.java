package com.supporttriage.config;


import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration 
public class CorsConfig {
	
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Allow the local React + Vite frontend.
        // Add the frontend EC2 origin to this list when its IP is known.
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:5173", 
                "http://3.7.223.110"
                // Example later: "http://YOUR_FRONTEND_EC2_PUBLIC_IP"
        ));

        // Allow the HTTP methods used by the frontend.
        configuration.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow JWT authentication and JSON request headers.
        configuration.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type"));

        // JWT is sent through the Authorization header, not cookies.
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
	


