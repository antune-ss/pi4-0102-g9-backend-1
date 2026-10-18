package br.puc.campinas.co2nect.model;

import java.time.LocalDateTime;

/**
 * Representa os dados de uma medição física coletada pelo sensor (SCD30).
 * 
 * Atua como um DTO (Data Transfer Object) entre:
 * - O contrato de entrada MQTT: CT1 (publishReading)
 * - O contrato de persistência no PostgreSQL: CT2 (sensor_reading)
 */
public class SensorReading {

    // Identificador único do dispositivo que realizou a leitura (ex: "ESP32-SALA-01")
    private String deviceId;
    // Concentração de CO2 medida em partes por milhão (ppm) - número inteiro conforme CT1 e CT2
    private int co2;
    // Temperatura ambiente em graus Celsius (°C)
    private double temperature;
    // Umidade relativa do ar em porcentagem (%)
    private double humidity;
    // Instante da leitura. Usa LocalDateTime por ser imutável e compatível com o TIMESTAMP do SQL
    private LocalDateTime measuredAt;

    /**
     * Construtor completo para instanciação manual da leitura.
     * 
     * @param deviceId    identificador do ESP32
     * @param co2         nível de CO2 em ppm
     * @param temperature temperatura em °C
     * @param humidity    umidade relativa em %
     * @param measuredAt  data e hora da medição (se nulo, assume a data/hora atual do servidor)
     */
    public SensorReading(String deviceId, int co2, double temperature, double humidity, LocalDateTime measuredAt) {
        this.deviceId = deviceId;
        this.co2 = co2;
        this.temperature = temperature;
        this.humidity = humidity;

        // Se measuredAt vier nulo (ex: ESP32 sem módulo RTC/NTP), assume-se o instante de recepção no servidor
        this.measuredAt = (measuredAt != null) ? measuredAt : LocalDateTime.now();
    }

    /**
     * Construtor padrão sem argumentos.
     * 
     * Essencial para bibliotecas de serialização/desserialização (como o Google Gson),
     * que utilizam o mecanismo de Reflexão do Java para instanciar a classe antes de popular os campos.
     */
    public SensorReading() {
        // Inicializa por padrão com a data/hora atual para garantir que o campo nunca fique nulo
        this.measuredAt = LocalDateTime.now();
    }

    // --- Getters e Setters ---

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getCo2() {
        return co2;
    }

    public void setCo2(int co2) {
        this.co2 = co2;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public LocalDateTime getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(LocalDateTime measuredAt) {
        this.measuredAt = measuredAt;
    }



    @Override
    public String toString() {
        return "{\n" +
               "  \"deviceId\": \"" + deviceId + "\",\n" +
               "  \"co2\": " + co2 + ",\n" +
               "  \"temperature\": " + temperature + ",\n" +
               "  \"humidity\": " + humidity + ",\n" +
               "  \"measuredAt\": \"" + measuredAt + "\"\n" +
               "}";
    }
}

