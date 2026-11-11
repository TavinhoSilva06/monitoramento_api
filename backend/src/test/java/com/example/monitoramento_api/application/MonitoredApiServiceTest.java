package com.example.monitoramento_api.application;

import com.example.monitoramento_api.application.port.MonitoredApiRepository;
import com.example.monitoramento_api.domain.ApiNameAlreadyExistsException;
import com.example.monitoramento_api.domain.ApiNotFoundException;
import com.example.monitoramento_api.domain.MonitoredApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonitoredApiServiceTest {

	private MonitoredApiRepository repositorio;
	private HealthCheckService saude;
	private MonitoredApiService servico;

	@BeforeEach
	void setUp() {
		repositorio = mock(MonitoredApiRepository.class);
		saude = mock(HealthCheckService.class);
		servico = new MonitoredApiService(repositorio, saude);
	}

	@Test
	void criaApiComNomeLivre() {
		when(repositorio.findByName("Pagamentos")).thenReturn(Optional.empty());
		when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));

		MonitoredApi criada = servico.criar(comando("Pagamentos"));

		assertThat(criada.name()).isEqualTo("Pagamentos");
		assertThat(criada.active()).isTrue();
		verify(repositorio).save(any());
	}

	@Test
	void rejeitaNomeJaCadastradoNaCriacao() {
		when(repositorio.findByName("Pagamentos")).thenReturn(Optional.of(api(2L, "Pagamentos")));

		assertThatThrownBy(() -> servico.criar(comando("Pagamentos")))
				.isInstanceOf(ApiNameAlreadyExistsException.class);
		verify(repositorio, never()).save(any());
	}

	@Test
	void atualizaComOProprioNome() {
		when(repositorio.findById(1L)).thenReturn(Optional.of(api(1L, "Pagamentos")));
		when(repositorio.findByName("Pagamentos")).thenReturn(Optional.of(api(1L, "Pagamentos")));
		when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));

		MonitoredApi atualizada = servico.atualizar(1L, comando("Pagamentos"));

		assertThat(atualizada.id()).isEqualTo(1L);
		verify(repositorio).save(any());
	}

	@Test
	void rejeitaNomeDeOutraApiNaAtualizacao() {
		when(repositorio.findById(1L)).thenReturn(Optional.of(api(1L, "Pagamentos")));
		when(repositorio.findByName("Cobrancas")).thenReturn(Optional.of(api(2L, "Cobrancas")));

		assertThatThrownBy(() -> servico.atualizar(1L, comando("Cobrancas")))
				.isInstanceOf(ApiNameAlreadyExistsException.class);
		verify(repositorio, never()).save(any());
	}

	@Test
	void preservaDataDeCriacaoNaAtualizacao() {
		MonitoredApi original = api(1L, "Pagamentos");
		when(repositorio.findById(1L)).thenReturn(Optional.of(original));
		when(repositorio.findByName(any())).thenReturn(Optional.empty());
		when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));

		MonitoredApi atualizada = servico.atualizar(1L, comando("Pagamentos"));

		assertThat(atualizada.createdAt()).isEqualTo(original.createdAt());
	}

	@Test
	void buscaApiInexistenteLancaNaoEncontrada() {
		when(repositorio.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servico.buscar(9L)).isInstanceOf(ApiNotFoundException.class);
	}

	@Test
	void removeApiExistente() {
		when(repositorio.findById(1L)).thenReturn(Optional.of(api(1L, "Pagamentos")));

		servico.remover(1L);

		verify(repositorio).deleteById(1L);
		verify(saude).esquecer(1L);
	}

	@Test
	void removeApiInexistenteLancaNaoEncontrada() {
		when(repositorio.findById(9L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> servico.remover(9L)).isInstanceOf(ApiNotFoundException.class);
		verify(repositorio, never()).deleteById(any());
	}

	private static ApiCommand comando(String nome) {
		return new ApiCommand(nome, "https://api.exemplo.com/health", 30, 200, true);
	}

	private static MonitoredApi api(Long id, String nome) {
		return new MonitoredApi(id, nome, "https://api.exemplo.com/health", 30, 200, true, Instant.now());
	}

}
