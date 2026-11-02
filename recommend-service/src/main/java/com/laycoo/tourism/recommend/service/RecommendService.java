package com.laycoo.tourism.recommend.service;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.common.exception.BusinessException;
import com.laycoo.tourism.recommend.dto.RecommendItem;
import com.laycoo.tourism.recommend.entity.RecommendLog;
import com.laycoo.tourism.recommend.feign.ContentClient;
import com.laycoo.tourism.recommend.feign.InteractionClient;
import com.laycoo.tourism.recommend.mapper.RecommendLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基础推荐引擎：召回（内容服务候选集）→ 多因子排序 → 多样性控制 → 推荐理由。
 * 后续可在此扩展协同过滤、向量召回与 AI 生成推荐理由。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendService {

    private static final int CANDIDATE_LIMIT = 300;

    private final ContentClient contentClient;
    private final InteractionClient interactionClient;
    private final RecommendLogMapper recommendLogMapper;

    public List<RecommendItem> home(Long userId, int size) {
        List<ContentCard> candidates = candidates();
        if (candidates.isEmpty()) {
            return List.of();
        }
        int limit = clamp(size, 1, 24);
        Profile profile = userId == null ? Profile.empty() : loadProfile(userId);

        double maxHot = candidates.stream().mapToDouble(this::hotScore).max().orElse(1D);
        List<RecommendItem> scored = new ArrayList<>();
        for (ContentCard card : candidates) {
            double hotNorm = hotScore(card) / maxHot;
            double fresh = freshness(card);
            double ratingNorm = rating(card) / 5D;
            double affinity = affinity(card, profile);
            double score;
            if (profile.loggedIn()) {
                score = 0.50 * affinity + 0.25 * hotNorm + 0.15 * fresh + 0.10 * ratingNorm;
            } else {
                score = 0.60 * hotNorm + 0.25 * fresh + 0.15 * ratingNorm;
            }
            scored.add(new RecommendItem(card, reason(card, profile, hotNorm, fresh), round(score), "HOME"));
        }
        scored.sort(Comparator.comparingDouble(RecommendItem::getScore).reversed());

        List<RecommendItem> picked = diversify(scored, limit);
        saveLog(userId, picked, "HOME", profile.loggedIn() ? "RULE_AFFINITY_HOT" : "RULE_HOT_FRESH");
        return picked;
    }

    public List<RecommendItem> hot(int limit) {
        List<ContentCard> candidates = candidates();
        int size = clamp(limit, 1, 30);
        List<ContentCard> sorted = candidates.stream()
                .sorted(Comparator.comparingDouble(this::hotScore).reversed())
                .limit(size)
                .toList();
        List<RecommendItem> items = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            ContentCard card = sorted.get(i);
            String reason = i < 3 ? "热度榜 TOP" + (i + 1) : "本周本地热门";
            items.add(new RecommendItem(card, reason, round(hotScore(card)), "HOT"));
        }
        saveLog(null, items, "HOT", "HOT_RANK");
        return items;
    }

    public List<RecommendItem> similar(Long contentId, int size) {
        List<ContentCard> candidates = candidates();
        ContentCard target = candidates.stream()
                .filter(card -> card.getId().equals(contentId))
                .findFirst()
                .orElse(null);
        if (target == null) {
            return List.of();
        }
        Set<String> targetTags = new HashSet<>(target.getTags() == null ? List.of() : target.getTags());
        double maxHot = candidates.stream().mapToDouble(this::hotScore).max().orElse(1D);
        List<RecommendItem> items = new ArrayList<>();
        for (ContentCard card : candidates) {
            if (card.getId().equals(contentId)) {
                continue;
            }
            int shared = 0;
            if (card.getTags() != null) {
                for (String tag : card.getTags()) {
                    if (targetTags.contains(tag)) {
                        shared++;
                    }
                }
            }
            boolean sameCategory = card.getCategoryCode() != null
                    && card.getCategoryCode().equals(target.getCategoryCode());
            double score = Math.min(shared, 3) * 0.22
                    + (sameCategory ? 0.34 : 0)
                    + 0.24 * (hotScore(card) / maxHot);
            if (score <= 0.05) {
                continue;
            }
            items.add(new RecommendItem(card, similarReason(target, card, shared), round(score), "SIMILAR"));
        }
        items.sort(Comparator.comparingDouble(RecommendItem::getScore).reversed());
        List<RecommendItem> result = items.stream().limit(clamp(size, 1, 12)).toList();
        saveLog(null, result, "SIMILAR", "SIMILAR_TAG");
        return result;
    }

    // ---------------------------------------------------------------- 推荐因子

    private List<ContentCard> candidates() {
        try {
            ApiResponse<List<ContentCard>> response = contentClient.candidates(CANDIDATE_LIMIT);
            if (response != null && response.getCode() == 0 && response.getData() != null) {
                return response.getData();
            }
            log.warn("内容服务候选集返回异常: {}", response == null ? "null" : response.getMsg());
        } catch (Exception e) {
            log.error("获取推荐候选集失败", e);
        }
        throw new BusinessException(503, "推荐服务暂时不可用，请稍后再试");
    }

    @SuppressWarnings("unchecked")
    private Profile loadProfile(Long userId) {
        try {
            ApiResponse<Map<String, Object>> response = interactionClient.profile(userId);
            if (response == null || response.getCode() != 0 || response.getData() == null) {
                return new Profile(true, List.of(), List.of(), Set.of(), Set.of());
            }
            Map<String, Object> data = response.getData();
            List<String> categories = (List<String>) data.getOrDefault("categoryCodes", List.of());
            List<String> tags = (List<String>) data.getOrDefault("tags", List.of());
            Set<Long> favoriteIds = toIdSet(data.get("favoriteIds"));
            Set<Long> likedIds = toIdSet(data.get("likedIds"));
            return new Profile(true, categories, tags, favoriteIds, likedIds);
        } catch (Exception e) {
            log.warn("获取用户画像失败 userId={}: {}", userId, e.getMessage());
            return new Profile(true, List.of(), List.of(), Set.of(), Set.of());
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Long> toIdSet(Object value) {
        Set<Long> ids = new LinkedHashSet<>();
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Number number) {
                    ids.add(number.longValue());
                }
            }
        }
        return ids;
    }

    private double affinity(ContentCard card, Profile profile) {
        if (!profile.loggedIn()) {
            return 0;
        }
        double score = 0;
        if (card.getCategoryCode() != null && profile.categoryCodes().contains(card.getCategoryCode())) {
            score += 0.55;
        }
        if (card.getTags() != null) {
            for (String tag : card.getTags()) {
                if (profile.tags().contains(tag)) {
                    score += 0.35;
                }
            }
        }
        if (profile.favoriteIds().contains(card.getId())) {
            score -= 0.15;
        }
        return Math.max(0, Math.min(score, 1.2)) / 1.2;
    }

    private double hotScore(ContentCard card) {
        return value(card.getViewCount())
                + value(card.getFavoriteCount()) * 5
                + value(card.getLikeCount()) * 3
                + value(card.getCommentCount()) * 4
                + rating(card) * 40;
    }

    private double freshness(ContentCard card) {
        LocalDateTime publishTime = card.getPublishTime();
        if (publishTime == null) {
            return 0.2;
        }
        double days = Math.max(0, Duration.between(publishTime, LocalDateTime.now()).toMinutes() / 1440D);
        return Math.exp(-days / 45D);
    }

    private double rating(ContentCard card) {
        return card.getRating() == null ? 0 : card.getRating().doubleValue();
    }

    private long value(Long number) {
        return number == null ? 0 : number;
    }

    // ---------------------------------------------------------------- 理由与多样性

    private String reason(ContentCard card, Profile profile, double hotNorm, double fresh) {
        if (profile.loggedIn() && card.getTags() != null) {
            for (String tag : card.getTags()) {
                if (profile.tags().contains(tag)) {
                    return "因为你关注「" + tag + "」";
                }
            }
        }
        if (profile.loggedIn() && card.getCategoryCode() != null
                && profile.categoryCodes().contains(card.getCategoryCode())) {
            return "你偏爱的" + card.getCategoryName();
        }
        if (hotNorm >= 0.65) {
            return "本地热门 · " + value(card.getViewCount()) + " 人浏览";
        }
        if (fresh >= 0.8) {
            return "新近发布 · 先睹为快";
        }
        if (rating(card) >= 4.7) {
            return "高分口碑 · 评分 " + card.getRating();
        }
        return "编辑精选 · 值得一去";
    }

    private String similarReason(ContentCard target, ContentCard card, int shared) {
        if (shared > 0) {
            return "与「" + target.getTitle() + "」同频";
        }
        if (target.getCategoryCode() != null && target.getCategoryCode().equals(card.getCategoryCode())) {
            return "同属" + target.getCategoryName();
        }
        return "本地人也常去";
    }

    private List<RecommendItem> diversify(List<RecommendItem> scored, int limit) {
        List<RecommendItem> picked = new ArrayList<>();
        Map<String, Integer> categoryCount = new HashMap<>();
        for (RecommendItem item : scored) {
            if (picked.size() >= limit) {
                break;
            }
            String code = item.getContent().getCategoryCode() == null ? "UNKNOWN" : item.getContent().getCategoryCode();
            int used = categoryCount.getOrDefault(code, 0);
            if (used >= 2) {
                continue;
            }
            categoryCount.put(code, used + 1);
            picked.add(item);
        }
        if (picked.size() < limit) {
            for (RecommendItem item : scored) {
                if (picked.size() >= limit) {
                    break;
                }
                if (!picked.contains(item)) {
                    picked.add(item);
                }
            }
        }
        return picked;
    }

    private void saveLog(Long userId, List<RecommendItem> items, String scene, String strategy) {
        try {
            LocalDateTime now = LocalDateTime.now();
            for (RecommendItem item : items) {
                RecommendLog log = new RecommendLog();
                log.setUserId(userId);
                log.setContentId(item.getContent().getId());
                log.setScene(scene);
                log.setStrategy(strategy);
                log.setScore(item.getScore());
                log.setReason(item.getReason());
                log.setCreateTime(now);
                recommendLogMapper.insert(log);
            }
        } catch (Exception e) {
            log.warn("记录推荐日志失败: {}", e.getMessage());
        }
    }

    private static double round(double value) {
        return Math.round(value * 10000D) / 10000D;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private record Profile(boolean loggedIn, List<String> categoryCodes, List<String> tags,
                           Set<Long> favoriteIds, Set<Long> likedIds) {
        static Profile empty() {
            return new Profile(false, List.of(), List.of(), Set.of(), Set.of());
        }
    }
}
