package com.example.monitoramento_api.application;

import com.example.monitoramento_api.application.port.MonitoredApiRepository;
import com.example.monitoramento_api.domain.ApiNameAlreadyExistsException;
import com.example.monitoramento_api.domain.ApiNotFoundException;
import com.example.monitoramento_api.domain.MonitoredApi;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class MonitoredApiService {

	private final MonitoredApiRepository apis;

	public MonitoredApiService(MonitoredApiRepository apis) {
		this.apis = apis;
	}

	public MonitoredApi criar(ApiCommand comando) {
		MonitoredApi api = new MonitoredApi(
				null,
				comando.name(),
				comando.url(),
				comando.intervalSeconds(),
				comando.expectedStatus(),
				comando.active(),
				Instant.now());
		garantirNomeLivre(api.name(), null);
		return apis.save(api);
	}

	public List<MonitoredApi> listar() {
		return apis.findAll();
	}

	public List<MonitoredApi> listarAtivas() {
		return apis.findAllActive();
	}

	public MonitoredApi buscar(Long id) {
		return apis.findById(id).orElseThrow(() -> new ApiNotFoundException(id));
	}

	public MonitoredApi atualizar(Long id, ApiCommand comando) {
		MonitoredApi atual = buscar(id);
		MonitoredApi atualizada = new MonitoredApi(
				id,
				comando.name(),
				comando.url(),
				comando.intervalSeconds(),
				comando.expectedStatus(),
				comando.active(),
				atual.createdAt());
		garantirNomeLivre(atualizada.name(), id);
		return apis.save(atualizada);
	}

	public void remover(Long id) {
		buscar(id);
		apis.deleteById(id);
	}

	private void garantirNomeLivre(String nome, Long idAtual) {
		apis.findByName(nome)
				.filter(existente -> !existente.id().equals(idAtual))
				.ifPresent(existente -> {
					throw new ApiNameAlreadyExistsException(nome);
				});
	}

}
