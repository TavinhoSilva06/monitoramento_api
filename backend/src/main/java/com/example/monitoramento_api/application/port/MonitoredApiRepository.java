package com.example.monitoramento_api.application.port;

import com.example.monitoramento_api.domain.MonitoredApi;

import java.util.List;
import java.util.Optional;

public interface MonitoredApiRepository {

	MonitoredApi save(MonitoredApi api);

	Optional<MonitoredApi> findById(Long id);

	Optional<MonitoredApi> findByName(String name);

	List<MonitoredApi> findAll();

	List<MonitoredApi> findAllActive();

	void deleteById(Long id);

}
