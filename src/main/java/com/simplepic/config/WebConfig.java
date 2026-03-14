package com.simplepic.config;

import com.simplepic.interceptor.AuthInterceptor;
import com.simplepic.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve static files from classpath, but don't handle root path
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/");
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/");
        registry.addResourceHandler("/lib/**")
                .addResourceLocations("classpath:/static/lib/");
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/static/fonts/");
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add rate limit interceptor
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health");

        // Add auth interceptor (don't exclude "/" or "/upload.html" so they can be handled)
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/upload",
                        "/api/image/**",
                        "/api/auth/**",
                        "/login.html",
                        "/css/**",
                        "/js/**",
                        "/lib/**",
                        "/fonts/**",
                        "/favicon.ico",
                        "/admin/login.html",
                        "/favicon.ico",
                        "*.ico",
                        "*.png",
                        "*.jpg",
                        "*.jpeg",
                        "*.gif",
                        "*.svg",
                        "*.woff",
                        "*.woff2",
                        "*.ttf"
                );
    }
}