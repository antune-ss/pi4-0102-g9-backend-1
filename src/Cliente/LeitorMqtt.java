package src.Cliente;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;

import java.util.UUID;

public class LeitorMqtt implements MqttCallback {

    private MqttClient cliente;

    public void iniciar() throws MqttException {

        String enderecoBroker = "tcp://localhost:1883";

        String clientId = "servidor-java-" + UUID.randomUUID();

        cliente = new MqttClient(enderecoBroker, clientId);

        cliente.setCallback(this);

        cliente.connect();

        cliente.subscribe("ar/sala01/leituras", 1);

        System.out.println("Conectado ao broker MQTT!");
        System.out.println("Aguardando leituras do ESP32...");
    }

    @Override
    public void messageArrived(String topic, MqttMessage mensagem) throws Exception {
        // mensagem para testes, apagar depois
        System.out.println("Mensagem recebida no tópico: " + topic);

        try {
            // declarando nossa leitura
            Leitura leitura = Leitura.pegaLeitura(mensagem);
            // mensagem para testes, apagar depois
            System.out.println("Leitura convertida com sucesso!");

            // Futuramente:
            // salvarLeituraNoBanco(leitura)
            // ou
            // mandar para outra classe nossa leitura para que seja tratada

        } catch (RuntimeException e) {
            System.err.println(
                    "Erro ao interpretar ou validar a leitura: "
                            + e.getMessage());
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
        // Este callback é utilizado para entregas de mensagens
        // publicadas pelo próprio cliente.
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

    public static void main(String[] args) throws MqttException {
        LeitorMqtt leitor = new LeitorMqtt();
        leitor.iniciar();
    }
}
