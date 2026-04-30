package com.wwh.simplepic.config;

import com.wwh.simplepic.interceptor.AuthInterceptor;
import com.wwh.simplepic.interceptor.HotlinkProtectionInterceptor;
import com.wwh.simplepic.interceptor.RateLimitInterceptor;
import com.wwh.simplepic.model.SystemConfig;
import com.wwh.simplepic.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    private HotlinkProtectionInterceptor hotlinkProtectionInterceptor;

    @Autowired
    private ConfigService configService;

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

    /**
     * 配置跨域资源共享 - 允许所有来源
     */
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
                .addPathPatterns("/api/**", "/images/**")
                .excludePathPatterns("/api/health");

        // Add hotlink protection interceptor for image serving paths
        registry.addInterceptor(hotlinkProtectionInterceptor)
                .addPathPatterns("/image/**", "/images/**");

        // Add auth interceptor (don't exclude "/" or "/upload.html" so they can be handled)
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/health",
                        "/api/upload",
                        "/image/**",
                        "/images/**",
                        "/api/auth/**",
                        "/login.html",
                        "/css/**",
                        "/js/**",
                        "/lib/**",
                        "/fonts/**",
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