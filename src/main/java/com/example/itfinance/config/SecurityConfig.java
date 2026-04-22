package com.example.itfinance.config;

import com.example.itfinance.security.JwtAuthFilter;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/api/auth/face-login", "/uploads/**",
                                "/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/file/upload").permitAll()
                        .requestMatchers("/api/report/trend", "/api/report/category", "/api/report/monthly-comparison")
                        .permitAll()
                        .requestMatchers("/api/ai/**", "/api/report/export/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/expense/approval/pending").hasAnyRole("ADMIN", "FINANCE")
                        .requestMatchers(HttpMethod.POST, "/api/expense/approval/**").hasAnyRole("ADMIN", "FINANCE")
                        .requestMatchers(HttpMethod.POST, "/api/payment/allocate-auto/**",
                                "/api/payment/allocate-manual/**")
                        .hasAnyRole("ADMIN", "FINANCE")
                        .requestMatchers(HttpMethod.GET, "/api/face/list", "/api/face/logs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/auth/login-logs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/face/logs/**", "/api/face/profile/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/face/enroll").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/face/profile/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/user/admin/create-with-face").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
