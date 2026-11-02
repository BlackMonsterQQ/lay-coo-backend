package com.laycoo.tourism.recommend.dto;

import com.laycoo.tourism.common.dto.ContentCard;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendItem {

    private ContentCard content;

    private String reason;

    private double score;

    private String scene;
}
