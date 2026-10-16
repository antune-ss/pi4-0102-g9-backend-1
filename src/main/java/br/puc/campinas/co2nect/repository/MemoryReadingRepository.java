
package br.puc.campinas.co2nect.repository;

import br.puc.campinas.co2nect.model.SensorData;
import java.util.ArrayList;
import java.util.List;

public class MemoryReadingRepository
        implements ReadingRepository {

    private final List<SensorData> readings =
            new ArrayList<>();

    @Override
    public synchronized void save(SensorData data) {
        readings.add(data);

        System.out.println(
            "[MEMORIA] Leitura salva: " + data
        );

        System.out.println(
            "[MEMORIA] Total de leituras: " +
            readings.size()
        );
    }

    public synchronized List<SensorData> findAll() {
        return new ArrayList<>(readings);
    }
}

