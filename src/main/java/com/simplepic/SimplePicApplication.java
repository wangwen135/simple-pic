package com.simplepic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Simple-Pic Application Entry Point
 * 本地图床应用启动类
 */
@SpringBootApplication
@EnableScheduling
public class SimplePicApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimplePicApplication.class, args);
    }
}