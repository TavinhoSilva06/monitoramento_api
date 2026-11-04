package com.example.monitoramento_api.interfaces.web;

import com.example.monitoramento_api.application.MonitoredApiService;
import com.example.monitoramento_api.domain.ApiNameAlreadyExistsException;
import com.example.monitoramento_api.domain.ApiNotFoundException;
import com.example.monitoramento_api.domain.MonitoredApi;
import com.example.monitoramento_api.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MonitoredApiController.class)
@Import(SecurityConfig.class)
class MonitoredApiControllerTest {

	private static final String JSON_VALIDO = """
			{"name": "Pagamentos", "url": "https://api.exemplo.com/health", "intervalSeconds": 30, "expectedStatus": 200}
			""";

	@Autowired
	private MockMvc mvc;

	@MockitoBean
	private MonitoredApiService servico;

	@Test
	void criaApiRetorna201ComLocation() throws Exception {
		when(servico.criar(any())).thenReturn(api(1L, "Pagamentos"));

		mvc.perform(post("/apis").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/apis/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Pagamentos"))
				.andExpect(jsonPath("$.active").value(true));
	}

	@Test
	void rejeitaPayloadInvalidoComErrosPorCampo() throws Exception {
		String invalido = """
				{"name": " ", "url": "", "intervalSeconds": 5, "expectedStatus": 700}
				""";

		mvc.perform(post("/apis").contentType(MediaType.APPLICATION_JSON).content(invalido))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors.name").exists())
				.andExpect(jsonPath("$.errors.url").exists())
				.andExpect(jsonPath("$.errors.intervalSeconds").exists())
				.andExpect(jsonPath("$.errors.expectedStatus").exists());
	}

	@Test
	void urlSemProtocoloHttpRetorna400() throws Exception {
		when(servico.criar(any())).thenThrow(new IllegalArgumentException("A URL deve ser http ou https e conter um host"));
		String ftp = """
				{"name": "API", "url": "ftp://api.exemplo.com", "intervalSeconds": 30, "expectedStatus": 200}
				""";

		mvc.perform(post("/apis").contentType(MediaType.APPLICATION_JSON).content(ftp))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("A URL deve ser http ou https e conter um host"));
	}

	@Test
	void nomeDuplicadoRetorna409() throws Exception {
		when(servico.criar(any())).thenThrow(new ApiNameAlreadyExistsException("Pagamentos"));

		mvc.perform(post("/apis").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
				.andExpect(status().isConflict());
	}

	@Test
	void buscaApiInexistenteRetorna404() throws Exception {
		when(servico.buscar(9L)).thenThrow(new ApiNotFoundException(9L));

		mvc.perform(get("/apis/9"))
				.andExpect(status().isNotFound());
	}

	@Test
	void listaApis() throws Exception {
		when(servico.listar()).thenReturn(List.of(api(1L, "Pagamentos")));

		mvc.perform(get("/apis"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Pagamentos"));
	}

	@Test
	void atualizaApi() throws Exception {
		when(servico.atualizar(eq(1L), any())).thenReturn(api(1L, "Pagamentos"));

		mvc.perform(put("/apis/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1));
	}

	@Test
	void removeApiRetorna204() throws Exception {
		doNothing().when(servico).remover(1L);

		mvc.perform(delete("/apis/1"))
				.andExpect(status().isNoContent());
		verify(servico).remover(1L);
	}

	private static MonitoredApi api(Long id, String nome) {
		return new MonitoredApi(id, nome, "https://api.exemplo.com/health", 30, 200, true, Instant.now());
	}

}
