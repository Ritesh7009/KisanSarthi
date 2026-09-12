package com.kisansarthi.config;

import com.kisansarthi.auth.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/mandis/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/crops/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/weather/**").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                // Role-restricted endpoints
                .requestMatchers("/api/v1/mandis/*/queue/next", "/api/v1/mandis/*/queue/*/call").hasAnyRole("ADMIN", "MANDI_OPERATOR", "MANDI_MANAGER")
                .requestMatchers("/api/v1/reports/**").hasAnyRole("ADMIN", "MANDI_MANAGER", "DISTRICT_OFFICER")
                .requestMatchers("/api/v1/sms/**").hasAnyRole("ADMIN", "MANDI_OPERATOR", "MANDI_MANAGER", "DISTRICT_OFFICER")
                // All other API endpoints require authenticated user
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        
        // Explicit allow-list of frontend origins (Render, Cloud Run, and localhost for dev)
        List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173",
            "https://kisansarthi-vsne.onrender.com"
        ));

        // Dynamically include deployment origin if specified in environment
        String appUrl = System.getenv("APP_URL");
        if (appUrl != null && !appUrl.isBlank()) {
            allowedOrigins.add(appUrl.trim().replaceAll("/+$", ""));
        }
        String frontendUrl = System.getenv("FRONTEND_URL");
        if (frontendUrl != null && !frontendUrl.isBlank()) {
            allowedOrigins.add(frontendUrl.trim().replaceAll("/+$", ""));
        }
        String corsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
        if (corsEnv != null && !corsEnv.isBlank()) {
            for (String origin : corsEnv.split(",")) {
                if (!origin.isBlank()) {
                    allowedOrigins.add(origin.trim().replaceAll("/+$", ""));
                }
            }
        }

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedOriginPatterns(List.of(
            "https://*.run.app",
            "https://*.onrender.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
