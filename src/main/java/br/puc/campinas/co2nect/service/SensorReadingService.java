package br.puc.campinas.co2nect.service;

import java.sql.SQLException;

import br.puc.campinas.co2nect.model.SensorReading;
import br.puc.campinas.co2nect.repository.ReadingRepository;

public class SensorReadingService {
    private final ReadingRepository repository;

    public SensorReadingService(ReadingRepository repository) {
        this.repository = repository;
    }

    public void processReading(SensorReading reading) {
        if (!isValid(reading)) {
            System.err.println("[INVALID_READING] Discarded invalid sensor reading:");
            System.err.println(reading);
            System.err.println();
            return;
        }

        if (reading.getCo2() > 1000) {
            System.out.println("[ALERT] High CO2 concentration detected (" + reading.getCo2() + " ppm) on device: " + reading.getDeviceId());
            // TODO: Publicar comando 'turnOnAlarm' no topico co2nect/{deviceId}/command (CT4)
        }

        try {
            repository.save(reading);
            System.out.println("Reading saved successfully for device: " + reading.getDeviceId());
            System.out.println(reading);
            System.out.println(); // Linha em branco entre cada save
        } catch (SQLException e) {
            System.err.println("[DATABASE_ERROR] Failed to persist reading for device '" + reading.getDeviceId() + "': " + e.getMessage());
            System.err.println();
        }
    }

    private boolean isValid(SensorReading reading) {
        if (reading == null || reading.getDeviceId() == null || reading.getDeviceId().trim().isEmpty()) return false;
        if (reading.getCo2() <= 0) return false;
        if (reading.getHumidity() < 0 || reading.getHumidity() > 100) return false;
        if (reading.getTemperature() < -40 || reading.getTemperature() > 90) return false;

        return true;
    }
}
