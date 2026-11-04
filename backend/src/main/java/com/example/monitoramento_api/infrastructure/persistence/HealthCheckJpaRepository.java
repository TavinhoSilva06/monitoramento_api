package com.example.monitoramento_api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

interface HealthCheckJpaRepository extends JpaRepository<HealthCheckEntity, Long> {

	List<HealthCheckEntity> findByApiIdAndCheckedAtAfterOrderByCheckedAtDesc(Long apiId, Instant since);

}
