package br.puc.campinas.co2nect;

import java.math.BigDecimal;

/** Aggregates for a requested device and time interval. Averages are null if count is zero. */
public record ReadingAverage(
        long readingCount,
        Long co2Average,
        BigDecimal temperatureAverage,
        BigDecimal humidityAverage) {
}
