package br.puc.campinas.co2nect.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeviceRepository {
    private final String url;
    private final String user;
    private final String password;

    public DeviceRepository(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Executa o UPSERT na tabela 'devices':
     * - Se o dispositivo não existir, faz INSERT com o nome padrão igual ao id.
     * - Se já existir, atualiza 'is_online' e 'last_seen'.
     */
    public void upsert(String deviceId, boolean isOnline) throws SQLException {
        String sql = """
            INSERT INTO devices (id, name, is_online, last_seen, created_at)
            VALUES (?, ?, ?, NOW(), NOW())
            ON CONFLICT (id) DO UPDATE
            SET is_online = EXCLUDED.is_online,
                last_seen = NOW();
        """;

        try (
            Connection conn = DriverManager.getConnection(url, user, password);
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, deviceId);
            stmt.setString(2, deviceId); // Nome inicial recebe o próprio ID (ex: 'ESP-SALA-101')
            stmt.setBoolean(3, isOnline);

            stmt.executeUpdate();
        }
    }
}