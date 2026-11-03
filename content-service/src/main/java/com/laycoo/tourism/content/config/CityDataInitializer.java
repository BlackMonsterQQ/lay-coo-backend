package com.laycoo.tourism.content.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laycoo.tourism.content.entity.City;
import com.laycoo.tourism.content.mapper.CityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 初始化国内全部地级及以上城市。
 * 只有杭州有本地精选内容，其余城市由 AI 基于通用知识生成。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class CityDataInitializer implements ApplicationRunner {

    private final CityMapper cityMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<City> cities;
        try (InputStream inputStream = new ClassPathResource("seed/city-seed.json").getInputStream()) {
            cities = objectMapper.readValue(inputStream, new TypeReference<List<City>>() {
            });
        }
        if (cities.isEmpty()) {
            return;
        }
        long current = cityMapper.selectCount(null);
        if (current == cities.size()) {
            return;
        }
        // 城市是参考数据，版本升级时整体重建，避免残留旧版国外城市。
        cityMapper.delete(null);
        for (City city : cities) {
            city.setId(null);
            if (city.getEnabled() == null) {
                city.setEnabled(1);
            }
            if (city.getHasContent() == null) {
                city.setHasContent(0);
            }
            cityMapper.insert(city);
        }
        log.info("初始化国内城市 {} 个", cities.size());
    }
}
