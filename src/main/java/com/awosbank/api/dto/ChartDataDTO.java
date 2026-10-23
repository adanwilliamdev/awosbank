package com.awosbank.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data @AllArgsConstructor
public class ChartDataDTO {
    private List<String> labels;
    private List<BigDecimal> entradas;
    private List<BigDecimal> saidas;
}