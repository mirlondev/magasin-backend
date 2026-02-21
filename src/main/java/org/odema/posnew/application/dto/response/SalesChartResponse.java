package org.odema.posnew.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record SalesChartResponse(
        String period,
        LocalDate startDate,
        LocalDate endDate,
        Map<String, BigDecimal> salesData,
        Map<String, BigDecimal> previousPeriodSalesData,
        BigDecimal growthRate,
        List<String> labels
) {
}
