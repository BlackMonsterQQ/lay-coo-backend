package com.laycoo.tourism.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laycoo.tourism.common.api.PageResult;
import com.laycoo.tourism.common.dto.ContentCard;
import com.laycoo.tourism.common.exception.BusinessException;
import com.laycoo.tourism.content.dto.AuditRequest;
import com.laycoo.tourism.content.dto.ContentDetailVO;
import com.laycoo.tourism.content.dto.ContentQuery;
import com.laycoo.tourism.content.dto.ContentSaveRequest;
import com.laycoo.tourism.content.dto.InternalCreateRequest;
import com.laycoo.tourism.content.dto.PlanContextItem;
import com.laycoo.tourism.content.dto.StatsRequest;
import com.laycoo.tourism.content.entity.Category;
import com.laycoo.tourism.content.entity.Content;
import com.laycoo.tourism.content.entity.ContentTag;
import com.laycoo.tourism.content.entity.Tag;
import com.laycoo.tourism.content.mapper.CategoryMapper;
import com.laycoo.tourism.content.mapper.ContentMapper;
import com.laycoo.tourism.content.mapper.ContentTagMapper;
import com.laycoo.tourism.content.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_REJECTED = "REJECTED";

    private static final String HOT_ORDER = "(COALESCE(view_count,0) + COALESCE(favorite_count,0) * 5"
            + " + COALESCE(like_count,0) * 3 + COALESCE(comment_count,0) * 4)";

    private final ContentMapper contentMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final ContentTagMapper contentTagMapper;
    private final ObjectMapper objectMapper;

    // ---------------------------------------------------------------- 查询

    public List<Category> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort));
    }

    public PageResult<ContentCard> list(ContentQuery query, boolean admin) {
        QueryWrapper<Content> wrapper = buildWrapper(query, admin);
        long current = Math.max(query.getPage(), 1);
        long size = Math.min(Math.max(query.getSize(), 1), 100);
        Page<Content> page = new Page<>(current, size);
        contentMapper.selectPage(page, wrapper);
        return PageResult.of(toCards(page.getRecords()), page.getTotal(), current, size);
    }

    private QueryWrapper<Content> buildWrapper(ContentQuery query, boolean admin) {
        QueryWrapper<Content> wrapper = new QueryWrapper<>();
        if (admin) {
            if (StringUtils.hasText(query.getStatus())) {
                wrapper.eq("status", query.getStatus());
            }
        } else {
            wrapper.eq("status", STATUS_PUBLISHED);
        }
        if (StringUtils.hasText(query.getSource())) {
            wrapper.eq("source", query.getSource());
        }
        if (query.getCategoryId() != null) {
            wrapper.eq("category_id", query.getCategoryId());
        }
        if (StringUtils.hasText(query.getCategoryCode())) {
            Category category = categoryByCode(query.getCategoryCode());
            if (category == null) {
                wrapper.eq("id", -1L);
            } else {
                wrapper.eq("category_id", category.getId());
            }
        }
        if (StringUtils.hasText(query.getType())) {
            List<Long> ids = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                            .eq(Category::getType, query.getType()))
                    .stream().map(Category::getId).toList();
            if (ids.isEmpty()) {
                wrapper.eq("id", -1L);
            } else {
                wrapper.in("category_id", ids);
            }
        }
        if (StringUtils.hasText(query.getArea())) {
            wrapper.eq("area", query.getArea());
        }
        if (StringUtils.hasText(query.getCityCode())) {
            wrapper.eq("city_code", query.getCityCode());
        }
        if (StringUtils.hasText(query.getTag())) {
            wrapper.exists("SELECT 1 FROM content_tag ct JOIN tag t ON ct.tag_id = t.id"
                    + " WHERE ct.content_id = content.id AND t.name = {0}", query.getTag());
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like("title", keyword)
                    .or().like("summary", keyword)
                    .or().like("body", keyword)
                    .or().like("address", keyword)
                    .or().like("area", keyword));
        }
        if ("new".equalsIgnoreCase(query.getSort())) {
            wrapper.orderByDesc("publish_time").orderByDesc("id");
        } else {
            wrapper.orderByDesc(HOT_ORDER).orderByDesc("publish_time");
        }
        return wrapper;
    }

    public ContentDetailVO detail(Long id, boolean countView) {
        Content content = contentMapper.selectById(id);
        if (content == null || !STATUS_PUBLISHED.equals(content.getStatus())) {
            throw BusinessException.notFound("内容不存在或未发布");
        }
        if (countView) {
            contentMapper.update(null, new UpdateWrapper<Content>()
                    .eq("id", id)
                    .setSql("view_count = COALESCE(view_count, 0) + 1"));
            content.setViewCount(nvl(content.getViewCount()) + 1);
        }
        return toDetail(content);
    }

    public ContentDetailVO adminDetail(Long id) {
        Content content = contentMapper.selectById(id);
        if (content == null) {
            throw BusinessException.notFound("内容不存在");
        }
        return toDetail(content);
    }

    public List<ContentCard> related(Long id, int size) {
        Content content = contentMapper.selectById(id);
        if (content == null) {
            return List.of();
        }
        List<Content> list = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, STATUS_PUBLISHED)
                .eq(Content::getCategoryId, content.getCategoryId())
                .ne(Content::getId, id)
                .last("ORDER BY " + HOT_ORDER + " DESC, publish_time DESC LIMIT " + Math.max(1, size)));
        return toCards(list);
    }

    // ---------------------------------------------------------------- 管理端

    public Long createManual(ContentSaveRequest request) {
        Content content = new Content();
        applyRequest(content, request);
        content.setSource("MANUAL");
        content.setStatus(STATUS_PUBLISHED);
        content.setPublishTime(LocalDateTime.now());
        initCounters(content);
        content.setCreateTime(LocalDateTime.now());
        content.setUpdateTime(LocalDateTime.now());
        content.setDeleted(0);
        contentMapper.insert(content);
        saveTags(content.getId(), request.getTags());
        return content.getId();
    }

    public Long createFromAi(InternalCreateRequest request) {
        Content content = new Content();
        applyRequest(content, request);
        if (!StringUtils.hasText(content.getSummary())) {
            content.setSummary(truncate(content.getBody(), 120));
        }
        content.setSource("AI");
        content.setStatus(STATUS_PENDING);
        content.setAiTaskId(request.getAiTaskId());
        initCounters(content);
        content.setCreateTime(LocalDateTime.now());
        content.setUpdateTime(LocalDateTime.now());
        content.setDeleted(0);
        contentMapper.insert(content);
        saveTags(content.getId(), request.getTags());
        return content.getId();
    }

    public void update(Long id, ContentSaveRequest request) {
        Content content = requireContent(id);
        applyRequest(content, request);
        content.setUpdateTime(LocalDateTime.now());
        contentMapper.updateById(content);
        saveTags(id, request.getTags());
    }

    public void audit(AuditRequest request) {
        Content content = requireContent(request.getId());
        String action = request.getAction() == null ? "" : request.getAction().toUpperCase();
        switch (action) {
            case "APPROVE" -> {
                content.setStatus(STATUS_PUBLISHED);
                content.setAuditRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark() : "审核通过");
                if (content.getPublishTime() == null) {
                    content.setPublishTime(LocalDateTime.now());
                }
            }
            case "REJECT" -> {
                content.setStatus(STATUS_REJECTED);
                content.setAuditRemark(StringUtils.hasText(request.getRemark()) ? request.getRemark() : "内容不符合发布规范");
            }
            default -> throw new BusinessException("不支持的审核动作: " + request.getAction());
        }
        content.setUpdateTime(LocalDateTime.now());
        contentMapper.updateById(content);
    }

    public void delete(Long id) {
        contentMapper.deleteById(id);
    }

    public Map<String, Object> adminStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", contentMapper.selectCount(null));
        stats.put("published", contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, STATUS_PUBLISHED)));
        stats.put("pending", contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, STATUS_PENDING)));
        stats.put("rejected", contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, STATUS_REJECTED)));

        List<Map<String, Object>> byCategory = new ArrayList<>();
        for (Category category : categories()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", category.getName());
            item.put("code", category.getCode());
            item.put("count", contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                    .eq(Content::getCategoryId, category.getId())));
            byCategory.add(item);
        }
        stats.put("byCategory", byCategory);

        Object views = contentMapper.selectObjs(new QueryWrapper<Content>()
                        .select("COALESCE(SUM(view_count), 0)"))
                .stream().findFirst().orElse(0);
        stats.put("totalViews", views);
        return stats;
    }

    // ---------------------------------------------------------------- 服务间

    public List<ContentCard> cardsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Content> contents = contentMapper.selectBatchIds(ids);
        Map<Long, Content> map = contents.stream()
                .collect(Collectors.toMap(Content::getId, Function.identity(), (a, b) -> a));
        List<Content> ordered = ids.stream().map(map::get).filter(java.util.Objects::nonNull).toList();
        return toCards(ordered);
    }

    public List<ContentCard> candidates(int limit) {
        int size = Math.min(Math.max(limit, 1), 500);
        List<Content> list = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, STATUS_PUBLISHED)
                .last("ORDER BY " + HOT_ORDER + " DESC, publish_time DESC LIMIT " + size));
        return toCards(list);
    }

    /**
     * 给 AI 行程规划提供的精简内容上下文。
     * 只返回已发布内容，按偏好匹配度和热度排序。
     */
    public List<PlanContextItem> planContext(String cityCode, List<String> preferences, int limit) {
        int size = Math.min(Math.max(limit, 1), 60);
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, STATUS_PUBLISHED);
        if (StringUtils.hasText(cityCode)) {
            wrapper.eq(Content::getCityCode, cityCode);
        }
        wrapper.last("ORDER BY " + HOT_ORDER + " DESC, publish_time DESC LIMIT 240");
        List<Content> contents = contentMapper.selectList(wrapper);
        if (contents.isEmpty()) {
            return List.of();
        }
        Map<Long, Category> categories = categories().stream()
                .collect(Collectors.toMap(Category::getId, Function.identity(), (a, b) -> a));
        Map<Long, List<String>> tags = loadTags(contents.stream().map(Content::getId).toList());
        List<String> prefs = preferences == null ? List.of() : preferences.stream()
                .filter(StringUtils::hasText).map(String::trim).toList();

        return contents.stream()
                .map(content -> {
                    Category category = categories.get(content.getCategoryId());
                    List<String> contentTags = tags.getOrDefault(content.getId(), List.of());
                    double score = preferenceScore(category, contentTags, prefs)
                            + (content.getRating() == null ? 0 : content.getRating().doubleValue()) * 0.5;
                    PlanContextItem item = new PlanContextItem();
                    item.setId(content.getId());
                    item.setTitle(content.getTitle());
                    item.setCategoryName(category == null ? "" : category.getName());
                    item.setCategoryCode(category == null ? "" : category.getCode());
                    item.setCityCode(content.getCityCode());
                    item.setArea(content.getArea());
                    item.setSummary(content.getSummary());
                    item.setTags(contentTags);
                    item.setOpeningHours(content.getOpeningHours());
                    item.setPriceRange(content.getPriceRange());
                    item.setRating(content.getRating());
                    return Map.entry(score, item);
                })
                .sorted(Map.Entry.<Double, PlanContextItem>comparingByKey().reversed())
                .limit(size)
                .map(Map.Entry::getValue)
                .toList();
    }

    private double preferenceScore(Category category, List<String> tags, List<String> preferences) {
        if (preferences == null || preferences.isEmpty()) {
            return 0;
        }
        double score = 0;
        for (String preference : preferences) {
            if (tags.stream().anyMatch(tag -> tag.contains(preference) || preference.contains(tag))) {
                score += 2;
            }
            if (category != null && (category.getName().contains(preference)
                    || preference.contains(category.getName()))) {
                score += 2;
            }
            if (category != null && "美食".equals(preference) && "FOOD".equals(category.getCode())) {
                score += 2;
            }
            if (category != null && "人文历史".equals(preference)
                    && ("HUMANITY".equals(category.getType()))) {
                score += 1;
            }
            if (category != null && "自然风光".equals(preference)
                    && ("SPOT".equals(category.getCode()) || "PLAY".equals(category.getCode()))) {
                score += 1;
            }
        }
        return score;
    }

    public void applyStats(Long id, StatsRequest request) {
        Content content = contentMapper.selectById(id);
        if (content == null) {
            return;
        }
        String type = request.getType() == null ? "" : request.getType();
        if ("rating".equals(type)) {
            contentMapper.update(null, new UpdateWrapper<Content>()
                    .eq("id", id)
                    .set("rating", request.getRatingAvg() == null ? BigDecimal.ZERO : request.getRatingAvg())
                    .set("rating_count", request.getRatingCount() == null ? 0 : request.getRatingCount())
                    .set("update_time", LocalDateTime.now()));
            return;
        }
        String column = switch (type) {
            case "view" -> "view_count";
            case "favorite" -> "favorite_count";
            case "like" -> "like_count";
            case "comment" -> "comment_count";
            default -> null;
        };
        if (column == null) {
            throw new BusinessException("不支持的统计类型: " + type);
        }
        long delta = request.getDelta();
        contentMapper.update(null, new UpdateWrapper<Content>()
                .eq("id", id)
                .setSql(column + " = GREATEST(COALESCE(" + column + ", 0) + (" + delta + "), 0)"));
    }

    public List<String> areas() {
        return contentMapper.selectObjs(new QueryWrapper<Content>()
                        .select("DISTINCT area")
                        .eq("status", STATUS_PUBLISHED)
                        .isNotNull("area")
                        .ne("area", ""))
                .stream().map(String::valueOf).sorted().toList();
    }

    public List<Map<String, Object>> hotTags(int limit) {
        Map<Long, Long> counts = contentTagMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(ContentTag::getTagId, Collectors.counting()));
        Map<Long, Tag> tags = tagMapper.selectList(null).stream()
                .collect(Collectors.toMap(Tag::getId, Function.identity(), (a, b) -> a));
        return counts.entrySet().stream()
                .filter(e -> tags.containsKey(e.getKey()))
                .sorted(Map.Entry.<Long, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(Math.max(1, limit))
                .map(e -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", e.getKey());
                    item.put("name", tags.get(e.getKey()).getName());
                    item.put("count", e.getValue());
                    return item;
                })
                .toList();
    }

    public List<String> hotKeywords() {
        return List.of("西湖", "龙井茶", "宋韵", "非遗", "夜游", "杭帮菜", "良渚", "citywalk");
    }

    // ---------------------------------------------------------------- 内部工具

    private Content requireContent(Long id) {
        Content content = contentMapper.selectById(id);
        if (content == null) {
            throw BusinessException.notFound("内容不存在");
        }
        return content;
    }

    private Category categoryByCode(String code) {
        return categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getCode, code)
                .last("limit 1"));
    }

    private void applyRequest(Content content, ContentSaveRequest request) {
        Category category = categoryByCode(request.getCategoryCode());
        if (category == null) {
            throw new BusinessException("分类不存在: " + request.getCategoryCode());
        }
        content.setTitle(request.getTitle().trim());
        content.setSummary(request.getSummary());
        content.setCategoryId(category.getId());
        content.setArea(request.getArea());
        content.setCityCode(StringUtils.hasText(request.getCityCode()) ? request.getCityCode() : "hangzhou");
        content.setAddress(request.getAddress());
        content.setBody(request.getBody());
        content.setHighlights(writeHighlights(request.getHighlights()));
        content.setTips(request.getTips());
        content.setOpeningHours(request.getOpeningHours());
        content.setPriceRange(request.getPriceRange());
        content.setCoverDesc(request.getCoverDesc());
    }

    private void initCounters(Content content) {
        content.setViewCount(0L);
        content.setFavoriteCount(0L);
        content.setLikeCount(0L);
        content.setCommentCount(0L);
        content.setRating(BigDecimal.ZERO);
        content.setRatingCount(0L);
    }

    private void saveTags(Long contentId, List<String> tags) {
        contentTagMapper.delete(new LambdaQueryWrapper<ContentTag>()
                .eq(ContentTag::getContentId, contentId));
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

    private String writeHighlights(List<String> highlights) {
        if (highlights == null || highlights.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(highlights);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> readHighlights(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<ContentCard> toCards(List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return List.of();
        }
        Map<Long, Category> categories = categories().stream()
                .collect(Collectors.toMap(Category::getId, Function.identity(), (a, b) -> a));
        Map<Long, List<String>> tags = loadTags(contents.stream().map(Content::getId).toList());
        List<ContentCard> cards = new ArrayList<>(contents.size());
        for (Content content : contents) {
            ContentCard card = new ContentCard();
            card.setId(content.getId());
            card.setTitle(content.getTitle());
            card.setSummary(content.getSummary());
            card.setCategoryId(content.getCategoryId());
            Category category = categories.get(content.getCategoryId());
            if (category != null) {
                card.setCategoryName(category.getName());
                card.setCategoryCode(category.getCode());
                card.setCategoryType(category.getType());
                card.setCategoryIcon(category.getIcon());
            }
            card.setArea(content.getArea());
            card.setAddress(content.getAddress());
            card.setCoverDesc(content.getCoverDesc());
            card.setSource(content.getSource());
            card.setStatus(content.getStatus());
            card.setViewCount(nvl(content.getViewCount()));
            card.setFavoriteCount(nvl(content.getFavoriteCount()));
            card.setLikeCount(nvl(content.getLikeCount()));
            card.setCommentCount(nvl(content.getCommentCount()));
            card.setRating(content.getRating() == null ? BigDecimal.ZERO : content.getRating());
            card.setRatingCount(nvl(content.getRatingCount()));
            card.setPublishTime(content.getPublishTime());
            card.setCreateTime(content.getCreateTime());
            card.setTags(tags.getOrDefault(content.getId(), List.of()));
            cards.add(card);
        }
        return cards;
    }

    private ContentDetailVO toDetail(Content content) {
        ContentDetailVO vo = new ContentDetailVO();
        vo.setId(content.getId());
        vo.setTitle(content.getTitle());
        vo.setSummary(content.getSummary());
        vo.setCategoryId(content.getCategoryId());
        Category category = content.getCategoryId() == null ? null : categoryMapper.selectById(content.getCategoryId());
        if (category != null) {
            vo.setCategoryName(category.getName());
            vo.setCategoryCode(category.getCode());
            vo.setCategoryType(category.getType());
            vo.setCategoryIcon(category.getIcon());
        }
        vo.setArea(content.getArea());
        vo.setAddress(content.getAddress());
        vo.setBody(content.getBody());
        vo.setHighlights(readHighlights(content.getHighlights()));
        vo.setTips(content.getTips());
        vo.setOpeningHours(content.getOpeningHours());
        vo.setPriceRange(content.getPriceRange());
        vo.setCoverDesc(content.getCoverDesc());
        vo.setSource(content.getSource());
        vo.setStatus(content.getStatus());
        vo.setAuditRemark(content.getAuditRemark());
        vo.setAiTaskId(content.getAiTaskId());
        vo.setViewCount(nvl(content.getViewCount()));
        vo.setFavoriteCount(nvl(content.getFavoriteCount()));
        vo.setLikeCount(nvl(content.getLikeCount()));
        vo.setCommentCount(nvl(content.getCommentCount()));
        vo.setRating(content.getRating() == null ? BigDecimal.ZERO : content.getRating());
        vo.setRatingCount(nvl(content.getRatingCount()));
        vo.setPublishTime(content.getPublishTime());
        vo.setCreateTime(content.getCreateTime());
        vo.setTags(loadTags(List.of(content.getId())).getOrDefault(content.getId(), List.of()));
        return vo;
    }

    private Map<Long, List<String>> loadTags(List<Long> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return Map.of();
        }
        List<ContentTag> relations = contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>()
                .in(ContentTag::getContentId, contentIds));
        if (relations.isEmpty()) {
            return Map.of();
        }
        Set<Long> tagIds = relations.stream().map(ContentTag::getTagId).collect(Collectors.toSet());
        Map<Long, String> names = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName, (a, b) -> a));
        Map<Long, List<String>> result = new HashMap<>();
        for (ContentTag relation : relations) {
            String name = names.get(relation.getTagId());
            if (name != null) {
                result.computeIfAbsent(relation.getContentId(), k -> new ArrayList<>()).add(name);
            }
        }
        return result;
    }

    private static long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        String plain = text.replaceAll("[#>*`\\-]", " ").replaceAll("\\s+", " ").trim();
        return plain.length() <= max ? plain : plain.substring(0, max) + "…";
    }
}
