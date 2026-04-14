package com.example.itfinance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security")
public class JwtProperties {
    private String jwtSecret;
    private long tokenExpireMinutes = 120;
    private boolean allowDemoLogin = false;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenExpireMinutes() {
        return tokenExpireMinutes;
    }

    public void setTokenExpireMinutes(long tokenExpireMinutes) {
        this.tokenExpireMinutes = tokenExpireMinutes;
    }

    public boolean isAllowDemoLogin() {
        return allowDemoLogin;
    }

    public void setAllowDemoLogin(boolean allowDemoLogin) {
        this.allowDemoLogin = allowDemoLogin;
    }
}
