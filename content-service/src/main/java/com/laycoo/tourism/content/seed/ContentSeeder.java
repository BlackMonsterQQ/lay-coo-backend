package com.laycoo.tourism.content.seed;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laycoo.tourism.content.entity.Category;
import com.laycoo.tourism.content.entity.Content;
import com.laycoo.tourism.content.entity.ContentTag;
import com.laycoo.tourism.content.entity.Tag;
import com.laycoo.tourism.content.mapper.CategoryMapper;
import com.laycoo.tourism.content.mapper.ContentMapper;
import com.laycoo.tourism.content.mapper.ContentTagMapper;
import com.laycoo.tourism.content.mapper.TagMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 首次启动时导入分类、标签与演示文旅内容（内容由 AI/运营生成的种子数据模拟）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentSeeder implements ApplicationRunner {

    private final ObjectMapper objectMapper;
    private final CategoryMapper categoryMapper;
    private final ContentMapper contentMapper;
    private final TagMapper tagMapper;
    private final ContentTagMapper contentTagMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        SeedFile seedFile;
        try (InputStream inputStream = new ClassPathResource("seed/content-seed.json").getInputStream()) {
            seedFile = objectMapper.readValue(inputStream, SeedFile.class);
        }

        if (categoryMapper.selectCount(null) == 0) {
            for (Category category : seedFile.getCategories()) {
                categoryMapper.insert(category);
            }
            log.info("导入分类 {} 个", seedFile.getCategories().size());
        }
        if (contentMapper.selectCount(null) > 0) {
            return;
        }

        Map<String, Category> categoryMap = new HashMap<>();
        for (Category category : categoryMapper.selectList(null)) {
            categoryMap.put(category.getCode(), category);
        }

        int count = 0;
        for (SeedContent seed : seedFile.getContents()) {
            Category category = categoryMap.get(seed.getCategoryCode());
            if (category == null) {
                continue;
            }
            Content content = new Content();
            content.setTitle(seed.getTitle());
            content.setSummary(seed.getSummary());
            content.setCategoryId(category.getId());
            content.setArea(seed.getArea());
            content.setAddress(seed.getAddress());
            content.setBody(seed.getBody());
            content.setHighlights(objectMapper.writeValueAsString(seed.getHighlights()));
            content.setTips(seed.getTips());
            content.setOpeningHours(seed.getOpeningHours());
            content.setPriceRange(seed.getPriceRange());
            content.setCoverDesc(seed.getCoverDesc());
            content.setSource(seed.getSource());
            content.setStatus(seed.getStatus());
            content.setViewCount(seed.getViewCount());
            content.setFavoriteCount(seed.getFavoriteCount());
            content.setLikeCount(seed.getLikeCount());
            content.setCommentCount(seed.getCommentCount());
            content.setRating(BigDecimal.valueOf(seed.getRating()).setScale(2, RoundingMode.HALF_UP));
            content.setRatingCount(seed.getRatingCount());
            content.setCreateTime(LocalDateTime.now().minusDays(seed.getDaysAgo() + 3L));
            content.setUpdateTime(LocalDateTime.now().minusDays(Math.max(seed.getDaysAgo() - 1L, 0)));
            content.setPublishTime(LocalDateTime.now().minusDays(seed.getDaysAgo()));
            content.setDeleted(0);
            contentMapper.insert(content);
            saveTags(content.getId(), seed.getTags());
            count++;
        }
        log.info("导入演示内容 {} 条", count);
    }

    private void saveTags(Long contentId, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return;
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String tag : tags) {
            if (StringUtils.hasText(tag)) {
                distinct.add(tag.trim());
            }
        }
        for (String name : distinct) {
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getName, name)
                    .last("limit 1"));
            if (tag == null) {
                tag = new Tag();
                tag.setName(name);
                tagMapper.insert(tag);
            }
            contentTagMapper.insert(new ContentTag(contentId, tag.getId()));
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeedFile {
        private List<Category> categories;
        private List<SeedContent> contents;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeedContent {
        private String title;
        private String summary;
        private String categoryCode;
        private String area;
        private String address;
        private String body;
        private List<String> highlights;
        private String tips;
        private String openingHours;
        private String priceRange;
        private String coverDesc;
        private List<String> tags;
        private long viewCount;
        private long favoriteCount;
        private long likeCount;
        private long commentCount;
        private double rating;
        private long ratingCount;
        private String source = "MANUAL";
        private String status = "PUBLISHED";
        private int daysAgo = 30;
    }
}
