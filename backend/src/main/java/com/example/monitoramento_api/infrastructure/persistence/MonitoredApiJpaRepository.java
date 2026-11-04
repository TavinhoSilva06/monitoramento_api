package com.example.monitoramento_api.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface MonitoredApiJpaRepository extends JpaRepository<MonitoredApiEntity, Long> {

	Optional<MonitoredApiEntity> findByName(String name);

	List<MonitoredApiEntity> findByActiveTrue();

}
