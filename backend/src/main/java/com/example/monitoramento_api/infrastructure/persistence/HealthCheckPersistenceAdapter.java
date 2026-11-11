package com.example.monitoramento_api.infrastructure.persistence;

import com.example.monitoramento_api.application.port.HealthCheckRepository;
import com.example.monitoramento_api.domain.HealthCheck;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
class HealthCheckPersistenceAdapter implements HealthCheckRepository {

	private final HealthCheckJpaRepository jpa;
	private final MonitoredApiJpaRepository apis;

	HealthCheckPersistenceAdapter(HealthCheckJpaRepository jpa, MonitoredApiJpaRepository apis) {
		this.jpa = jpa;
		this.apis = apis;
	}

	@Override
	public HealthCheck save(HealthCheck check) {
		HealthCheckEntity entity = new HealthCheckEntity();
		entity.setApi(apis.getReferenceById(check.apiId()));
		entity.setStatusCode(check.statusCode());
		entity.setResponseTimeMs(check.responseTimeMs());
		entity.setSuccess(check.success());
		entity.setErrorMessage(check.errorMessage());
		entity.setCheckedAt(check.checkedAt());
		return toDomain(jpa.save(entity));
	}

	@Override
	@Transactional(readOnly = true)
	public List<HealthCheck> findEntre(Long apiId, Instant from, Instant to) {
		return jpa.findByApiIdAndCheckedAtGreaterThanEqualAndCheckedAtLessThanOrderByCheckedAtDesc(apiId, from, to).stream()
				.map(HealthCheckPersistenceAdapter::toDomain)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<HealthCheck> findUltimas(Long apiId, int limite) {
		return jpa.findByApiIdOrderByCheckedAtDesc(apiId, PageRequest.of(0, limite)).stream()
				.map(HealthCheckPersistenceAdapter::toDomain)
				.toList();
	}

	@Override
	@Transactional
	public int apagarAnterioresA(Instant limite) {
		return jpa.apagarAnterioresA(limite);
	}

	private static HealthCheck toDomain(HealthCheckEntity entity) {
		return new HealthCheck(
				entity.getId(),
				entity.getApi().getId(),
				entity.getStatusCode(),
				entity.getResponseTimeMs(),
				entity.isSuccess(),
				entity.getErrorMessage(),
				entity.getCheckedAt());
	}

}
