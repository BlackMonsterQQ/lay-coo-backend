package com.laycoo.tourism.ai.service;

import com.laycoo.tourism.ai.dto.GeneratedResult;
import com.laycoo.tourism.ai.entity.AiTask;

public interface ContentGenerator {

    GeneratedResult generate(AiTask task);
}
