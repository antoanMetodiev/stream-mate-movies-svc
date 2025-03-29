package com.example.streammatemoviesvc.app.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // Позволява всички пътища
                .allowedOrigins("https://dark-sissy-stream-mate-b1e9d2a2.koyeb.app", "http://localhost:8080")  // Разрешава само даден домейн
                .allowedMethods("GET", "POST", "PUT", "DELETE")  // Разрешава само определени методи
                .allowedHeaders("*");  // Позволява всички хедъри
    }
}