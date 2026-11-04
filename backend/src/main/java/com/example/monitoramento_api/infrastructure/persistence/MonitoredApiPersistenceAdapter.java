package com.example.monitoramento_api.infrastructure.persistence;

import com.example.monitoramento_api.application.port.MonitoredApiRepository;
import com.example.monitoramento_api.domain.ApiNotFoundException;
import com.example.monitoramento_api.domain.MonitoredApi;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class MonitoredApiPersistenceAdapter implements MonitoredApiRepository {

	private final MonitoredApiJpaRepository jpa;

	MonitoredApiPersistenceAdapter(MonitoredApiJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public MonitoredApi save(MonitoredApi api) {
		MonitoredApiEntity entity = api.id() == null
				? new MonitoredApiEntity()
				: jpa.findById(api.id()).orElseThrow(() -> new ApiNotFoundException(api.id()));
		entity.setName(api.name());
		entity.setUrl(api.url());
		entity.setIntervalSeconds(api.intervalSeconds());
		entity.setExpectedStatus(api.expectedStatus());
		entity.setActive(api.active());
		entity.setCreatedAt(api.createdAt());
		return toDomain(jpa.save(entity));
	}

	@Override
	public Optional<MonitoredApi> findById(Long id) {
		return jpa.findById(id).map(MonitoredApiPersistenceAdapter::toDomain);
	}

	@Override
	public Optional<MonitoredApi> findByName(String name) {
		return jpa.findByName(name).map(MonitoredApiPersistenceAdapter::toDomain);
	}

	@Override
	public List<MonitoredApi> findAll() {
		return jpa.findAll().stream().map(MonitoredApiPersistenceAdapter::toDomain).toList();
	}

	@Override
	public List<MonitoredApi> findAllActive() {
		return jpa.findByActiveTrue().stream().map(MonitoredApiPersistenceAdapter::toDomain).toList();
	}

	@Override
	public void deleteById(Long id) {
		jpa.deleteById(id);
	}

	private static MonitoredApi toDomain(MonitoredApiEntity entity) {
		return new MonitoredApi(
				entity.getId(),
				entity.getName(),
				entity.getUrl(),
				entity.getIntervalSeconds(),
				entity.getExpectedStatus(),
				entity.isActive(),
				entity.getCreatedAt());
	}

}
