package com.laycoo.tourism.interaction;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.laycoo.tourism")
@MapperScan("com.laycoo.tourism.interaction.mapper")
@EnableFeignClients(basePackages = "com.laycoo.tourism.interaction.feign")
public class InteractionApplication {

    public static void main(String[] args) {
        SpringApplication.run(InteractionApplication.class, args);
    }
}
