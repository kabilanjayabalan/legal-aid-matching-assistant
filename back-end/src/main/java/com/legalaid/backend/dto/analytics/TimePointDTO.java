package com.legalaid.backend.dto.analytics;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TimePointDTO {
    private String time;   // day / month / year label
    private Long count;
}
