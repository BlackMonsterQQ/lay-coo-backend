package com.laycoo.tourism.ai.service;

import com.laycoo.tourism.ai.dto.AiContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 内容规则审核：必填字段、长度、敏感词与格式检查，作为人工复核前的第一道关口。
 */
@Slf4j
@Component
public class ContentModerator {

    private static final List<String> SENSITIVE_WORDS = List.of(
            "赌博", "博彩", "色情", "毒品", "代开发票", "办证", "枪支", "暴力", "传销", "洗钱");

    private static final List<String> PLACEHOLDERS = List.of(
            "待补充", "此处省略", "TODO", "xxx", "XXX", "示例内容");

    /**
     * @return null 表示通过，否则返回拒绝原因
     */
    public String check(AiContent content) {
        if (content == null) {
            return "内容为空";
        }
        String title = trim(content.getTitle());
        String summary = trim(content.getSummary());
        String body = trim(content.getBody());
        if (title.length() < 4 || title.length() > 60) {
            return "标题长度不符合要求";
        }
        if (summary.length() < 15 || summary.length() > 200) {
            return "摘要长度不符合要求";
        }
        if (body.length() < 120) {
            return "正文内容过短";
        }
        if (!StringUtils.hasText(content.getAddress())) {
            return "缺少地址信息";
        }
        String all = title + summary + body;
        for (String word : SENSITIVE_WORDS) {
            if (all.contains(word)) {
                return "包含敏感词：" + word;
            }
        }
        for (String word : PLACEHOLDERS) {
            if (all.toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT))) {
                return "存在未完成内容占位符：" + word;
            }
        }
        return null;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
