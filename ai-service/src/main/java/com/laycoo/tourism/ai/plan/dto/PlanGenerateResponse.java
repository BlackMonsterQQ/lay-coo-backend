package com.laycoo.tourism.ai.plan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanGenerateResponse {

    private String planNo;
    private String status;
    private int dayCount;
    private int nights;
}
