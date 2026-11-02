package com.laycoo.tourism.interaction.service;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.common.exception.BusinessException;
import com.laycoo.tourism.interaction.feign.ContentClient;
import com.laycoo.tourism.interaction.feign.StatsPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 对内容服务调用的统一封装：核心数据强依赖，统计类调用失败仅告警。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentGateway {

    private final ContentClient contentClient;

    public List<ContentCard> batch(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        try {
            ApiResponse<List<ContentCard>> response = contentClient.batch(List.copyOf(ids));
            if (response != null && response.getCode() == 0 && response.getData() != null) {
                return response.getData();
            }
            log.warn("内容服务返回异常: {}", response == null ? "null" : response.getMsg());
        } catch (Exception e) {
            log.warn("调用内容服务失败: {}", e.getMessage());
        }
        return List.of();
    }

    public ContentCard require(Long contentId) {
        List<ContentCard> cards = batch(List.of(contentId));
        if (cards.isEmpty()) {
            throw BusinessException.notFound("内容不存在或已下架");
        }
        return cards.get(0);
    }

    public void increase(Long contentId, String type, long delta) {
        try {
            contentClient.stats(contentId, StatsPayload.of(type, delta));
        } catch (Exception e) {
            log.warn("同步内容统计失败 contentId={}, type={}: {}", contentId, type, e.getMessage());
        }
    }

    public void updateRating(Long contentId, BigDecimal avg, long count) {
        try {
            contentClient.stats(contentId, StatsPayload.rating(avg, count));
        } catch (Exception e) {
            log.warn("同步评分失败 contentId={}: {}", contentId, e.getMessage());
        }
    }
}
