package com.example.monitoramento_api;

import com.sun.net.httpserver.HttpServer;
import com.example.monitoramento_api.application.HealthCheckService;
import com.example.monitoramento_api.application.MonitoredApiService;
import com.example.monitoramento_api.domain.HealthStatus;
import com.example.monitoramento_api.domain.MonitoredApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo com PostgreSQL real (Testcontainers) e checagens HTTP reais contra um servidor local:
 * cadastra a API, roda checagens, o estado muda, aparece no status e no resumo, e sobrevive a um "reinicio".
 * O agendador fica com o ciclo longo, e as checagens sao disparadas manualmente para o teste ser deterministico.
 */
@SpringBootTest(properties = "monitoramento.checagem.intervalo-ciclo-ms=3600000")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ContainersConfig.class)
class FluxoMonitoramentoTest {

	private static final Pattern ID = Pattern.compile("\"id\":(\\d+)");

	@Autowired
	private MockMvc mvc;

	@Autowired
	private MonitoredApiService apis;

	@Autowired
	private HealthCheckService saude;

	private HttpServer alvo;
	private final AtomicInteger statusDaAlvo = new AtomicInteger(500);

	@BeforeEach
	void subirApiAlvo() throws IOException {
		alvo = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		alvo.createContext("/health", exchange -> {
			exchange.sendResponseHeaders(statusDaAlvo.get(), -1);
			exchange.close();
		});
		alvo.start();
	}

	@AfterEach
	void derrubarApiAlvo() {
		alvo.stop(0);
	}

	@Test
	void cadastraChecaMudaEstadoEAparecePeloPainel() throws Exception {
		String url = "http://localhost:" + alvo.getAddress().getPort() + "/health";
		MvcResult criada = mvc.perform(post("/apis").contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Fluxo\",\"url\":\"" + url + "\",\"intervalSeconds\":10,\"expectedStatus\":200}"))
				.andExpect(status().isCreated())
				.andReturn();
		Matcher id = ID.matcher(criada.getResponse().getContentAsString());
		assertThat(id.find()).isTrue();
		long apiId = Long.parseLong(id.group(1));
		MonitoredApi api = apis.buscar(apiId);

		// Alvo respondendo 500: tres falhas seguidas derrubam a API
		for (int i = 0; i < 3; i++) {
			saude.checar(api);
		}
		mvc.perform(get("/apis/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == " + apiId + ")].estado").value(org.hamcrest.Matchers.contains("DOWN")));
		mvc.perform(get("/apis/" + apiId + "/checks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(3));
		mvc.perform(get("/apis/" + apiId + "/summary"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalChecks").value(3))
				.andExpect(jsonPath("$.uptimePercent").value(0.0));

		// Simula a perda do estado em memoria (reinicio): o estado volta a partir do historico gravado
		saude.esquecer(apiId);
		assertThat(saude.estadoAtual(apiId)).isEqualTo(HealthStatus.UNKNOWN);
		saude.restaurar(api);
		assertThat(saude.estadoAtual(apiId)).isEqualTo(HealthStatus.DOWN);

		// Alvo recuperado: uma checagem com sucesso volta a API para UP
		statusDaAlvo.set(200);
		saude.checar(api);
		assertThat(saude.estadoAtual(apiId)).isEqualTo(HealthStatus.UP);
		mvc.perform(get("/apis/" + apiId + "/summary"))
				.andExpect(jsonPath("$.totalChecks").value(4))
				.andExpect(jsonPath("$.uptimePercent").value(25.0));
	}

}
