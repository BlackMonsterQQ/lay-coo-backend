package com.laycoo.tourism.ai.plan.service;

import com.laycoo.tourism.ai.plan.dto.GeneratedPlan;
import com.laycoo.tourism.ai.plan.dto.PlanContextItem;
import com.laycoo.tourism.ai.plan.entity.TravelPlan;

import java.util.List;

public interface PlanGenerator {

    GeneratedPlan generate(TravelPlan plan, List<PlanContextItem> context);
}
