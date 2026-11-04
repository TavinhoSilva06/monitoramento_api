# Checklist do Backend: Monitor de APIs

Base atual: Spring Boot 4.1.1, Java 21, Maven. Já estão no `pom.xml`: Actuator, Data JPA, Security, Micrometer Prometheus, PostgreSQL, Lombok e Docker Compose support.

## 0. Ajustes no projeto existente

- [x] Adicionar `spring-boot-starter-webmvc` (API REST; o Actuator sozinho não expõe controllers)
- [x] Adicionar `spring-boot-starter-validation` (Bean Validation nos DTOs)
- [x] Adicionar springdoc-openapi (Swagger UI), versão 3.1.1
- [x] Schema pelo JPA com `ddl-auto=update` (Flyway removido: as tabelas saem das anotações das entidades)
- [x] Remover a senha em texto puro do `compose.yaml` e usar variáveis de ambiente (`.env` no `.gitignore`, modelo em `.env.example`)
- [x] Criar `application-dev.properties` e `application-test.properties` (perfis)
- [x] Definir `spring.application.name` e configurar timezone (UTC) para os timestamps
- [x] Revisar o `MonitoramentoApiApplicationTests` para ele subir o contexto com perfil de teste (o perfil `test` liga o Postgres do compose, que precisa do Docker rodando)

## 1. Estrutura (clean architecture)

- [x] Criar os pacotes: `domain`, `application`, `infrastructure`, `interfaces` (ou `web`)
- [x] `domain`: entidades e regra de alerta, sem anotações de framework
- [x] `application`: casos de uso (cadastrar API, executar checagem, avaliar alerta)
- [x] `application`: portas (interfaces) para repositório de APIs, repositório de checagens e cliente HTTP
- [x] `infrastructure`: adaptadores JPA, cliente HTTP (`RestClient`) e configuração do agendador
- [x] `interfaces`: controllers REST e DTOs

## 2. Cadastro de APIs monitoradas (CRUD)

- [x] Entidade `MonitoredApi`: id, nome, URL, intervalo (segundos), status HTTP esperado, ativa (boolean), criada em
- [x] Validar: URL válida (http/https), intervalo mínimo, nome único
- [x] Endpoints: `POST /apis`, `GET /apis`, `GET /apis/{id}`, `PUT /apis/{id}`, `DELETE /apis/{id}`
- [x] Tratamento global de erros (`@RestControllerAdvice`) com respostas padronizadas (404, 400, 409), no formato `ProblemDetail`
- [x] Testes unitários dos casos de uso de CRUD

## 3. Checagem periódica

- [x] Habilitar agendamento com `@EnableScheduling`
- [x] Agendador que busca as APIs ativas e dispara cada checagem no seu intervalo
- [x] Cliente HTTP com timeout de conexão e de leitura configuráveis
- [x] Registrar por checagem: API, status HTTP, tempo de resposta (ms), sucesso (boolean), erro (mensagem, se houver), data/hora
- [x] Tratar exceções de rede (timeout, DNS, conexão recusada) como falha, sem derrubar o agendador
- [x] Checagens em paralelo com limite, para uma API lenta não atrasar as outras
- [x] Testes do agendador com Mockito (cliente HTTP mockado)

## 4. Regra de alerta

- [x] Contador de falhas consecutivas por API
- [x] Estado do serviço: `UP`, `DEGRADED` (uma ou duas falhas), `DOWN` (três falhas seguidas)
- [x] Voltar para `UP` na primeira checagem bem-sucedida e zerar o contador
- [x] Disparar evento/log quando o estado muda (`UP` → `DOWN` e `DOWN` → `UP`) — hoje é log `INFO`, sem barramento de eventos
- [x] Os três limites (3 falhas, etc.) em configuração, não fixos no código (`monitoramento.alerta.falhas-para-down`)
- [x] Testes unitários da regra: sequência de falhas, recuperação, falha intercalada com sucesso
- [ ] Restaurar o estado de saúde a partir do histórico ao reiniciar (hoje fica em memória: após reiniciar, todas as APIs voltam a `UP` até a próxima checagem)

## 5. Histórico (PostgreSQL)

- [x] Tabela `monitored_api` e tabela `health_check` (com índice em `api_id` + `checked_at`)
- [x] Repositório JPA com consulta das últimas 24 horas por API (`findByApiSince`; o chamador passa o `since`)
- [ ] Política de retenção: job diário que apaga checagens com mais de N dias
- [ ] Testes de repositório com Testcontainers (ou H2 em teste, se Testcontainers ficar pesado)

## 6. Endpoints para o frontend

- [ ] `GET /apis/status`: lista de APIs com estado atual, última latência e última checagem
- [ ] `GET /apis/{id}/checks?from=&to=`: histórico para o gráfico de latência (últimas 24 horas por padrão)
- [ ] `GET /apis/{id}/summary`: uptime e latência média/p95 no período
- [ ] Liberar CORS para a origem do Angular (ex.: `http://localhost:4200`)
- [x] Configurar Spring Security: liberar os endpoints de leitura do painel e os de Actuator necessários, proteger o CRUD (ou documentar que é local) — projeto local: CRUD aberto, rotas desconhecidas negadas (`SecurityConfig`)
- [ ] Documentação Swagger acessível em `/swagger-ui.html`

## 7. Métricas e observabilidade do próprio monitor

- [ ] Expor `/actuator/prometheus` e `/actuator/health`
- [ ] Limitar os endpoints do Actuator expostos (`management.endpoints.web.exposure.include`)
- [ ] Métricas customizadas com Micrometer: contador de checagens por resultado, timer de latência por API, gauge de APIs `DOWN`
- [ ] Logs estruturados (nível INFO para mudança de estado, WARN para falha, DEBUG para cada checagem)

## 8. Infraestrutura local (Docker Compose)

- [ ] Serviço `postgres` com volume nomeado e senha por variável de ambiente (arquivo `.env` no `.gitignore`)
- [ ] Serviço `prometheus` com `prometheus.yml` apontando para `backend:8080/actuator/prometheus`
- [ ] Serviço `grafana` com dashboard provisionado (arquivo JSON versionado)
- [ ] Serviço `backend` (Dockerfile multi-stage: build com Maven, runtime com JRE 21)
- [ ] Testar `docker compose up` do zero e confirmar que o Grafana recebe métricas

## 9. Testes

- [x] Testes unitários da regra de alerta e dos casos de uso (JUnit 5 + Mockito)
- [x] Testes de controller com `@WebMvcTest` (validação e códigos HTTP)
- [ ] Teste de integração do fluxo completo: cadastra API, checagem roda, estado muda (com Testcontainers)
- [ ] Cobertura mínima definida e medida (JaCoCo)

## 10. CI (GitHub Actions)

- [ ] Workflow `backend-ci.yml`: checkout, JDK 21, `./mvnw verify` a cada push e PR
- [ ] Cache do Maven
- [ ] Publicar relatório de testes e cobertura como artefato
- [ ] Badge de build no README

## 11. Documentação e entrega

- [ ] README do backend: como subir com Docker Compose, como rodar testes, variáveis de ambiente
- [ ] Diagrama simples da arquitetura (agendador → banco → API → painel → Prometheus → Grafana)
- [ ] Runbook: "se um serviço aparecer como fora do ar, o que checar"
- [ ] Prints do Grafana e do Swagger no README

## Critério de pronto do backend

- [ ] Cadastro, checagem periódica, alerta de três falhas e histórico funcionando
- [ ] Derrubar uma API monitorada muda o estado para `DOWN` e aparece no endpoint de status
- [ ] Métricas aparecem no Grafana
- [ ] Testes e pipeline de CI passando
