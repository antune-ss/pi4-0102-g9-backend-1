
package br.puc.campinas.co2nect;

import br.puc.campinas.co2nect.mqtt.MqttSubscriber;
import br.puc.campinas.co2nect.repository.MemoryReadingRepository;
import br.puc.campinas.co2nect.service.ReadingService;

public class MqttTest {

    public static void main(String[] args) {

        MemoryReadingRepository repository =
            new MemoryReadingRepository();

        ReadingService service =
            new ReadingService(repository);

        String broker = System.getenv("MQTT_BROKER_URL");

        if (broker == null || broker.isBlank()) {
            broker = "tcp://localhost:1883";
        }

        MqttSubscriber subscriber =
            new MqttSubscriber(broker, service);

        try {
            subscriber.iniciar();

            System.out.println(
                "[APP] Backend MQTT em execucao."
            );

        } catch (Exception e) {
            System.err.println(
                "[APP] Falha ao iniciar MQTT: " +
                e.getMessage()
            );
            e.printStackTrace();
        }
    }
}

