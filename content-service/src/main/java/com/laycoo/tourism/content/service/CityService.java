package com.laycoo.tourism.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laycoo.tourism.common.exception.BusinessException;
import com.laycoo.tourism.content.entity.City;
import com.laycoo.tourism.content.mapper.CityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityService {

    private final CityMapper cityMapper;

    public List<City> list() {
        return cityMapper.selectList(new LambdaQueryWrapper<City>()
                .eq(City::getEnabled, 1)
                .orderByAsc(City::getSort));
    }

    public City byCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return cityMapper.selectOne(new LambdaQueryWrapper<City>()
                .eq(City::getCode, code)
                .eq(City::getEnabled, 1)
                .last("limit 1"));
    }

    public City require(String code) {
        City city = byCode(code);
        if (city == null) {
            throw BusinessException.notFound("城市不存在: " + code);
        }
        return city;
    }
}
