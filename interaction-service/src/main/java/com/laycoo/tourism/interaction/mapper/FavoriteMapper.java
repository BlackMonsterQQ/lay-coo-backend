package com.laycoo.tourism.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.laycoo.tourism.interaction.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}
