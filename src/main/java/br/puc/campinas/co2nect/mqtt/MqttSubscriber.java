
package br.puc.campinas.co2nect.mqtt;

import br.puc.campinas.co2nect.service.ReadingService;

import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;

public class MqttSubscriber {

    private final String brokerUrl;
    private final ReadingService service;
    private MqttClient client;

    public MqttSubscriber(
            String brokerUrl,
            ReadingService service) {

        this.brokerUrl = brokerUrl;
        this.service = service;
    }

    public void iniciar() throws MqttException {

        client = new MqttClient(
            brokerUrl,
            MqttClient.generateClientId()
        );

        MqttConnectOptions options =
            new MqttConnectOptions();

        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        client.setCallback(new MqttCallbackExtended() {

            @Override
            public void connectComplete(
                    boolean reconnect,
                    String serverURI) {

                try {
                    client.subscribe("/sensor/data", 1);

                    System.out.println(
                        "[MQTT] Conectado: " + serverURI
                    );

                    System.out.println(
                        "[MQTT] Aguardando /sensor/data"
                    );

                } catch (MqttException e) {
                    System.err.println(
                        "[MQTT] Erro na assinatura: " +
                        e.getMessage()
                    );
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                System.err.println(
                    "[MQTT] Conexao perdida: " +
                    cause.getMessage()
                );
            }

            @Override
            public void messageArrived(
                    String topic,
                    MqttMessage message) {

                String json = new String(
                    message.getPayload(),
                    StandardCharsets.UTF_8
                );

                System.out.println(
                    "[MQTT] Mensagem recebida: " + json
                );

                service.processar(json);
            }

            @Override
            public void deliveryComplete(
                    IMqttDeliveryToken token) {
                // Cliente utilizado apenas para receber.
            }
        });

        client.connect(options);
    }

    public void encerrar() throws MqttException {
        if (client != null) {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        }
    }
}
