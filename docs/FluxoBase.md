# Fluxos

## Índice:
- Fluxo resumido
- Cliente-Servidor
- MQTT

## Fluxo resumido

ESP32 pega valores medidos -> 
Publica os valores via MQTT ->
Classe LeitorMqtt pega a publicação via método messageArrived() -> 
Os valores de messageArrived() são passados para uma instância de Leitura ->
Leitura agora pode manusear os valores como bem quiser ->
Tendo Leitura pronta, é possível mandar seus valores para o Servidor ->
Servidor pega a Leitura e manda para o Websocket e Banco ->
Websocket pode mandar pedidos para o Servidor relacionado ao Banco ->
Servidor devolve o que for pedido

## Interação Cliente-Servidor

O Cliente representa o ESP32, que enviará postará mensagens no canal do Broker, com isso, a classe LeitorMqtt irá ler a mensagem postada e converte-la em um objeto de Leitura, que será enfileirado em uma BlockingQueue (que é uma fila que funciona para todas as possíveis threads ao mesmo tempo [aqui, threads seriam basicamente os ESP32, caso houvessem mais, a fila receberia as informações de todos]), com o BlockingQueue, é possível que Cliente acesse as Leituras enfileiradas e as envie diretamente para o Servidor, que fará suas tratativas para se comunicar com os outros extremos

Então ficou

### Servidor

- Recebe as Leituras do Cliente
- Servidor usa os valores para adicionar as informações ao banco
- Manda as informações para o front (Dashboard ou Resultados de requisições de banco)

### Cliente

- Recebe as informações do ESP32 via LeitorMqtt
- LeitorMqtt instância a classe Leitura de acordo com os valores recebidos
- Manda as informações para o Servidor.


## MQTT

Resumo do fluxo completo
1. O sensor conectado ao ESP32 realiza uma medição.
2. O ESP32 monta o JSON e publica a mensagem em ar/sala01/leituras.
3. O Mosquitto recebe a mensagem e a encaminha aos clientes inscritos.
4. O método messageArrived() da classe LeitorMqtt é acionado.
5. O método Leitura.pegaLeitura() interpreta o JSON e cria um objeto Leitura, executando as validações do construtor.
6. O servidor Java utiliza os dados desse objeto para executar um INSERT parametrizado no PostgreSQL.


- inicia com compose.yaml
services:
  mqtt:
    image: eclipse-mosquitto:2
    container_name: broker-mqtt
    ports:
      - "1883:1883"
    volumes:
      - ./mosquitto/config:/mosquitto/config

- Adição das bibliotecas no pom.xml
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
    <version>1.2.5</version>
</dependency>

<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.13.1</version>
</dependency>

- Para usos do mqtt, use estes imports
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;