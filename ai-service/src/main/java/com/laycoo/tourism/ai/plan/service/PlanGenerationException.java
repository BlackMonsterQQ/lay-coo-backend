package com.laycoo.tourism.ai.plan.service;

public class PlanGenerationException extends RuntimeException {

    public PlanGenerationException(String message) {
        super(message);
    }

    public PlanGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
