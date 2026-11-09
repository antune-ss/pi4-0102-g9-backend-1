package br.puc.campinas.co2nect;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Camada de persistência PostgreSQL do CO2NNECT.
 *
 * Cada operação abre e fecha sua própria conexão JDBC. Isso evita manter uma
 * conexão inválida quando o contêiner PostgreSQL é reiniciado e permite que a
 * próxima leitura recebida volte a ser gravada sem reiniciar a aplicação Java.
 */
public class DatabaseManager {
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/co2nect_db";
    private static final String DEFAULT_USER = "co2nect_user";
    private static final String DEFAULT_PASSWORD = "co2nect_pass";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        this(
                setting("DB_URL", DEFAULT_URL),
                setting("DB_USER", DEFAULT_USER),
                setting("DB_PASSWORD", DEFAULT_PASSWORD));
    }

    /** Construtor configurável, também utilizado pelos testes automatizados. */
    public DatabaseManager(String url, String user, String password) {
        this.url = requireText(url, "DB_URL");
        this.user = requireText(user, "DB_USER");
        this.password = requireText(password, "DB_PASSWORD");
    }

    private static String setting(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " não pode ser nulo ou vazio.");
        }
        return value.trim();
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Abre uma conexão real e propaga SQLException caso o banco esteja indisponível. */
    public boolean checkConnection() throws SQLException {
        try (Connection connection = openConnection()) {
            return connection.isValid(3);
        }
    }

    /**
     * Persiste uma leitura de sensor e retorna o ID gerado pelo PostgreSQL.
     * O dispositivo é cadastrado/atualizado antes da leitura para respeitar a FK
     * sensor_reading.device_id -> sensor_device.device_id do schema atual.
     */
    public long saveRead(SensorData data) throws SQLException {
        ValidatedReading reading = validateAndNormalize(data);
        String upsertDeviceSql = """
                INSERT INTO sensor_device (device_id, display_name, is_online, last_seen_at, last_status_at)
                VALUES (?, ?, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (device_id) DO UPDATE
                   SET is_online = TRUE,
                       last_seen_at = CURRENT_TIMESTAMP,
                       updated_at = CURRENT_TIMESTAMP
                """;
        String insertReadingSql = """
                INSERT INTO sensor_reading
                    (device_id, co2, temperature, humidity, measured_at)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """;

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(upsertDeviceSql)) {
                    statement.setString(1, reading.deviceId());
                    statement.setString(2, reading.deviceId());
                    statement.executeUpdate();
                }

                long generatedId;
                try (PreparedStatement statement = connection.prepareStatement(insertReadingSql)) {
                    statement.setString(1, reading.deviceId());
                    statement.setInt(2, reading.co2());
                    statement.setBigDecimal(3, BigDecimal.valueOf(reading.temperature()));
                    statement.setBigDecimal(4, BigDecimal.valueOf(reading.humidity()));
                    statement.setTimestamp(5, Timestamp.from(reading.measuredAt()));

                    try (ResultSet resultSet = statement.executeQuery()) {
                        if (!resultSet.next()) {
                            throw new SQLException("O PostgreSQL não retornou o ID da leitura inserida.");
                        }
                        generatedId = resultSet.getLong("id");
                    }
                }

                connection.commit();
                System.out.printf("[DB] Leitura salva: id=%d, dispositivo=%s, CO2=%d ppm%n",
                        generatedId, reading.deviceId(), reading.co2());
                return generatedId;
            } catch (SQLException | RuntimeException exception) {
                rollbackQuietly(connection);
                throw exception;
            }
        }
    }

    /** Compatibilidade com o caminho WebSocket já existente no projeto. */
    public void insertReading(String deviceId, int co2, double temperature,
                              double humidity, Timestamp measuredAt) throws SQLException {
        SensorData data = new SensorData();
        data.deviceId = deviceId;
        data.co2 = co2;
        data.temperature = temperature;
        data.humidity = humidity;
        data.measuredAt = measuredAt == null ? null : measuredAt.toInstant().toString();
        saveRead(data);
    }

    /** Atualiza o estado de conectividade recebido no tópico MQTT /status. */
    public void updateDeviceStatus(String deviceId, boolean online) throws SQLException {
        String normalizedDeviceId = validateDeviceId(deviceId);
        String sql = """
                INSERT INTO sensor_device
                    (device_id, display_name, is_online, last_seen_at, last_status_at)
                VALUES (?, ?, ?, CASE WHEN ? THEN CURRENT_TIMESTAMP ELSE NULL END, CURRENT_TIMESTAMP)
                ON CONFLICT (device_id) DO UPDATE
                   SET is_online = EXCLUDED.is_online,
                       last_seen_at = CASE
                           WHEN EXCLUDED.is_online THEN CURRENT_TIMESTAMP
                           ELSE sensor_device.last_seen_at
                       END,
                       last_status_at = CURRENT_TIMESTAMP,
                       updated_at = CURRENT_TIMESTAMP
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedDeviceId);
            statement.setString(2, normalizedDeviceId);
            statement.setBoolean(3, online);
            statement.setBoolean(4, online);
            statement.executeUpdate();
        }
    }

    /**
     * Lista as leituras de um dispositivo no intervalo solicitado, mais recentes
     * primeiro. O segundo critério (id DESC) desempata leituras com mesmo horário.
     */
    public List<SensorReading> listReadings(String deviceId, Instant start,
                                            Instant end, int limit) throws SQLException {
        String normalizedDeviceId = validateDeviceId(deviceId);
        validateInterval(start, end);
        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException("limit deve estar entre 1 e 1000.");
        }

        String sql = """
                SELECT id, device_id, co2, temperature, humidity, measured_at, received_at
                  FROM sensor_reading
                 WHERE device_id = ?
                   AND measured_at >= ?
                   AND measured_at <= ?
                 ORDER BY measured_at DESC, id DESC
                 LIMIT ?
                """;

        List<SensorReading> readings = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedDeviceId);
            statement.setTimestamp(2, Timestamp.from(start));
            statement.setTimestamp(3, Timestamp.from(end));
            statement.setInt(4, limit);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    readings.add(mapReading(resultSet));
                }
            }
        }
        return readings;
    }

    /** Histórico resumido para compatibilidade: últimas 50 leituras do dispositivo. */
    public List<SensorReading> getHistory(String deviceId) throws SQLException {
        String normalizedDeviceId = validateDeviceId(deviceId);
        String sql = """
                SELECT id, device_id, co2, temperature, humidity, measured_at, received_at
                  FROM sensor_reading
                 WHERE device_id = ?
                 ORDER BY measured_at DESC, id DESC
                 LIMIT 50
                """;

        List<SensorReading> readings = new ArrayList<>();
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedDeviceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    readings.add(mapReading(resultSet));
                }
            }
        }
        return readings;
    }

    /** Calcula as médias diretamente no PostgreSQL para o intervalo solicitado. */
    public ReadingAverage getAverage(String deviceId, Instant start, Instant end) throws SQLException {
        String normalizedDeviceId = validateDeviceId(deviceId);
        validateInterval(start, end);

        String sql = """
                SELECT COUNT(*) AS reading_count,
                       CASE WHEN COUNT(*) = 0 THEN NULL ELSE ROUND(AVG(co2))::BIGINT END AS co2_average,
                       ROUND(AVG(temperature), 2) AS temperature_average,
                       ROUND(AVG(humidity), 2) AS humidity_average
                  FROM sensor_reading
                 WHERE device_id = ?
                   AND measured_at >= ?
                   AND measured_at <= ?
                """;

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalizedDeviceId);
            statement.setTimestamp(2, Timestamp.from(start));
            statement.setTimestamp(3, Timestamp.from(end));

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new SQLException("Não foi possível calcular as médias do período.");
                }
                long count = resultSet.getLong("reading_count");
                long co2Value = resultSet.getLong("co2_average");
                Long co2Average = resultSet.wasNull() ? null : co2Value;
                BigDecimal temperatureAverage = resultSet.getBigDecimal("temperature_average");
                BigDecimal humidityAverage = resultSet.getBigDecimal("humidity_average");
                return new ReadingAverage(count, co2Average, temperatureAverage, humidityAverage);
            }
        }
    }

    private static SensorReading mapReading(ResultSet resultSet) throws SQLException {
        return new SensorReading(
                resultSet.getLong("id"),
                resultSet.getString("device_id"),
                resultSet.getInt("co2"),
                resultSet.getBigDecimal("temperature"),
                resultSet.getBigDecimal("humidity"),
                resultSet.getObject("measured_at", OffsetDateTime.class),
                resultSet.getObject("received_at", OffsetDateTime.class));
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException rollbackError) {
            System.err.println("[DB] Falha ao desfazer transação: " + rollbackError.getMessage());
        }
    }

    private static ValidatedReading validateAndNormalize(SensorData data) {
        if (data == null) {
            throw new IllegalArgumentException("A leitura não pode ser nula.");
        }
        String deviceId = validateDeviceId(data.deviceId);

        if (data.co2 == null || data.co2 < 0) {
            throw new IllegalArgumentException("CO2 deve ser um inteiro maior ou igual a zero.");
        }
        if (data.temperature == null || !Double.isFinite(data.temperature)
                || data.temperature < -40.0 || data.temperature > 100.0) {
            throw new IllegalArgumentException("Temperatura deve estar entre -40 e 100 °C.");
        }
        if (data.humidity == null || !Double.isFinite(data.humidity)
                || data.humidity < 0.0 || data.humidity > 100.0) {
            throw new IllegalArgumentException("Umidade deve estar entre 0 e 100%.");
        }
        if (data.measuredAt == null || data.measuredAt.isBlank()) {
            throw new IllegalArgumentException("measuredAt é obrigatório e deve estar no formato ISO-8601.");
        }

        return new ValidatedReading(deviceId, data.co2, data.temperature, data.humidity,
                parseMeasuredAt(data.measuredAt));
    }

    private static String validateDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId é obrigatório.");
        }
        String normalized = deviceId.trim();
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("deviceId deve ter no máximo 100 caracteres.");
        }
        return normalized;
    }

    private static void validateInterval(Instant start, Instant end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("As datas inicial e final são obrigatórias.");
        }
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("A data inicial não pode ser posterior à data final.");
        }
    }

    /** Aceita ISO-8601 com offset/Z ou data-hora local (interpretada no fuso da JVM). */
    private static Instant parseMeasuredAt(String value) {
        String normalized = value.trim();
        try {
            return OffsetDateTime.parse(normalized).toInstant();
        } catch (DateTimeParseException ignored) {
            try {
                return Instant.parse(normalized);
            } catch (DateTimeParseException ignoredAgain) {
                try {
                    return LocalDateTime.parse(normalized)
                            .atZone(ZoneId.systemDefault())
                            .toInstant();
                } catch (DateTimeParseException invalid) {
                    throw new IllegalArgumentException(
                            "measuredAt deve seguir ISO-8601, por exemplo 2026-10-13T19:20:00Z.",
                            invalid);
                }
            }
        }
    }

    private record ValidatedReading(String deviceId, int co2, double temperature,
                                    double humidity, Instant measuredAt) {
    }
}
