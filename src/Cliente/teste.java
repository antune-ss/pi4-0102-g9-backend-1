package src.Cliente;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LeitorMqtt implements MqttCallback, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger(LeitorMqtt.class.getName());

    private static final String BROKER = "tcp://localhost:1883";

    private static final String TOPICO = "ar/sala01/leituras";

    private MqttClient cliente;

    public void iniciar() throws MqttException {
        cliente = new MqttClient(
                BROKER,
                "servidor-java-" + UUID.randomUUID());

        cliente.setCallback(this);

        MqttConnectOptions opcoes = new MqttConnectOptions();
        opcoes.setAutomaticReconnect(true);
        opcoes.setCleanSession(true);
        opcoes.setConnectionTimeout(10);

        cliente.connect(opcoes);
        cliente.subscribe(TOPICO, 1);

        LOGGER.info("Cliente MQTT conectado e inscrito no tópico.");
    }

    @Override
    public void messageArrived(
            String topic,
            MqttMessage mensagem) {

        try {
            Leitura leitura = Leitura.pegaLeitura(mensagem);

            LOGGER.fine("Leitura MQTT convertida com sucesso.");

            // Próximas etapas:
            // 1. Persistir leitura no PostgreSQL.
            // 2. Notificar os clientes WebSocket.

        } catch (RuntimeException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Mensagem MQTT inválida; leitura descartada.",
                    e);
        }
    }

    @Override
    public void connectionLost(Throwable causa) {
        LOGGER.log(
                Level.WARNING,
                "Conexão MQTT perdida; será tentada a reconexão.",
                causa);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // Não é necessário fazer nada aqui se este cliente
        // apenas recebe mensagens e não publica.
    }

    @Override
    public void close() throws MqttException {
        if (cliente != null && cliente.isConnected()) {
            cliente.disconnect();
        }

        if (cliente != null) {
            cliente.close();
        }
    }
}