package br.puc.campinas.co2nect.model;

public class SensorData {

    public String deviceId;
    public Integer co2;
    public Double temperature;
    public Double humidity;
    public String measuredAt;

    @Override
    public String toString() {
        return "SensorData{" +
                "deviceId='" + deviceId + '\'' +
                ", co2=" + co2 +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", measuredAt='" + measuredAt + '\'' +
                '}';
    }
}