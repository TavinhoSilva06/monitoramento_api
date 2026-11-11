package com.example.monitoramento_api.infrastructure.persistence;

import com.example.monitoramento_api.ContainersConfig;
import com.example.monitoramento_api.domain.HealthCheck;
import com.example.monitoramento_api.domain.MonitoredApi;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({ContainersConfig.class, HealthCheckPersistenceAdapter.class, MonitoredApiPersistenceAdapter.class})
class HealthCheckPersistenceTest {

	private static final Instant T0 = Instant.parse("2026-01-10T12:00:00Z");

	@Autowired
	private HealthCheckPersistenceAdapter checks;

	@Autowired
	private MonitoredApiPersistenceAdapter apis;

	private Long apiId;

	@Test
	void ultimasRetornamDaMaisRecenteParaAMaisAntiga() {
		gravarChecagens(T0.minusSeconds(300), T0.minusSeconds(200), T0.minusSeconds(100));

		List<HealthCheck> ultimas = checks.findUltimas(apiId, 2);

		assertThat(ultimas).hasSize(2);
		assertThat(ultimas.get(0).checkedAt()).isEqualTo(T0.minusSeconds(100));
		assertThat(ultimas.get(1).checkedAt()).isEqualTo(T0.minusSeconds(200));
	}

	@Test
	void findEntreIncluiInicioEExcluiFim() {
		gravarChecagens(T0.minusSeconds(300), T0.minusSeconds(200), T0.minusSeconds(100));

		List<HealthCheck> periodo = checks.findEntre(apiId, T0.minusSeconds(300), T0.minusSeconds(100));

		assertThat(periodo).extracting(HealthCheck::checkedAt)
				.containsExactly(T0.minusSeconds(200), T0.minusSeconds(300));
	}

	@Test
	void apagarAnterioresARemoveSoOsMaisAntigos() {
		gravarChecagens(T0.minusSeconds(300), T0.minusSeconds(200), T0.minusSeconds(100));

		int apagadas = checks.apagarAnterioresA(T0.minusSeconds(150));

		assertThat(apagadas).isEqualTo(2);
		assertThat(checks.findUltimas(apiId, 10)).singleElement()
				.satisfies(c -> assertThat(c.checkedAt()).isEqualTo(T0.minusSeconds(100)));
	}

	private void gravarChecagens(Instant... momentos) {
		apiId = apis.save(new MonitoredApi(null, "Pagamentos " + System.nanoTime(), "https://api.exemplo.com",
				30, 200, true, T0)).id();
		for (Instant momento : momentos) {
			checks.save(new HealthCheck(null, apiId, 200, 50L, true, null, momento));
		}
	}

}
