package com.laycoo.tourism.content.config;

import com.laycoo.tourism.content.entity.City;
import com.laycoo.tourism.content.mapper.CityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 初始化世界地图上可选的服务城市。
 * 目前只有杭州有本地内容，其余城市由 AI 基于通用知识生成。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class CityDataInitializer implements ApplicationRunner {

    private final CityMapper cityMapper;

    @Override
    public void run(ApplicationArguments args) {
        if (cityMapper.selectCount(null) > 0) {
            return;
        }
        List<City> cities = List.of(
                city("hangzhou", "杭州", "中国", "120.155100", "30.274100", 1, 1),
                city("suzhou", "苏州", "中国", "120.585300", "31.298900", 0, 2),
                city("shanghai", "上海", "中国", "121.473700", "31.230400", 0, 3),
                city("chengdu", "成都", "中国", "104.066500", "30.572300", 0, 4),
                city("xian", "西安", "中国", "108.939800", "34.341600", 0, 5),
                city("nanjing", "南京", "中国", "118.796900", "32.060300", 0, 6),
                city("tokyo", "东京", "日本", "139.691700", "35.689500", 0, 7),
                city("paris", "巴黎", "法国", "2.352200", "48.856600", 0, 8),
                city("newyork", "纽约", "美国", "-74.006000", "40.712800", 0, 9),
                city("london", "伦敦", "英国", "-0.127600", "51.507400", 0, 10),
                city("sydney", "悉尼", "澳大利亚", "151.209300", "-33.868800", 0, 11)
        );
        for (City city : cities) {
            cityMapper.insert(city);
        }
        log.info("初始化世界地图城市 {} 个", cities.size());
    }

    private City city(String code, String name, String country,
                      String longitude, String latitude, int hasContent, int sort) {
        City city = new City();
        city.setCode(code);
        city.setName(name);
        city.setCountry(country);
        city.setLongitude(new BigDecimal(longitude));
        city.setLatitude(new BigDecimal(latitude));
        city.setHasContent(hasContent);
        city.setEnabled(1);
        city.setSort(sort);
        return city;
    }
}
