package com.example.monitoramento_api.infrastructure.http;

import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.MonitoredApi;
import com.example.monitoramento_api.infrastructure.config.MonitoramentoProperties;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientHttpProbeTest {

	private final MonitoramentoProperties propriedades = new MonitoramentoProperties(
			new MonitoramentoProperties.Alerta(3),
			new MonitoramentoProperties.Checagem(3000, 5000, 5000, 10));

	private final RestClientHttpProbe probe = new RestClientHttpProbe(propriedades);

	@Test
	void portaFechadaViraFalhaComACausaReal() throws IOException {
		int portaLivre;
		try (ServerSocket socket = new ServerSocket(0)) {
			portaLivre = socket.getLocalPort();
		}
		MonitoredApi api = new MonitoredApi(1L, "Fechada", "http://localhost:" + portaLivre, 30, 200, true, Instant.now());

		HealthCheck resultado = probe.probe(api);

		assertThat(resultado.success()).isFalse();
		assertThat(resultado.statusCode()).isNull();
		// Depende da pilha de rede do SO: ConnectException ou ClosedChannelException
		assertThat(resultado.errorMessage()).matches("(ConnectException|ClosedChannelException).*");
	}

}
