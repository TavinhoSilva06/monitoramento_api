# Checklist do Backend: Monitor de APIs

Base atual: Spring Boot 4.1.1, Java 21, Maven. Já estão no `pom.xml`: Actuator, Data JPA, Security, Micrometer Prometheus, PostgreSQL, Lombok e Docker Compose support.

## 0. Ajustes no projeto existente

- [x] Adicionar `spring-boot-starter-webmvc` (API REST; o Actuator sozinho não expõe controllers)
- [x] Adicionar `spring-boot-starter-validation` (Bean Validation nos DTOs)
- [x] Adicionar springdoc-openapi (Swagger UI), versão 3.1.1
- [x] Adicionar Flyway (migrations do banco) e usar `ddl-auto=validate`
- [x] Remover a senha em texto puro do `compose.yaml` e usar variáveis de ambiente (`.env` no `.gitignore`, modelo em `.env.example`)
- [x] Criar `application-dev.properties` e `application-test.properties` (perfis)
- [x] Definir `spring.application.name` e configurar timezone (UTC) para os timestamps
- [ ] Revisar o `MonitoramentoApiApplicationTests` para ele subir o contexto com perfil de teste (pendente: precisa do Docker rodando)

## 1. Estrutura (clean architecture)

- [ ] Criar os pacotes: `domain`, `application`, `infrastructure`, `interfaces` (ou `web`)
- [ ] `domain`: entidades e regra de alerta, sem anotações de framework
- [ ] `application`: casos de uso (cadastrar API, executar checagem, avaliar alerta)
- [ ] `application`: portas (interfaces) para repositório de APIs, repositório de checagens e cliente HTTP
- [ ] `infrastructure`: adaptadores JPA, cliente HTTP (`RestClient`) e configuração do agendador
- [ ] `interfaces`: controllers REST e DTOs

## 2. Cadastro de APIs monitoradas (CRUD)

- [ ] Entidade `MonitoredApi`: id, nome, URL, intervalo (segundos), status HTTP esperado, ativa (boolean), criada em
- [ ] Validar: URL válida (http/https), intervalo mínimo, nome único
- [ ] Endpoints: `POST /apis`, `GET /apis`, `GET /apis/{id}`, `PUT /apis/{id}`, `DELETE /apis/{id}`
- [ ] Tratamento global de erros (`@RestControllerAdvice`) com respostas padronizadas (404, 400, 409)
- [ ] Testes unitários dos casos de uso de CRUD

## 3. Checagem periódica

- [ ] Habilitar agendamento com `@EnableScheduling`
- [ ] Agendador que busca as APIs ativas e dispara cada checagem no seu intervalo
- [ ] Cliente HTTP com timeout de conexão e de leitura configuráveis
- [ ] Registrar por checagem: API, status HTTP, tempo de resposta (ms), sucesso (boolean), erro (mensagem, se houver), data/hora
- [ ] Tratar exceções de rede (timeout, DNS, conexão recusada) como falha, sem derrubar o agendador
- [ ] Checagens em paralelo com limite, para uma API lenta não atrasar as outras
- [ ] Testes do agendador com Mockito (cliente HTTP mockado)

## 4. Regra de alerta

- [ ] Contador de falhas consecutivas por API
- [ ] Estado do serviço: `UP`, `DEGRADED` (uma ou duas falhas), `DOWN` (três falhas seguidas)
- [ ] Voltar para `UP` na primeira checagem bem-sucedida e zerar o contador
- [ ] Disparar evento/log quando o estado muda (`UP` → `DOWN` e `DOWN` → `UP`)
- [ ] Os três limites (3 falhas, etc.) em configuração, não fixos no código
- [ ] Testes unitários da regra: sequência de falhas, recuperação, falha intercalada com sucesso

## 5. Histórico (PostgreSQL)

- [ ] Tabela `monitored_api` e tabela `health_check` (com índice em `api_id` + `checked_at`)
- [ ] Repositório JPA com consulta das últimas 24 horas por API
- [ ] Política de retenção: job diário que apaga checagens com mais de N dias
- [ ] Testes de repositório com Testcontainers (ou H2 em teste, se Testcontainers ficar pesado)

## 6. Endpoints para o frontend

- [ ] `GET /apis/status`: lista de APIs com estado atual, última latência e última checagem
- [ ] `GET /apis/{id}/checks?from=&to=`: histórico para o gráfico de latência (últimas 24 horas por padrão)
- [ ] `GET /apis/{id}/summary`: uptime e latência média/p95 no período
- [ ] Liberar CORS para a origem do Angular (ex.: `http://localhost:4200`)
- [ ] Configurar Spring Security: liberar os endpoints de leitura do painel e os de Actuator necessários, proteger o CRUD (ou documentar que é local)
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

- [ ] Testes unitários da regra de alerta e dos casos de uso (JUnit 5 + Mockito)
- [ ] Testes de controller com `@WebMvcTest` (validação e códigos HTTP)
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
