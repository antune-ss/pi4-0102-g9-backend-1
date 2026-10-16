
package br.puc.campinas.co2nect.service;

import br.puc.campinas.co2nect.model.SensorData;
import br.puc.campinas.co2nect.repository.ReadingRepository;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

public class ReadingService {

    private final Gson gson = new Gson();
    private final ReadingRepository repository;

    public ReadingService(ReadingRepository repository) {
        this.repository = repository;
    }

    public void processar(String json) {
        try {
            SensorData data =
                gson.fromJson(json, SensorData.class);

            validar(data);
            repository.save(data);

            System.out.println(
                "[SERVICE] Leitura processada com sucesso."
            );

        } catch (JsonParseException e) {
            System.err.println(
                "[ERRO] INVALID_JSON: " + e.getMessage()
            );

        } catch (IllegalArgumentException e) {
            System.err.println(
                "[ERRO] INVALID_READING: " + e.getMessage()
            );

        } catch (Exception e) {
            System.err.println(
                "[ERRO] Falha no armazenamento: " +
                e.getMessage()
            );
        }
    }

    private void validar(SensorData data) {

        if (data == null ||
            data.deviceId == null ||
            data.deviceId.isBlank() ||
            data.co2 == null ||
            data.temperature == null ||
            data.humidity == null ||
            data.measuredAt == null ||
            data.measuredAt.isBlank()) {

            throw new IllegalArgumentException(
                "Campos obrigatorios ausentes."
            );
        }

        if (data.co2 < 0 ||
            data.temperature < 0 ||
            data.humidity < 0 ||
            !Double.isFinite(data.temperature) ||
            !Double.isFinite(data.humidity)) {

            throw new IllegalArgumentException(
                "Valores negativos ou invalidos."
            );
        }

        try {
            LocalDateTime.parse(data.measuredAt);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                "Data measuredAt invalida."
            );
        }
    }
}
