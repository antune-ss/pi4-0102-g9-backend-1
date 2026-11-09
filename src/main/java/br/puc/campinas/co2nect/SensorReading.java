package br.puc.campinas.co2nect;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** One persisted sensor sample. Times include their UTC offset. */
public record SensorReading(
        long id,
        String deviceId,
        int co2,
        BigDecimal temperature,
        BigDecimal humidity,
        OffsetDateTime measuredAt,
        OffsetDateTime receivedAt) {
}
