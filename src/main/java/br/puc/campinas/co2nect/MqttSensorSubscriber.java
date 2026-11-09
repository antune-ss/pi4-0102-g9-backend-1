package br.puc.campinas.co2nect;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Recebe telemetria e status do ESP32 pelo broker Mosquitto. */
public final class MqttSensorSubscriber implements AutoCloseable {
    private final DatabaseManager database;
    private final String readingsTopicFilter;
    private final String statusTopicFilter;
    private final MqttClient client;
    private final Gson gson = new Gson();

    public MqttSensorSubscriber(DatabaseManager database, String brokerUrl,
                                String clientId, String readingsTopicFilter,
                                String statusTopicFilter) throws MqttException {
        if (database == null) {
            throw new IllegalArgumentException("DatabaseManager é obrigatório.");
        }
        this.database = database;
        this.readingsTopicFilter = requireText(readingsTopicFilter, "MQTT_READINGS_TOPIC");
        this.statusTopicFilter = requireText(statusTopicFilter, "MQTT_STATUS_TOPIC");
        this.client = new MqttClient(
                requireText(brokerUrl, "MQTT_BROKER_URL"),
                requireText(clientId, "MQTT_CLIENT_ID"),
                new MemoryPersistence());

        this.client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println("[MQTT] Conectado a " + serverURI
                        + (reconnect ? " (reconectado)" : ""));
                if (reconnect) {
                    subscribeAfterReconnect();
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                System.err.println("[MQTT] Conexão perdida: "
                        + (cause == null ? "causa desconhecida" : cause.getMessage()));
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                handleMessage(topic, payload);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // O subscriber não publica mensagens de aplicação.
            }
        });
    }

    public void start() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(30);

        client.connect(options);
        subscribeTopics();
        System.out.println("[MQTT] Assinando: " + readingsTopicFilter
                + " e " + statusTopicFilter);
    }

    private void subscribeAfterReconnect() {
        try {
            subscribeTopics();
        } catch (MqttException exception) {
            System.err.println("[MQTT] Não foi possível restaurar as assinaturas: "
                    + exception.getMessage());
        }
    }

    private void subscribeTopics() throws MqttException {
        client.subscribe(readingsTopicFilter, 1);
        client.subscribe(statusTopicFilter, 1);
    }

    private void handleMessage(String topic, String payload) {
        try {
            if (matchesTopic(readingsTopicFilter, topic)) {
                SensorData data = gson.fromJson(payload, SensorData.class);
                if (data == null) {
                    throw new IllegalArgumentException("Payload JSON vazio ou inválido.");
                }

                if (data.deviceId == null || data.deviceId.isBlank()) {
                    data.deviceId = extractDeviceId(topic, "telemetry");
                }
                // O ESP32 pode omitir a data; nesse caso registramos o instante de recebimento.
                if (data.measuredAt == null || data.measuredAt.isBlank()) {
                    data.measuredAt = Instant.now().toString();
                }

                long id = database.saveRead(data);
                System.out.println("[MQTT] Medição persistida: id=" + id
                        + ", dispositivo=" + data.deviceId);
                return;
            }

            if (matchesTopic(statusTopicFilter, topic)) {
                String deviceId = extractDeviceId(topic, "status");
                Integer status = parseStatus(payload);
                if (status == null || (status != 0 && status != 1)) {
                    throw new IllegalArgumentException("status deve ser 0 (offline) ou 1 (online).");
                }
                database.updateDeviceStatus(deviceId, status == 1);
                System.out.println("[MQTT] Status atualizado: dispositivo=" + deviceId
                        + ", online=" + (status == 1));
                return;
            }

            System.out.println("[MQTT] Mensagem ignorada em tópico não tratado: " + topic);
        } catch (Exception exception) {
            // Uma mensagem inválida ou uma falha transitória no banco não derruba o subscriber.
            System.err.println("[MQTT] Mensagem descartada no tópico " + topic
                    + ": " + exception.getMessage());
        }
    }

    private static String extractDeviceId(String topic, String expectedSuffix) {
        String[] segments = topic.split("/");
        if (segments.length != 3 || !"co2nect".equals(segments[0])
                || !expectedSuffix.equals(segments[2]) || segments[1].isBlank()) {
            throw new IllegalArgumentException("Tópico MQTT fora do padrão esperado: " + topic);
        }
        return segments[1];
    }

    /** Implementa os curingas MQTT '+' e '#', suficientes para os filtros configurados. */
    private static boolean matchesTopic(String filter, String topic) {
        String[] filterParts = filter.split("/", -1);
        String[] topicParts = topic.split("/", -1);

        int index = 0;
        while (index < filterParts.length) {
            String filterPart = filterParts[index];
            if ("#".equals(filterPart)) {
                return index == filterParts.length - 1;
            }
            if (index >= topicParts.length) {
                return false;
            }
            if (!"+".equals(filterPart) && !filterPart.equals(topicParts[index])) {
                return false;
            }
            index++;
        }
        return index == topicParts.length;
    }

    private static Integer parseStatus(String payload) {
        String value = payload == null ? "" : payload.trim();
        try {
            if (value.startsWith("{")) {
                JsonObject object = JsonParser.parseString(value).getAsJsonObject();
                JsonElement status = object.get("status");
                if (status == null || status.isJsonNull()) {
                    return null;
                }
                return status.getAsInt();
            }
            return Integer.valueOf(value);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    @Override
    public void close() {
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException exception) {
            System.err.println("[MQTT] Erro ao encerrar cliente: " + exception.getMessage());
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " é obrigatório.");
        }
        return value.trim();
    }
}
