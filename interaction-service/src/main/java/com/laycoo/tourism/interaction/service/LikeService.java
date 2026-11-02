package com.laycoo.tourism.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laycoo.tourism.interaction.entity.LikeRecord;
import com.laycoo.tourism.interaction.mapper.LikeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeMapper likeMapper;
    private final ContentGateway contentGateway;

    public boolean add(Long userId, Long contentId) {
        contentGateway.require(contentId);
        if (find(userId, contentId) == null) {
            LikeRecord record = new LikeRecord();
            record.setUserId(userId);
            record.setContentId(contentId);
            record.setCreateTime(LocalDateTime.now());
            likeMapper.insert(record);
            contentGateway.increase(contentId, "like", 1);
        }
        return true;
    }

    public boolean remove(Long userId, Long contentId) {
        int deleted = likeMapper.delete(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getContentId, contentId));
        if (deleted > 0) {
            contentGateway.increase(contentId, "like", -1);
        }
        return false;
    }

    public boolean status(Long userId, Long contentId) {
        return find(userId, contentId) != null;
    }

    private LikeRecord find(Long userId, Long contentId) {
        return likeMapper.selectOne(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getContentId, contentId)
                .last("limit 1"));
    }

    public long count(Long contentId) {
        return likeMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getContentId, contentId));
    }

    public List<Long> contentIds(Long userId) {
        return likeMapper.selectList(new LambdaQueryWrapper<LikeRecord>()
                        .eq(LikeRecord::getUserId, userId))
                .stream().map(LikeRecord::getContentId).toList();
    }
}
