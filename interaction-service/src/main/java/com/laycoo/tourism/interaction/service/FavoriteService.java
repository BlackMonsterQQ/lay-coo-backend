package com.laycoo.tourism.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.laycoo.tourism.common.api.PageResult;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.interaction.entity.Favorite;
import com.laycoo.tourism.interaction.mapper.FavoriteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ContentGateway contentGateway;

    public boolean add(Long userId, Long contentId) {
        contentGateway.require(contentId);
        Favorite exists = find(userId, contentId);
        if (exists == null) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setContentId(contentId);
            favorite.setCreateTime(LocalDateTime.now());
            favoriteMapper.insert(favorite);
            contentGateway.increase(contentId, "favorite", 1);
        }
        return true;
    }

    public boolean remove(Long userId, Long contentId) {
        int deleted = favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getContentId, contentId));
        if (deleted > 0) {
            contentGateway.increase(contentId, "favorite", -1);
        }
        return false;
    }

    public boolean status(Long userId, Long contentId) {
        return find(userId, contentId) != null;
    }

    private Favorite find(Long userId, Long contentId) {
        return favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getContentId, contentId)
                .last("limit 1"));
    }

    public PageResult<ContentCard> list(Long userId, long page, long size) {
        long current = Math.max(page, 1);
        long pageSize = Math.min(Math.max(size, 1), 50);
        Page<Favorite> favoritePage = favoriteMapper.selectPage(new Page<>(current, pageSize),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime)
                        .orderByDesc(Favorite::getId));
        List<Long> ids = favoritePage.getRecords().stream().map(Favorite::getContentId).toList();
        List<ContentCard> cards = contentGateway.batch(ids);
        Map<Long, ContentCard> map = new LinkedHashMap<>();
        for (ContentCard card : cards) {
            map.put(card.getId(), card);
        }
        List<ContentCard> ordered = new ArrayList<>();
        for (Long id : ids) {
            ContentCard card = map.get(id);
            if (card != null) {
                ordered.add(card);
            }
        }
        return PageResult.of(ordered, favoritePage.getTotal(), current, pageSize);
    }

    public long count(Long contentId) {
        return favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getContentId, contentId));
    }

    public List<Long> contentIds(Long userId) {
        return favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime))
                .stream().map(Favorite::getContentId).toList();
    }
}
