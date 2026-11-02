package com.laycoo.tourism.ai.service;

import com.laycoo.tourism.ai.dto.AiContent;
import com.laycoo.tourism.ai.feign.ContentClient;
import com.laycoo.tourism.common.api.ApiResponse;
import com.laycoo.tourism.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentPublisher {

    private final ContentClient contentClient;

    public Long publish(AiContent content) {
        ApiResponse<Long> response = contentClient.create(content);
        if (response == null || response.getCode() != 0 || response.getData() == null) {
            throw new BusinessException(502, "内容入库失败: "
                    + (response == null ? "无响应" : response.getMsg()));
        }
        return response.getData();
    }
}
