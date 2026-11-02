package com.laycoo.tourism.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratedResult {

    private List<AiContent> contents = new ArrayList<>();

    private long tokens;

    private String model;

    private String prompt;
}
