package com.example.monitoramento_api;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * PostgreSQL real em container para os testes de integracao. Mesma versao do compose local.
 * O Spring liga o datasource ao container pelo {@link ServiceConnection}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfig {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgres() {
		return new PostgreSQLContainer("postgres:18");
	}

}
