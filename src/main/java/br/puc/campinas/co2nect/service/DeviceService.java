package br.puc.campinas.co2nect.service;

import java.sql.SQLException;
import br.puc.campinas.co2nect.repository.DeviceRepository;

public class DeviceService {
    private final DeviceRepository repository;

    public DeviceService(DeviceRepository repository) {
        this.repository = repository;
    }

    public void updateStatus(String deviceId, boolean isOnline) {
        if (deviceId == null || deviceId.trim().isEmpty()) return;
        
        try {
            repository.upsert(deviceId.trim(), isOnline);
            System.out.println("[STATUS] Device '" + deviceId + "' updated successfully: " + (isOnline ? "ONLINE" : "OFFLINE"));
            System.out.println();
        } catch (SQLException e) {
            System.err.println("[DATABASE_ERROR] Failed to persist device  '" + deviceId + "': " + e.getMessage());
            System.err.println();
        }
    }
}
