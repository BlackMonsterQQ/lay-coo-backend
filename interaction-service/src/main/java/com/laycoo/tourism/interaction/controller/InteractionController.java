package com.laycoo.tourism.interaction.controller;

import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.common.security.UserContext;
import com.laycoo.tourism.interaction.service.CommentService;
import com.laycoo.tourism.interaction.service.ContentGateway;
import com.laycoo.tourism.interaction.service.FavoriteService;
import com.laycoo.tourism.interaction.service.LikeService;
import com.laycoo.tourism.interaction.service.RateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/interaction")
@RequiredArgsConstructor
public class InteractionController {

    private final FavoriteService favoriteService;
    private final LikeService likeService;
    private final CommentService commentService;
    private final RateService rateService;
    private final ContentGateway contentGateway;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats(@RequestParam Long contentId) {
        Long userId = UserContext.getUserId();
        // 总数使用内容服务维护的聚合值（含运营初始数据），个人状态使用互动库
        ContentCard card = contentGateway.require(contentId);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("favoriteCount", card.getFavoriteCount() == null ? 0 : card.getFavoriteCount());
        stats.put("likeCount", card.getLikeCount() == null ? 0 : card.getLikeCount());
        stats.put("commentCount", card.getCommentCount() == null ? 0 : card.getCommentCount());
        stats.put("rating", card.getRating() == null ? 0 : card.getRating());
        stats.put("ratingCount", card.getRatingCount() == null ? 0 : card.getRatingCount());
        stats.put("favorited", userId != null && favoriteService.status(userId, contentId));
        stats.put("liked", userId != null && likeService.status(userId, contentId));
        stats.put("myRating", rateService.summary(contentId, userId).get("mine"));
        return ApiResponse.ok(stats);
    }
}
