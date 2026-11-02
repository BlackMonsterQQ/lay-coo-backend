package com.laycoo.tourism.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.laycoo.tourism")
@MapperScan("com.laycoo.tourism.ai.mapper")
@EnableFeignClients(basePackages = "com.laycoo.tourism.ai.feign")
@ConfigurationPropertiesScan("com.laycoo.tourism.ai.config")
public class AiApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
