package br.puc.campinas.co2nect;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ponto de entrada do backend CO2NNECT. */
public class App {
    private static final String DEFAULT_BROKER_URL = "tcp://localhost:1883";
    private static final String DEFAULT_READINGS_TOPIC = "co2nect/+/telemetry";
    private static final String DEFAULT_STATUS_TOPIC = "co2nect/+/status";

    public static void main(String[] args) {
        loadDotEnv();

        DatabaseManager database = new DatabaseManager();
        MqttSensorSubscriber subscriber = null;
        SensorServer webSocketServer = null;

        try {
            String brokerUrl = setting("MQTT_BROKER_URL", DEFAULT_BROKER_URL);
            String clientId = setting("MQTT_CLIENT_ID", "co2nect-backend-1");
            String readingsTopic = setting("MQTT_READINGS_TOPIC", DEFAULT_READINGS_TOPIC);
            String statusTopic = setting("MQTT_STATUS_TOPIC", DEFAULT_STATUS_TOPIC);
            int webSocketPort = Integer.parseInt(setting("WEBSOCKET_PORT", "8080"));

            subscriber = new MqttSensorSubscriber(database, brokerUrl, clientId,
                    readingsTopic, statusTopic);
            webSocketServer = new SensorServer(webSocketPort, database);

            MqttSensorSubscriber finalSubscriber = subscriber;
            SensorServer finalWebSocketServer = webSocketServer;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[APP] Encerrando CO2NNECT Backend...");
                finalSubscriber.close();
                try {
                    finalWebSocketServer.stop(1000);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }, "co2nect-shutdown"));

            // Inicie os serviços depois de construir todas as configurações.
            subscriber.start();
            webSocketServer.start();

            System.out.println("[APP] Backend iniciado. MQTT: " + brokerUrl
                    + " | WebSocket: porta " + webSocketPort);
            System.out.println("[APP] Pressione Ctrl+C para encerrar.");
            Thread.currentThread().join();
        } catch (Exception exception) {
            System.err.println("[APP] Falha ao iniciar o backend: " + exception.getMessage());
            exception.printStackTrace();
            if (subscriber != null) {
                subscriber.close();
            }
            if (webSocketServer != null) {
                try {
                    webSocketServer.stop(1000);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static String setting(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    /** Lê .env local como fallback; variáveis do sistema continuam tendo prioridade. */
    private static void loadDotEnv() {
        Path envPath = Path.of(".env");
        if (!Files.isRegularFile(envPath)) {
            return;
        }

        try {
            for (String rawLine : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int separator = line.indexOf('=');
                if (separator <= 0) {
                    continue;
                }

                String key = line.substring(0, separator).trim();
                String value = line.substring(separator + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException exception) {
            System.err.println("[APP] Não foi possível ler .env: " + exception.getMessage());
        }
    }
}
