
package br.puc.campinas.co2nect.repository;

import br.puc.campinas.co2nect.model.SensorData;

public interface ReadingRepository {

    void save(SensorData data) throws Exception;
}

