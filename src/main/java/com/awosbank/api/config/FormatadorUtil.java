package com.awosbank.api.config;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Component("formatador")
public class FormatadorUtil {
    private static final DecimalFormat FMT =
            new DecimalFormat("#,##0.00", new DecimalFormatSymbols(new Locale("pt", "BR")));

    public String moeda(BigDecimal v) {
        return v == null ? "R$ 0,00" : "R$ " + FMT.format(v);
    }
}