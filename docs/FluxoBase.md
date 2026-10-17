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
Leitura agora pode manusear os valores como bem quiser

-> Tendo Leitura pronta, é possível mandar seus valores para o Websocket e para o Postgre

## Interação Cliente-Servidor


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