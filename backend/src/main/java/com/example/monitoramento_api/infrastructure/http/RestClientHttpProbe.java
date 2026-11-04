package com.example.monitoramento_api.infrastructure.http;

import com.example.monitoramento_api.application.port.HttpProbe;
import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.MonitoredApi;
import com.example.monitoramento_api.infrastructure.config.MonitoramentoProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;

@Component
class RestClientHttpProbe implements HttpProbe {

	private static final int TAMANHO_MAXIMO_ERRO = 500;

	private final RestClient restClient;

	RestClientHttpProbe(MonitoramentoProperties propriedades) {
		MonitoramentoProperties.Checagem checagem = propriedades.checagem();
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(checagem.timeoutConexaoMs()))
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(Duration.ofMillis(checagem.timeoutLeituraMs()));
		this.restClient = RestClient.builder()
				.requestFactory(requestFactory)
				.build();
	}

	@Override
	public HealthCheck probe(MonitoredApi api) {
		long inicio = System.nanoTime();
		try {
			int status = restClient.get()
					.uri(URI.create(api.url()))
					.exchange((request, response) -> response.getStatusCode().value());
			boolean sucesso = status == api.expectedStatus();
			String erro = sucesso ? null : "Status esperado " + api.expectedStatus() + ", recebido " + status;
			return new HealthCheck(null, api.id(), status, tempoDesde(inicio), sucesso, erro, Instant.now());
		} catch (RestClientException e) {
			return new HealthCheck(null, api.id(), null, tempoDesde(inicio), false, mensagem(e), Instant.now());
		}
	}

	private static long tempoDesde(long inicio) {
		return (System.nanoTime() - inicio) / 1_000_000;
	}

	private static String mensagem(RestClientException e) {
		String texto = e.getClass().getSimpleName() + ": " + e.getMessage();
		return texto.length() > TAMANHO_MAXIMO_ERRO ? texto.substring(0, TAMANHO_MAXIMO_ERRO) : texto;
	}

}
