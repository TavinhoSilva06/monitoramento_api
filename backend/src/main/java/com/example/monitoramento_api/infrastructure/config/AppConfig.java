package com.example.monitoramento_api.infrastructure.config;

import com.example.monitoramento_api.domain.AlertPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(MonitoramentoProperties.class)
public class AppConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean(destroyMethod = "close")
	ExecutorService checagemExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

	@Bean
	AlertPolicy alertPolicy(MonitoramentoProperties propriedades) {
		return new AlertPolicy(propriedades.alerta().falhasParaDown());
	}

}
