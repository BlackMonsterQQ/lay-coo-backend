package com.laycoo.tourism.ai.plan.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedPlan {

    private JsonNode plan;

    private String model;

    private long tokens;

    private String prompt;
}
