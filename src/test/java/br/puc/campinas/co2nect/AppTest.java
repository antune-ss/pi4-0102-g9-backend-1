package br.puc.campinas.co2nect;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Teste básico do DTO utilizado na desserialização das mensagens do sensor. */
public class AppTest {
    @Test
    public void sensorDataArmazenaOsCamposDaLeitura() {
        SensorData data = new SensorData();
        data.setDeviceId("ESP32-SALA-01");
        data.setCo2(850);
        data.setTemperature(24.5);
        data.setHumidity(45.2);
        data.setMeasuredAt("2026-11-04T18:30:00-03:00");

        assertEquals("ESP32-SALA-01", data.getDeviceId());
        assertEquals(Integer.valueOf(850), data.getCo2());
        assertEquals(Double.valueOf(24.5), data.getTemperature());
        assertEquals(Double.valueOf(45.2), data.getHumidity());
        assertEquals("2026-11-04T18:30:00-03:00", data.getMeasuredAt());
    }
}
