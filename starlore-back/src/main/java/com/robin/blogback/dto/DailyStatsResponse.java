package com.robin.blogback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyStatsResponse {
    private List<DailyItem> data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyItem {
        private String date;
        private int count;
    }
}
