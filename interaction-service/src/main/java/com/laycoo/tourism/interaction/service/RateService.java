package com.laycoo.tourism.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.common.exception.BusinessException;
import com.laycoo.tourism.interaction.dto.RateRequest;
import com.laycoo.tourism.interaction.entity.Rate;
import com.laycoo.tourism.interaction.mapper.RateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RateService {

    private final RateMapper rateMapper;
    private final ContentGateway contentGateway;

    public Map<String, Object> rate(Long userId, RateRequest request) {
        ContentCard card = contentGateway.require(request.getContentId());
        if (request.getScore() == null || request.getScore() < 1 || request.getScore() > 5) {
            throw new BusinessException("评分需在 1-5 之间");
        }
        List<Rate> before = rates(request.getContentId());
        Rate existing = before.stream()
                .filter(rate -> userId.equals(rate.getUserId()))
                .findFirst()
                .orElse(null);
        if (existing == null) {
            Rate rate = new Rate();
            rate.setUserId(userId);
            rate.setContentId(request.getContentId());
            rate.setScore(request.getScore());
            rate.setCreateTime(LocalDateTime.now());
            rateMapper.insert(rate);
        } else {
            existing.setScore(request.getScore());
            rateMapper.updateById(existing);
        }
        return refresh(request.getContentId(), userId, card, before);
    }

    public Map<String, Object> summary(Long contentId, Long userId) {
        ContentCard card = contentGateway.require(contentId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avg", card.getRating() == null ? BigDecimal.ZERO : card.getRating());
        result.put("count", card.getRatingCount() == null ? 0L : card.getRatingCount());
        result.put("mine", mine(rates(contentId), userId));
        return result;
    }

    /**
     * 评分合并策略：内容表保存的是「运营初始评分基线 + 用户评分」的加权结果。
     * 每次用户评分后，先用评分基线反推出原始基线与分数，再叠加全部用户评分，避免重复计数。
     */
    private Map<String, Object> refresh(Long contentId, Long userId, ContentCard card, List<Rate> before) {
        List<Rate> after = rates(contentId);
        long oldUserCount = before.size();
        BigDecimal oldUserSum = sumOf(before);
        BigDecimal shownCount = BigDecimal.valueOf(card.getRatingCount() == null ? 0 : card.getRatingCount());
        BigDecimal shownSum = (card.getRating() == null ? BigDecimal.ZERO : card.getRating()).multiply(shownCount);

        BigDecimal baseCount = shownCount.subtract(BigDecimal.valueOf(oldUserCount));
        if (baseCount.signum() < 0) {
            baseCount = BigDecimal.ZERO;
        }
        BigDecimal baseSum = shownSum.subtract(oldUserSum);
        if (baseSum.signum() < 0) {
            baseSum = BigDecimal.ZERO;
        }

        long newCount = baseCount.longValue() + after.size();
        BigDecimal totalSum = baseSum.add(sumOf(after));
        BigDecimal avg = newCount == 0
                ? BigDecimal.ZERO
                : totalSum.divide(BigDecimal.valueOf(newCount), 2, RoundingMode.HALF_UP);

        contentGateway.updateRating(contentId, avg, newCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("avg", avg);
        result.put("count", newCount);
        result.put("mine", mine(after, userId));
        return result;
    }

    private List<Rate> rates(Long contentId) {
        return rateMapper.selectList(new LambdaQueryWrapper<Rate>()
                .eq(Rate::getContentId, contentId));
    }

    private BigDecimal sumOf(List<Rate> rates) {
        return rates.stream()
                .map(rate -> BigDecimal.valueOf(rate.getScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Integer mine(List<Rate> rates, Long userId) {
        return rates.stream()
                .filter(rate -> userId != null && userId.equals(rate.getUserId()))
                .map(Rate::getScore)
                .findFirst()
                .orElse(null);
    }

    public List<Long> contentIds(Long userId) {
        return rateMapper.selectList(new LambdaQueryWrapper<Rate>()
                        .eq(Rate::getUserId, userId))
                .stream().map(Rate::getContentId).toList();
    }
}
