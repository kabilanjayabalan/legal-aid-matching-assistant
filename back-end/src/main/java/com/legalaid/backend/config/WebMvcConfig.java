package com.legalaid.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 🔥 REQUIRED for SockJS iframe + jsonp fallbacks
        registry.addResourceHandler("/ws-chat/**")
                .addResourceLocations("classpath:/META-INF/resources/");
    }
}
