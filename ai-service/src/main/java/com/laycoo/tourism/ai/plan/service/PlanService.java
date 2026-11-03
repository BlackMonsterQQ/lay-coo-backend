package com.laycoo.tourism.ai.plan.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.laycoo.tourism.ai.config.AiProperties;
import com.laycoo.tourism.ai.feign.ContentClient;
import com.laycoo.tourism.ai.plan.dto.CityInfo;
import com.laycoo.tourism.ai.plan.dto.PlanDetailResponse;
import com.laycoo.tourism.ai.plan.dto.PlanGenerateRequest;
import com.laycoo.tourism.ai.plan.dto.PlanGenerateResponse;
import com.laycoo.tourism.ai.plan.dto.PlanSummaryResponse;
import com.laycoo.tourism.ai.plan.entity.TravelPlan;
import com.laycoo.tourism.ai.plan.mapper.TravelPlanMapper;
import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.api.PageResult;
import com.laycoo.tourism.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String PLAN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final TravelPlanMapper planMapper;
    private final ContentClient contentClient;
    private final AiProperties properties;
    private final PlanRateLimiter rateLimiter;
    private final PlanTaskService planTaskService;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    public PlanGenerateResponse create(PlanGenerateRequest request, Long userId, String ip) {
        CityInfo city = loadCity(request.getCityCode());
        CityInfo origin = loadCity(request.getOriginCityCode());
        if (city.getCode().equals(origin.getCode())) {
            throw new BusinessException(40010, "出发城市不能与目的地相同");
        }
        LocalDate today = LocalDate.now();
        if (request.getStartDate().isBefore(today)) {
            throw new BusinessException(40006, "开始日期不能早于今天");
        }
        if (request.getStartDate().isAfter(today.plusDays(90))) {
            throw new BusinessException(40007, "最多提前 90 天规划");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(40004, "结束日期不能早于开始日期");
        }
        int dayCount = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (dayCount > properties.getMaxDays()) {
            throw new BusinessException(40005, "最多规划 " + properties.getMaxDays() + " 天行程");
        }

        List<String> preferences = normalizePreferences(request.getPreferences());
        if (preferences.size() > 5) {
            throw new BusinessException(40008, "最多选择 5 个偏好");
        }
        String extraNote = request.getExtraNote() == null ? "" : request.getExtraNote().trim();
        if (extraNote.length() > 100) {
            throw new BusinessException(40009, "补充说明最多 100 个字符");
        }
        String transportPreference = normalizeTransport(request.getTransportPreference());

        if (!rateLimiter.allow(userId, ip)) {
            throw new BusinessException(42901, "今日生成次数已用完，请明天再试");
        }

        String hash = hash(city.getCode(), origin.getCode(), transportPreference,
                request.getStartDate(), request.getEndDate(), preferences, extraNote);
        TravelPlan existing = findRecent(hash);
        if (existing != null) {
            return response(existing);
        }

        TravelPlan plan = new TravelPlan();
        plan.setPlanNo(nextPlanNo());
        plan.setUserId(userId);
        plan.setCityCode(city.getCode());
        plan.setCityName(city.getName());
        plan.setOriginCityCode(origin.getCode());
        plan.setOriginCityName(origin.getName());
        plan.setTransportPreference(transportPreference);
        plan.setStartDate(request.getStartDate());
        plan.setEndDate(request.getEndDate());
        plan.setDayCount(dayCount);
        plan.setPreferences(writePreferences(preferences));
        plan.setExtraNote(extraNote);
        plan.setRequestHash(hash);
        plan.setStatus(STATUS_PENDING);
        plan.setProgress(0);
        plan.setCostTokens(0L);
        plan.setCreateTime(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        planMapper.insert(plan);
        planTaskService.runAsync(plan.getId());
        return response(plan);
    }

    public PageResult<PlanSummaryResponse> list(Long userId, long page, long size) {
        if (userId == null) {
            throw new BusinessException(401, "请先登录后查看我的行程");
        }
        long current = Math.max(page, 1);
        long pageSize = Math.min(Math.max(size, 1), 50);
        Page<TravelPlan> result = planMapper.selectPage(new Page<>(current, pageSize),
                new LambdaQueryWrapper<TravelPlan>()
                        .eq(TravelPlan::getUserId, userId)
                        .orderByDesc(TravelPlan::getCreateTime));
        List<PlanSummaryResponse> list = result.getRecords().stream().map(plan -> {
            PlanSummaryResponse item = new PlanSummaryResponse();
            item.setPlanNo(plan.getPlanNo());
            item.setCity(plan.getCityName());
            item.setCityCode(plan.getCityCode());
            item.setOriginCity(plan.getOriginCityName());
            item.setOriginCityCode(plan.getOriginCityCode());
            item.setStartDate(plan.getStartDate());
            item.setEndDate(plan.getEndDate());
            item.setDayCount(plan.getDayCount());
            item.setNights(Math.max(0, plan.getDayCount() - 1));
            item.setStatus(plan.getStatus());
            item.setTitle(plan.getTitle());
            item.setSummary(plan.getSummary());
            item.setTransportPreference(plan.getTransportPreference());
            item.setModel(plan.getModel());
            item.setCreateTime(plan.getCreateTime());
            return item;
        }).toList();
        return PageResult.of(list, result.getTotal(), current, pageSize);
    }

    public PlanDetailResponse detail(String planNo) {
        TravelPlan plan = require(planNo);
        PlanDetailResponse response = new PlanDetailResponse();
        response.setPlanNo(plan.getPlanNo());
        response.setStatus(plan.getStatus());
        response.setCity(plan.getCityName());
        response.setCityCode(plan.getCityCode());
        response.setOriginCity(plan.getOriginCityName());
        response.setOriginCityCode(plan.getOriginCityCode());
        response.setTransportPreference(plan.getTransportPreference());
        response.setStartDate(plan.getStartDate());
        response.setEndDate(plan.getEndDate());
        response.setDayCount(plan.getDayCount());
        response.setNights(Math.max(0, plan.getDayCount() - 1));
        response.setPreferences(readPreferences(plan.getPreferences()));
        response.setTitle(plan.getTitle());
        response.setSummary(plan.getSummary());
        response.setError(plan.getError());
        response.setProgress(plan.getProgress() == null ? 0 : plan.getProgress());
        response.setModel(plan.getModel());
        response.setCreateTime(plan.getCreateTime());
        if (StringUtils.hasText(plan.getContentJson())) {
            try {
                response.setPlan(objectMapper.readTree(plan.getContentJson()));
            } catch (Exception e) {
                log.warn("解析行程 JSON 失败 planNo={}: {}", planNo, e.getMessage());
            }
        }
        return response;
    }

    public PlanDetailResponse retry(String planNo, Long userId, String ip) {
        TravelPlan plan = require(planNo);
        if (!STATUS_FAILED.equals(plan.getStatus())) {
            throw new BusinessException("只有失败的行程可以重试");
        }
        if (!rateLimiter.allow(userId, ip)) {
            throw new BusinessException(42901, "今日生成次数已用完，请明天再试");
        }
        plan.setStatus(STATUS_PENDING);
        plan.setProgress(0);
        plan.setError(null);
        plan.setContentJson(null);
        plan.setTitle(null);
        plan.setSummary(null);
        plan.setUpdateTime(LocalDateTime.now());
        planMapper.updateById(plan);
        planTaskService.runAsync(plan.getId());
        return detail(planNo);
    }

    private CityInfo loadCity(String cityCode) {
        try {
            ApiResponse<CityInfo> response = contentClient.city(cityCode);
            if (response != null && response.getCode() == 0 && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("查询城市失败 code={}: {}", cityCode, e.getMessage());
        }
        throw new BusinessException(40002, "城市不存在或暂未开放");
    }

    private TravelPlan findRecent(String hash) {
        return planMapper.selectOne(new LambdaQueryWrapper<TravelPlan>()
                .eq(TravelPlan::getRequestHash, hash)
                .ge(TravelPlan::getCreateTime, LocalDateTime.now().minusMinutes(10))
                .in(TravelPlan::getStatus, List.of(STATUS_PENDING, STATUS_RUNNING, STATUS_SUCCESS))
                .orderByDesc(TravelPlan::getId)
                .last("limit 1"));
    }

    private TravelPlan require(String planNo) {
        TravelPlan plan = planMapper.selectOne(new LambdaQueryWrapper<TravelPlan>()
                .eq(TravelPlan::getPlanNo, planNo)
                .last("limit 1"));
        if (plan == null) {
            throw BusinessException.notFound("行程不存在");
        }
        return plan;
    }

    private PlanGenerateResponse response(TravelPlan plan) {
        return new PlanGenerateResponse(plan.getPlanNo(), plan.getStatus(),
                plan.getDayCount(), Math.max(0, plan.getDayCount() - 1));
    }

    private List<String> normalizePreferences(List<String> preferences) {
        Set<String> values = new LinkedHashSet<>();
        if (preferences != null) {
            for (String value : preferences) {
                if (StringUtils.hasText(value)) {
                    values.add(value.trim());
                }
            }
        }
        return new ArrayList<>(values);
    }

    private String writePreferences(List<String> preferences) {
        try {
            return objectMapper.writeValueAsString(preferences);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> readPreferences(String preferences) {
        if (!StringUtils.hasText(preferences)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(preferences,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String nextPlanNo() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder builder = new StringBuilder("LP");
            for (int i = 0; i < 10; i++) {
                builder.append(PLAN_CHARS.charAt(random.nextInt(PLAN_CHARS.length())));
            }
            String planNo = builder.toString();
            Long count = planMapper.selectCount(new LambdaQueryWrapper<TravelPlan>()
                    .eq(TravelPlan::getPlanNo, planNo));
            if (count == null || count == 0) {
                return planNo;
            }
        }
        throw new BusinessException(50001, "生成行程编号失败，请重试");
    }

    private String normalizeTransport(String value) {
        if (!StringUtils.hasText(value)) {
            return "AUTO";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "FLIGHT", "TRAIN", "BOTH", "SELF_DRIVE", "AUTO" -> normalized;
            default -> "AUTO";
        };
    }

    private String hash(String cityCode, String originCityCode, String transportPreference,
                        LocalDate startDate, LocalDate endDate,
                        List<String> preferences, String extraNote) {
        String raw = cityCode + "|" + originCityCode + "|" + transportPreference + "|"
                + startDate + "|" + endDate + "|"
                + String.join(",", preferences) + "|" + extraNote;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    public JsonNode planJson(TravelPlan plan) {
        if (!StringUtils.hasText(plan.getContentJson())) {
            return null;
        }
        try {
            return objectMapper.readTree(plan.getContentJson());
        } catch (Exception e) {
            return null;
        }
    }
}
