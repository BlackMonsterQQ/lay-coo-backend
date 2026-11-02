package com.laycoo.tourism.recommend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.laycoo.tourism")
@MapperScan("com.laycoo.tourism.recommend.mapper")
@EnableFeignClients(basePackages = "com.laycoo.tourism.recommend.feign")
public class RecommendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecommendApplication.class, args);
    }
}
