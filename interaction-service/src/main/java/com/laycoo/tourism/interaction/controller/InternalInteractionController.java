package com.laycoo.tourism.interaction.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.interaction.service.ContentGateway;
import com.laycoo.tourism.interaction.service.FavoriteService;
import com.laycoo.tourism.interaction.service.LikeService;
import com.laycoo.tourism.interaction.service.RateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户兴趣画像，供推荐服务内部调用。
 */
@RestController
@RequestMapping("/internal/interaction")
@RequiredArgsConstructor
public class InternalInteractionController {

    private final FavoriteService favoriteService;
    private final LikeService likeService;
    private final RateService rateService;
    private final ContentGateway contentGateway;

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile(@RequestParam Long userId) {
        List<Long> favoriteIds = favoriteService.contentIds(userId);
        List<Long> likedIds = likeService.contentIds(userId);
        List<Long> ratedIds = rateService.contentIds(userId);

        Set<Long> all = new LinkedHashSet<>();
        all.addAll(favoriteIds);
        all.addAll(likedIds);
        all.addAll(ratedIds);

        Map<String, Integer> categoryWeights = new HashMap<>();
        Map<String, Integer> tagWeights = new HashMap<>();
        for (ContentCard card : contentGateway.batch(all)) {
            if (card.getCategoryCode() != null) {
                categoryWeights.merge(card.getCategoryCode(), 1, Integer::sum);
            }
            if (card.getTags() != null) {
                for (String tag : card.getTags()) {
                    tagWeights.merge(tag, 1, Integer::sum);
                }
            }
        }

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("favoriteIds", favoriteIds);
        profile.put("likedIds", likedIds);
        profile.put("ratedIds", ratedIds);
        profile.put("categoryCodes", topKeys(categoryWeights, 3));
        profile.put("tags", topKeys(tagWeights, 5));
        return ApiResponse.ok(profile);
    }

    private List<String> topKeys(Map<String, Integer> weights, int limit) {
        List<String> keys = new ArrayList<>(weights.keySet());
        keys.sort(Comparator.comparingInt((String key) -> weights.getOrDefault(key, 0)).reversed());
        return keys.stream().limit(limit).toList();
    }
}
