package br.puc.campinas.co2nect;

/**
 * Payload comum recebido do ESP32 por MQTT ou por WebSocket.
 *
 * Os nomes dos campos seguem o contrato de telemetria do projeto:
 * deviceId, co2, temperature, humidity e measuredAt.
 */
public class SensorData {
    public String deviceId;
    public Integer co2;
    public Double temperature;
    public Double humidity;
    public String measuredAt;

    public SensorData() {
        // Necessário para a desserialização JSON do Gson.
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getCo2() {
        return co2;
    }

    public void setCo2(Integer co2) {
        this.co2 = co2;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public String getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(String measuredAt) {
        this.measuredAt = measuredAt;
    }
}
