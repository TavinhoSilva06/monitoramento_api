package com.example.monitoramento_api.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonitoredApiTest {

	@Test
	void aceitaDadosValidos() {
		assertThatCode(() -> novaApi("Pagamentos", "https://api.exemplo.com/health", 30, 200))
				.doesNotThrowAnyException();
	}

	@Test
	void rejeitaNomeVazio() {
		assertThatThrownBy(() -> novaApi(" ", "https://api.exemplo.com", 30, 200))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejeitaUrlSemProtocoloHttp() {
		assertThatThrownBy(() -> novaApi("API", "ftp://api.exemplo.com", 30, 200))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejeitaUrlSemHost() {
		assertThatThrownBy(() -> novaApi("API", "http://", 30, 200))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejeitaIntervaloAbaixoDoMinimo() {
		assertThatThrownBy(() -> novaApi("API", "https://api.exemplo.com", MonitoredApi.MIN_INTERVAL_SECONDS - 1, 200))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejeitaStatusEsperadoForaDaFaixaHttp() {
		assertThatThrownBy(() -> novaApi("API", "https://api.exemplo.com", 30, 600))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private MonitoredApi novaApi(String name, String url, int intervalSeconds, int expectedStatus) {
		return new MonitoredApi(null, name, url, intervalSeconds, expectedStatus, true, Instant.now());
	}

}
