package com.example.monitoramento_api.infrastructure.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

interface HealthCheckJpaRepository extends JpaRepository<HealthCheckEntity, Long> {

	List<HealthCheckEntity> findByApiIdAndCheckedAtGreaterThanEqualAndCheckedAtLessThanOrderByCheckedAtDesc(
			Long apiId, Instant from, Instant to);

	List<HealthCheckEntity> findByApiIdOrderByCheckedAtDesc(Long apiId, Pageable pageable);

	@Modifying
	@Query("delete from HealthCheckEntity h where h.checkedAt < :limite")
	int apagarAnterioresA(@Param("limite") Instant limite);

}
