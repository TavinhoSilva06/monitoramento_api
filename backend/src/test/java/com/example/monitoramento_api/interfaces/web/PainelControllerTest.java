package com.example.monitoramento_api.interfaces.web;

import com.example.monitoramento_api.application.ApiStatus;
import com.example.monitoramento_api.application.ConsultaPainelService;
import com.example.monitoramento_api.domain.ApiNotFoundException;
import com.example.monitoramento_api.domain.ApiSummary;
import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.HealthStatus;
import com.example.monitoramento_api.domain.MonitoredApi;
import com.example.monitoramento_api.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PainelController.class)
@Import(SecurityConfig.class)
class PainelControllerTest {

	private static final Instant AGORA = Instant.parse("2026-01-10T12:00:00Z");

	private final MonitoredApi api = new MonitoredApi(1L, "Pagamentos", "https://api.exemplo.com/health", 30, 200, true, AGORA);

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private ConsultaPainelService consultas;

	@Test
	void statusListaEstadoEUltimaChecagem() throws Exception {
		HealthCheck ultima = new HealthCheck(9L, 1L, 500, 80L, false, "Status esperado 200, recebido 500", AGORA);
		when(consultas.status()).thenReturn(List.of(new ApiStatus(api, HealthStatus.DEGRADED, ultima)));

		mvc.perform(get("/apis/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].estado").value("DEGRADED"))
				.andExpect(jsonPath("$[0].ultimaChecagem.statusCode").value(500))
				.andExpect(jsonPath("$[0].ultimaChecagem.success").value(false));
	}

	@Test
	void apiSemChecagemTemUltimaChecagemNula() throws Exception {
		when(consultas.status()).thenReturn(List.of(new ApiStatus(api, HealthStatus.UNKNOWN, null)));

		mvc.perform(get("/apis/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].estado").value("UNKNOWN"))
				.andExpect(jsonPath("$[0].ultimaChecagem").doesNotExist());
	}

	@Test
	void checagensAceitamPeriodoEmIsoUtc() throws Exception {
		when(consultas.checagens(eq(1L), any(), any())).thenReturn(List.of(
				new HealthCheck(1L, 1L, 200, 42L, true, null, AGORA)));

		mvc.perform(get("/apis/1/checks").param("from", "2026-01-09T00:00:00Z").param("to", "2026-01-10T00:00:00Z"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].responseTimeMs").value(42));
	}

	@Test
	void resumoRetornaUptimeELatencia() throws Exception {
		ApiSummary resumo = new ApiSummary(1L, AGORA.minusSeconds(3600), AGORA, 4, 3, 75.0, 120.5, 300L);
		when(consultas.resumo(eq(1L), any(), any())).thenReturn(resumo);

		mvc.perform(get("/apis/1/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.uptimePercent").value(75.0))
				.andExpect(jsonPath("$.averageLatencyMs").value(120.5))
				.andExpect(jsonPath("$.p95LatencyMs").value(300));
	}

	@Test
	void apiInexistenteRetorna404() throws Exception {
		when(consultas.resumo(eq(9L), any(), any())).thenThrow(new ApiNotFoundException(9L));

		mvc.perform(get("/apis/9/summary"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("API monitorada nao encontrada: 9"));
	}

	@Test
	void periodoInvalidoRetorna400() throws Exception {
		when(consultas.checagens(eq(1L), any(), any()))
				.thenThrow(new IllegalArgumentException("O parametro 'from' deve ser anterior a 'to'"));

		mvc.perform(get("/apis/1/checks").param("from", "2026-01-10T00:00:00Z").param("to", "2026-01-09T00:00:00Z"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("O parametro 'from' deve ser anterior a 'to'"));
	}

	@Test
	void preflightDoCorsLiberaAOrigemDoAngular() throws Exception {
		mvc.perform(options("/apis/status")
						.header("Origin", "http://localhost:4200")
						.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
	}

	@Test
	void origemNaoPermitidaNaoRecebeCabecalhoDeCors() throws Exception {
		mvc.perform(options("/apis/status")
						.header("Origin", "http://site-estranho.exemplo.com")
						.header("Access-Control-Request-Method", "GET"))
				.andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

}
