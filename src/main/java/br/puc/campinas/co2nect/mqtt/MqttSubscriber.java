package br.puc.campinas.co2nect.mqtt;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import br.puc.campinas.co2nect.model.SensorReading;
import br.puc.campinas.co2nect.service.SensorReadingService;

/**
 * Adaptador de Entrada para o protocolo MQTT.
 * 
 * Responsável por:
 * 1. Estabelecer e manter a conexão TCP assíncrona com o Broker Eclipse Mosquitto.
 * 2. Assinar os tópicos de telemetria e status/LWT om QoS 1.
 * 3. Rotear mensagens recebidas, desserializar o JSON via Gson e despachar para o SensorReadingService.
 * 4. Garantir reconexão automática e re-inscrição em caso de instabilidade de rede.
 */
public class MqttSubscriber implements MqttCallbackExtended {
    private final String brokerUrl;
    private final String username;
    private final String password;
    private final String caCertPath;

    private final String[] topics;
    private final SensorReadingService service;
    private final Gson gson;
    private MqttClient client;

    /**
     * Construtor que recebe as dependências necessárias para a comunicação MQTT.
     * 
     * @param brokerUrl URL de conexão do broker (ex: "tcp://localhost:1883")
     * @param topics    lista de tópicos que o cliente deve assinar
     * @param service   camada de serviço para processamento e validação das leituras
     */
    public MqttSubscriber(String brokerUrl, String username, String password, String caCertPath, String[] topics, SensorReadingService service) {
        this.brokerUrl = brokerUrl;
        this.username = username;
        this.password = password;
        this.caCertPath = caCertPath;

        this.topics = topics;
        this.service = service;

        /**
         * Por padrão, o Gson desconhece as classes de data do Java 8+ (java.time.*) e tenta inspecionar
         * os campos privados internos da JVM via reflexão, o que é bloqueado pelo sistema de módulos do Java 21.
         * 
         * Este adaptador intercepta qualquer campo LocalDateTime e define a regra de conversão:
         *  1. Se o campo vier ausente, nulo ou vazio no JSON (o ESP32 não enviou o horário), assume o instante atual (now).
         *  2. Se vier preenchido (ex: "2026-10-17T21:55:00"), faz o parse do texto no padrão ISO-8601 nativamente.
         * 
         * OBSERVAÇÕES:
         * - new GsonBuilder(): Em vez de pegar a configuração "crua" do new Gson(), o Builder permite customizar 
         *   como o Gson deve se comportar antes de gerar o objeto final com o .create().
         * 
         * - .registerTypeAdapter(LocalDateTime.class, ...): Instrui o Gson a não usar reflexão para LocalDateTime,
         *   delegando a conversão para o adaptador customizado.
         * 
         * - A Função Lambda (json, type, context) -> { ... }: (https://www.devmedia.com.br/como-usar-funcoes-lambda-em-java/32826)
         * 
         * - A assinatura do método registerTypeAdapter dentro da biblioteca do Gson: public GsonBuilder registerTypeAdapter(Type type, Object typeAdapter)
         *   O Gson aceita no segundo parâmetro qualquer Object, porque ele pode receber várias coisas diferentes: Um JsonDeserializer (para ler JSON), Um JsonSerializer (para gerar JSON), Um TypeAdapter, Um InstanceCreator
         * 
         *   Faz um cast da interface 'JsonDeserializer<LocalDateTime>', cujo método é:
         *   public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
         *   Isso é necessário pq uma função lambda nn tem tipo e o registerTypeAdapter nn vai seber oq fazer
         * 
         *   O que são os 3 parâmetros?
         *   1. json (JsonElement): É o valor bruto daquele campo específico no JSON (ex: "2026-10-17T21:55:00" ou null).
         *      É dele que se extrai o texto real chamando json.getAsString().
         *   2. type (Type): O tipo de dado Java de destino esperado (neste caso, a classe LocalDateTime.class).
         *   3. context (JsonDeserializationContext): O próprio assistente do Gson, usado caso fosse preciso 
         *      desserializar outros sub-objetos complexos aninhados (não necessário para uma data simples).
         */
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, type, context) -> {
                if (json == null || json.isJsonNull() || json.getAsString().trim().isEmpty()) {
                    return LocalDateTime.now();
                }

                return LocalDateTime.parse(json.getAsString());
            }).create();
    }

    public void start(boolean reconnect, boolean cleanSession, int timeout, int keepAlive) throws MqttException {
        String clientId = "co2nect-server-" + UUID.randomUUID().toString().substring(0, 8);

        /**
         * O MQTT precisa guardar em algum lugar as mensagens que ainda estão aguardando confirmação (QoS 1 e 2).
         * Por padrão, se nada for passado, o Paho cria pastas e arquivos no disco rígido para salvar esse estado.
         * O MemoryPersistence instrui o Paho a gerenciar isso direto na memória RAM. É muito mais rápido e evita deixar arquivos temporários presos no seu projeto.
         */
        this.client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        /**
         * O MqttConnectOptions efine as regras da conversa durante o aperto de mão (handshake) do protocolo:
         * - options.setAutomaticReconnect(reconnect): Se o contêiner do Docker do Mosquitto for reiniciado, ou se o cabo de rede oscilar, o Paho não deixa a aplicação morrer.
         *      - Uma thread interna do Paho fica tentando restabelecer o canal TCP a cada poucos segundos em segundo plano (com espera exponencial).
         * - options.setCleanSession(cleanSession): Diz ao Mosquitto para esqueçer qualquer coisa do passado e começar uma sessão limpa do zero.
         *      - Se for false (sessão persistente), o Mosquitto ficaria acumulando mensagens na fila dele enquanto o Java estivesse desligado, o que poderia sobrecarregar o backend com milhares de leituras antigas de uma vez quando ele ligasse.
         * - options.setConnectionTimeout(timeout): É o tempo limite que o Java vai esperar o Mosquitto responder ao tentar se conectar pela primeira vez.
         * - options.setKeepAliveInterval(keepAlive): Se passarem 60 segundos, por exemplo, sem nenhuma leitura de sensor trafegando na rede, o Paho envia um pacote chamado PINGREQ para o Mosquitto, e o Mosquitto responde com PINGRESP.
         *      - Isso garante que os roteadores e firewalls não matem a conexão por inatividade e permite que o broker saiba se o cliente morreu.
         */
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(reconnect);
        options.setCleanSession(cleanSession);
        options.setConnectionTimeout(timeout);
        options.setKeepAliveInterval(keepAlive);

        if (username != null && !username.trim().isEmpty()) {
            options.setUserName(username);
        }
        if (password != null && !password.trim().isEmpty()) {
            options.setPassword(password.toCharArray());
        }

        if (brokerUrl != null && brokerUrl.startsWith("ssl://") && caCertPath != null && !caCertPath.trim().isEmpty()) {
            try {
                options.setSocketFactory(MqttSslUtils.getSocketFactory(caCertPath));
            } catch (Exception e) {
                throw new MqttException(new Exception("Failed to configure TLS with CA Certificate" + e.getMessage(), e));
            }
        }

        // Está dizendo ao Paho que quando chegar qualquer pacote da rede (mensagem, queda de conexão ou reconexão), ele deve chamar os métodos dessa class (this).
        this.client.setCallback(this);

        /**
         * 1. O Java abre o socket TCP com o endereço tcp://localhost:1883.
         * 2. Envia o pacote de controle MQTT CONNECT com todas as opções acima.
         * 3. Fica aguardando o pacote de resposta do Mosquitto (CONNACK).
         * 4. Se o Mosquitto aceitar, a conexão é concluída e o Paho dispara o método connectComplete dessa classe por causa da linha acima
         */
        this.client.connect(options);
    }

    /**
     * Sobrecarga de conveniência que inicia o subscriber com as configurações recomendadas para o CO2NNECT:
     * - Reconexão automática ativada (true)
     * - Sessão limpa (true)
     * - Timeout de conexão de 10 segundos
     * - Intervalo de Keep-Alive de 60 segundos
     * 
     * @throws MqttException repassa a exceção caso ocorra falha de conexão inicial com o Mosquitto
     */
    public void start() throws MqttException {
        start(true, true, 10, 60);
    }

    /**
     * Executa o Graceful Shutdown da conexão com o Broker.
     * 
     * Se o cliente estiver ativo e conectado:
     * 1. Envia um pacote de controle MQTT DISCONNECT para informar ao Mosquitto que a saída é voluntária
     *    (evitando que o broker interprete como queda inesperada ou dispare falso alarme de LWT).
     * 2. Fecha o canal de comunicação TCP e libera os buffers de memória do MemoryPersistence.
     */
    public void stop() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
                System.out.println("[MQTT] Client disconnected cleanly from broker.");
            }
        } catch (MqttException e) {
            System.err.println("[MQTT] Error while disconnecting: " + e.getMessage());
        }
    }

    // --- Métodos da Interface ---

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        if (reconnect) {
            System.out.println("[MQTT] Reconnected to broker: " + serverURI);
        } else {
            System.out.println("[MQTT] Connected to broker: " + serverURI);
        }

        try {
            /**
             * QoS (Quality of Service) no protocolo MQTT define a garantia de entrega da mensagem:
             * - QoS 0 (At most once): "Dispare e esqueça". Mais rápido, mas pode haver perda se a rede oscilar.
             * - QoS 1 (At least once): Entrega garantida com confirmação (PUBACK). Se houver instabilidade, 
             *   a mensagem é reenviada até ser confirmada. Ideal para a telemetria e alertas do CO2NNECT.
             * - QoS 2 (Exactly once): Garante entrega única sem duplicatas via handshake de 4 vias, 
             *   porém gera maior latência e overhead de rede.
             * 
             * Usamos QoS 1 para assegurar que nenhuma leitura crítica de CO2 seja perdida pelo Broker.
             */
            int[] qos = new int[topics.length];
            Arrays.fill(qos, 1);

            client.subscribe(topics, qos);
        } catch (MqttException e) {
            System.err.println("[MQTT] Failed to subscribe to topics " + Arrays.toString(topics) + ": " + e.getMessage());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // O MQTT transporta apenas um array de bytes (byte[]).
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        
        try {
            if (topic.endsWith("/telemetry")) {
                SensorReading reading = gson.fromJson(payload, SensorReading.class);
                // Fallback para caso o deviceId não venha no corpo do JSON
                if (reading != null && (reading.getDeviceId() == null || reading.getDeviceId().trim().isEmpty())) {
                    String[] parts = topic.split("/");
                    if (parts.length >= 2) {
                        reading.setDeviceId(parts[1]);
                    }
                }
                service.processReading(reading);
            } else if (topic.endsWith("/status")) {
                System.out.println("[STATUS] Device status update on topic '" + topic + "': " + payload);
                System.out.println();
                // TODO (CT3): Tratar se o sensor está online (1) ou caiu (0 via Last Will and Testament)
            } else {
                System.out.println("[MQTT] Received message on unhandled topic: " + topic);
                System.out.println();
            }
        } catch (Exception e) {
            // Se o JSON vier quebrado ou inválido, descarta e registra no log sem derrubar o cliente
            System.err.println("[MQTT] Discarded malformed payload on topic '" + topic + "': " + e.getMessage());
            System.err.println();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.err.println("[MQTT] Connection to broker lost: " + (cause != null ? cause.getMessage() : "Unknown cause"));
        System.out.println("[MQTT] Waiting for automatic reconnection...");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {}
}
