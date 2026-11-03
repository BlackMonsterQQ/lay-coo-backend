package com.laycoo.tourism.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /** deepseek / qwen / doubao，仅用于展示与 prompt 标识 */
    private String provider = "deepseek";

    /** OpenAI 兼容的 Chat Completions 地址，如 https://api.deepseek.com */
    private String baseUrl = "https://api.deepseek.com";

    private String apiKey = "";

    private String model = "deepseek-chat";

    private int timeoutSeconds = 60;

    private int countLimit = 5;

    private double temperature = 0.8;

    /** 行程规划最多天数 */
    private int maxDays = 7;

    /** DeepSeek 调用失败时，是否用内置模板兜底 */
    private boolean planFallbackEnabled = true;

    private int guestDailyLimit = 3;

    private int userDailyLimit = 10;

    public boolean llmEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }
}
