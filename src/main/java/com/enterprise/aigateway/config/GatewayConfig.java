package com.enterprise.aigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class GatewayConfig {

  @Bean
  public CorsWebFilter corsWebFilter() {
    CorsConfiguration corsConfig = new CorsConfiguration();

    // Chỉ định các domain nội bộ được phép gọi đến Gateway (bảo mật: hạn chế dùng
    // '*')
    corsConfig.setAllowedOrigins(Collections.singletonList("https://internal-app.enterprise.com"));
    corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    corsConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-User-Id"));
    corsConfig.setAllowCredentials(true);
    corsConfig.setMaxAge(3600L); // Cache cấu hình CORS trong 1 giờ

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfig);

    return new CorsWebFilter(source);
  }
}