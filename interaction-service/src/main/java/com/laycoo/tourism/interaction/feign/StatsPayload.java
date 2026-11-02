package com.laycoo.tourism.interaction.feign;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StatsPayload {

    private String type;

    private long delta = 1;

    private BigDecimal ratingAvg;

    private Long ratingCount;

    public static StatsPayload of(String type, long delta) {
        StatsPayload payload = new StatsPayload();
        payload.setType(type);
        payload.setDelta(delta);
        return payload;
    }

    public static StatsPayload rating(BigDecimal avg, long count) {
        StatsPayload payload = new StatsPayload();
        payload.setType("rating");
        payload.setRatingAvg(avg);
        payload.setRatingCount(count);
        return payload;
    }
}
