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
                        .requestMatchers("/api/auth/login", "/api/auth/refresh", "/uploads/**", "/error").permitAll()
                        .requestMatchers("/api/report/trend", "/api/report/category", "/api/report/monthly-comparison").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/expense/approval/pending").hasAnyRole("ADMIN", "FINANCE")
                        .requestMatchers(HttpMethod.POST, "/api/expense/approval/**").hasAnyRole("ADMIN", "FINANCE")
                        .requestMatchers(HttpMethod.POST, "/api/payment/allocate-auto/**",
                                "/api/payment/allocate-manual/**")
                        .hasAnyRole("ADMIN", "FINANCE")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
