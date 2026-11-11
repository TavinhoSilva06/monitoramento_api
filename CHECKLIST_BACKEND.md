# Checklist do Backend: Monitor de APIs

Base atual: Spring Boot 4.1.1, Java 21, Maven. Já estão no `pom.xml`: Actuator, Data JPA, Security, Micrometer Prometheus, PostgreSQL, Lombok, Docker Compose support, Testcontainers e JaCoCo.

Última atualização: 11/11/2026. Testes: 75 passando e cobertura de linhas de 95,7% (`./mvnw verify`, mínimo de 85%). Itens que dependem de ação fora do código (executar o CI no GitHub, capturas de tela) continuam abertos e estão anotados.

## 0. Ajustes no projeto existente

- [x] Adicionar `spring-boot-starter-webmvc` (API REST; o Actuator sozinho não expõe controllers)
- [x] Adicionar `spring-boot-starter-validation` (Bean Validation nos DTOs)
- [x] Adicionar springdoc-openapi (Swagger UI), versão 3.1.1
- [x] Schema pelo JPA com `ddl-auto=update` (Flyway removido: as tabelas saem das anotações das entidades)
- [x] Remover a senha em texto puro do `compose.yaml` e usar variáveis de ambiente (`.env` no `.gitignore`, modelo em `.env.example`)
- [x] Criar `application-dev.properties` e `application-test.properties` (perfis)
- [x] Definir `spring.application.name` e configurar timezone (UTC) para os timestamps
- [x] Revisar o `MonitoramentoApiApplicationTests` para ele subir o contexto com perfil de teste (o perfil `test` usa o PostgreSQL do Testcontainers, via `ContainersConfig`, e não o compose)

## 1. Estrutura (clean architecture)

- [x] Criar os pacotes: `domain`, `application`, `infrastructure`, `interfaces` (ou `web`)
- [x] `domain`: entidades e regra de alerta, sem anotações de framework
- [x] `application`: casos de uso (cadastrar API, executar checagem, avaliar alerta, consultar painel, retenção)
- [x] `application`: portas (interfaces) para repositório de APIs, repositório de checagens, cliente HTTP e métricas
- [x] `infrastructure`: adaptadores JPA, cliente HTTP (`RestClient`), Micrometer e configuração do agendador
- [x] `interfaces`: controllers REST e DTOs

## 2. Cadastro de APIs monitoradas (CRUD)

- [x] Entidade `MonitoredApi`: id, nome, URL, intervalo (segundos), status HTTP esperado, ativa (boolean), criada em
- [x] Validar: URL válida (http/https), intervalo mínimo, nome único
- [x] Endpoints: `POST /apis`, `GET /apis`, `GET /apis/{id}`, `PUT /apis/{id}`, `DELETE /apis/{id}`
- [x] Tratamento global de erros (`@RestControllerAdvice`) com respostas padronizadas (404, 400, 409, 415), no formato `ProblemDetail`, sem stack trace
- [x] Testes unitários dos casos de uso de CRUD

## 3. Checagem periódica

- [x] Habilitar agendamento com `@EnableScheduling`
- [x] Agendador que busca as APIs ativas e dispara cada checagem no seu intervalo
- [x] Cliente HTTP com timeout de conexão e de leitura configuráveis
- [x] Registrar por checagem: API, status HTTP, tempo de resposta (ms), sucesso (boolean), erro (mensagem, se houver), data/hora
- [x] Tratar exceções de rede (timeout, DNS, conexão recusada) como falha, sem derrubar o agendador. A mensagem de erro usa a causa real (ex.: `UnknownHostException`), não o wrapper genérico
- [x] Checagens em paralelo com limite, para uma API lenta não atrasar as outras
- [x] Testes do agendador com Mockito (cliente HTTP mockado) e da probe contra porta fechada

## 4. Regra de alerta

- [x] Contador de falhas consecutivas por API
- [x] Estado do serviço: `UNKNOWN` (sem checagens), `UP`, `DEGRADED` (uma ou duas falhas), `DOWN` (três falhas seguidas)
- [x] Voltar para `UP` na primeira checagem bem-sucedida e zerar o contador
- [x] Disparar evento/log quando o estado muda (`UP` → `DOWN` e `DOWN` → `UP`) — hoje é log `INFO`, sem barramento de eventos
- [x] Os três limites (3 falhas, etc.) em configuração, não fixos no código (`monitoramento.alerta.falhas-para-down`)
- [x] Testes unitários da regra: sequência de falhas, recuperação, falha intercalada com sucesso
- [x] Restaurar o estado de saúde a partir do histórico ao reiniciar (`ApplicationReadyEvent`, reproduzindo as últimas N checagens). Coberto no teste de fluxo completo

## 5. Histórico (PostgreSQL)

- [x] Tabela `monitored_api` e tabela `health_check` (com índice em `api_id` + `checked_at`)
- [x] Consultas por período (`findEntre`) e das últimas checagens (`findUltimas`)
- [x] Política de retenção: job diário (`RetencaoScheduler`, 03:00) que apaga checagens com mais de `monitoramento.retencao.dias` dias (30 por padrão)
- [x] Testes de repositório com Testcontainers (PostgreSQL real): ordem, limites do período e retenção

## 6. Endpoints para o frontend

- [x] `GET /apis/status`: lista de APIs com estado atual e a última checagem (latência, status HTTP, sucesso, mensagem de erro)
- [x] `GET /apis/{id}/checks?from=&to=`: histórico do período (últimas 24 horas por padrão, máximo de 30 dias)
- [x] `GET /apis/{id}/summary?from=&to=`: uptime (%), latência média e p95 do período. A latência considera só checagens que receberam resposta
- [x] Liberar CORS para a origem do Angular (`http://localhost:4200`, configurável em `monitoramento.cors.origens`)
- [x] Configurar Spring Security: liberar os endpoints de leitura do painel e os de Actuator necessários, proteger o CRUD (ou documentar que é local) — projeto local: CRUD aberto, rotas desconhecidas negadas (`SecurityConfig`)
- [x] Documentação Swagger acessível em `/swagger-ui.html` (e OpenAPI em `/v3/api-docs`)

## 7. Métricas e observabilidade do próprio monitor

- [x] Expor `/actuator/prometheus` e `/actuator/health`
- [x] Limitar os endpoints do Actuator expostos (`management.endpoints.web.exposure.include`)
- [x] Métricas customizadas com Micrometer: `monitoramento_checagens_total{resultado}` (contador), `monitoramento_checagem_latencia_seconds{api}` (timer com histograma) e `monitoramento_apis_estado{estado}` (gauge de APIs `DOWN`, `DEGRADED` e `UP`)
- [x] Logs estruturados em JSON/ECS no console (`logging.structured.format.console=ecs`), com os níveis INFO para mudança de estado, WARN para falha, DEBUG para cada checagem bem-sucedida

## 8. Infraestrutura local (Docker Compose)

- [x] Serviço `postgres` com volume nomeado (`postgres-dados`) e senha por variável de ambiente (arquivo `.env` no `.gitignore`)
- [x] Serviço `prometheus` com `observabilidade/prometheus/prometheus.yml` apontando para `backend:8080/actuator/prometheus`
- [x] Serviço `grafana` com datasource e dashboard provisionados (`observabilidade/grafana/`, dashboard versionado em JSON)
- [x] Serviço `backend` (`backend/Dockerfile` multi-stage: build com Maven, runtime com JRE 21, usuário não-root)
- [x] Pilha completa sobe com `docker compose --profile completo up -d --build`. Sem o perfil, o compose sobe só o PostgreSQL, e o `spring-boot:run` continua funcionando
- [x] Confirmado: alvo do Prometheus `UP`, métricas com séries, datasource do Grafana saudável e dashboard listado pela API do Grafana

## 9. Testes

- [x] Testes unitários da regra de alerta e dos casos de uso (JUnit 5 + Mockito)
- [x] Testes de controller com `@WebMvcTest` (validação, códigos HTTP, erros de corpo e CORS)
- [x] Teste de integração do fluxo completo com PostgreSQL real: cadastra API, checagens reais contra um servidor local, estado muda para `DOWN`, aparece no status e no resumo, e volta a `UP` depois da recuperação
- [x] Cobertura mínima definida e medida (JaCoCo): mínimo de 85% de linhas, medido em 95,7%

## 10. CI (GitHub Actions)

- [x] Workflow `.github/workflows/backend-ci.yml`: checkout, JDK 21 (Temurin), `./mvnw -B verify` a cada push e PR
- [x] Cache do Maven
- [x] Publicar relatório de testes e cobertura como artefato
- [ ] Workflow executado com sucesso no GitHub (depende de fazer push; o build local equivalente já passa)
- [x] Badge de build no README (aparece depois do primeiro run no GitHub)

## 11. Documentação e entrega

- [x] README: como subir com Docker Compose (dois modos), como rodar testes, variáveis de ambiente, endpoints e arquitetura
- [x] Diagrama da arquitetura no README (Mermaid: agendador → banco → API → painel → Prometheus → Grafana)
- [x] Runbook: `RUNBOOK.md`, com diagnóstico de API `DOWN`, `UNKNOWN`, cadastro com erro, métricas ausentes e banco indisponível
- [ ] Prints do Grafana e do Swagger no README (capturas de tela, feitas manualmente)

## Critério de pronto do backend

- [x] Cadastro, checagem periódica, alerta de três falhas e histórico funcionando (histórico lido pelos endpoints do painel)
- [x] Derrubar uma API monitorada muda o estado para `DOWN` e aparece no endpoint de status (coberto no teste de fluxo completo)
- [x] Métricas chegam ao Prometheus e ao Grafana (verificado pelas APIs do Prometheus e do Grafana; a visualização do dashboard ainda não foi conferida por captura de tela)
- [ ] Testes e pipeline de CI passando no GitHub (testes passam localmente; falta rodar o workflow após o push)
