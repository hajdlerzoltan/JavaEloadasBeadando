package hu.forex.app.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRate(
        LocalDate date,
        String currency,
        int unit,
        BigDecimal rate
) {
}