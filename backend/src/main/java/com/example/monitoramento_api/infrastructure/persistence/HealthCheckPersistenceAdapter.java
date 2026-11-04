package com.example.monitoramento_api.infrastructure.persistence;

import com.example.monitoramento_api.application.port.HealthCheckRepository;
import com.example.monitoramento_api.domain.HealthCheck;
import org.springframework.stereotype.Component;

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
	public List<HealthCheck> findByApiSince(Long apiId, Instant since) {
		return jpa.findByApiIdAndCheckedAtAfterOrderByCheckedAtDesc(apiId, since).stream()
				.map(HealthCheckPersistenceAdapter::toDomain)
				.toList();
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
