# Projeto: Painel de Monitoramento de APIs (Angular + Spring Boot)

## Ideia geral

Um sistema com um **backend em Java** que monitora um conjunto de APIs: faz chamadas periódicas, mede tempo de resposta e status, registra falhas e expõe métricas. Um **frontend em Angular** mostra um painel com a saúde de cada serviço, gráficos de latência e alertas quando algo sai do normal.

Esse projeto cobre o lado de observabilidade que aparece em várias vagas (monitoramento de APIs, Datadog, Kibana, New Relic, Opsgenie) e combina backend com frontend, o que é pedido na vaga de Full Stack Java/Angular da 4DF Connect.

## Por que este projeto ajuda nos processos seletivos

- A vaga **4DF Full Stack (Java Angular e AWS)** pede desenvolvimento com Java e Angular, testes, revisão de código e suporte a produção.
- A vaga **4DF Back End** lista explicitamente Datadog, Kibana, New Relic e Opsgenie, e pede conhecimento de monitoramento de APIs e microsserviços.
- As vagas **DXC Backend Java/Kotlin** e **DXC Integração** citam observabilidade, monitoramento e sustentação como diferenciais.
- A vaga **DXC Backend Java/Kotlin** também pede AngularJS, então o frontend serve para as duas frentes.

## Tecnologias

| Tecnologia | Para que serve no projeto | Vagas que pedem |
|---|---|---|
| Java 21 e Spring Boot 4.1.1 | Backend, agendamento das checagens, API para o frontend | Quase todas |
| Spring Boot Actuator e Micrometer | Métricas e endpoint de saúde do próprio sistema | 4DF Back End, DXC Backend |
| Prometheus e Grafana | Coleta e visualização de métricas (ferramenta de referência para observabilidade) | 4DF Back End (equivalentes: Datadog, New Relic) |
| Angular | Painel com lista de serviços e gráficos | 4DF Full Stack |
| Chart.js ou ngx-charts | Gráficos de latência | 4DF Full Stack |
| PostgreSQL (SQL) | Histórico de checagens | Várias |
| JUnit 5 e Mockito | Testes do agendador e da lógica de alerta | Várias |
| Docker Compose | Subir backend, banco, Prometheus e Grafana | Vertem, BairesDev |
| GitHub Actions | Pipeline de build, testes e build do frontend | 4DF, BairesDev |

## Escopo do MVP

1. Cadastro de APIs a monitorar (nome, URL, intervalo, status esperado).
2. Checagem periódica com Spring Scheduling, registrando status HTTP e tempo de resposta.
3. Regra de alerta: três falhas seguidas marcam o serviço como fora do ar.
4. Painel Angular com a lista de serviços, cor por estado e gráfico de latência das últimas 24 horas.
5. Endpoint `/actuator/prometheus` expondo as métricas do próprio monitor.

## Arquitetura sugerida

- Backend com **clean architecture**: o agendador e a lógica de alerta não dependem de como o histórico é guardado.
- Frontend em Angular com **serviços** para chamadas HTTP e componentes separados por tela, com testes unitários dos serviços.
- Comunicação por REST entre frontend e backend, com contrato documentado (OpenAPI/Swagger).

## Estado atual (11/11/2026)

- **Backend: completo** (seções 0 a 11 do [CHECKLIST_BACKEND.md](CHECKLIST_BACKEND.md)). CRUD de APIs, checagens periódicas, histórico com retenção, regra de alerta com restauração no reinício, endpoints do painel (status, histórico e resumo), CORS, métricas no Micrometer, logs em JSON, pilha completa no Docker Compose com Prometheus e Grafana, testes de integração com Testcontainers, CI no GitHub Actions e runbook. 75 testes passando, com 95,7% de cobertura de linhas.
- **Pendências do backend que dependem de ação fora do código**: executar o workflow de CI no GitHub (depois do push) e capturar os prints do Grafana e do Swagger.
- **Frontend Angular**: próxima etapa, começa agora que o backend está completo.

## Roteiro de desenvolvimento

1. [x] Criar o backend com CRUD de APIs monitoradas e testes da regra de alerta.
2. [x] Implementar o agendador de checagens e a gravação do histórico.
3. [x] Expor os endpoints REST, a documentação Swagger e os endpoints de leitura para o painel.
4. [ ] Criar o painel Angular consumindo a API.
5. [x] Subir Prometheus e Grafana no Docker Compose e configurar um dashboard (backend completo; falta só a captura de tela).
6. [~] Configurar o pipeline de CI e escrever o README com prints do painel. (CI e README prontos; falta executar o CI no GitHub e incluir os prints.)

## Diferenciais para destacar

- Alerta com regra explicável (três falhas seguidas), em vez de só mostrar vermelho e verde.
- Dashboard de métricas do próprio monitor, mostrando que você olha para o sistema em produção.
- Testes do frontend e do backend no mesmo pipeline.
- Um runbook curto: "se um serviço aparecer como fora do ar, o que checar". Isso demonstra a mentalidade de suporte que as vagas de sustentação pedem.

## Como apresentar na entrevista

Derrube uma das APIs monitoradas de propósito e mostre o painel mudando de estado, o alerta sendo disparado e a métrica aparecendo no Grafana. Depois, explique por que escolheu três falhas seguidas como regra e o que mudaria para reduzir alarmes falsos. Se a vaga for de Angular, aprofunde no serviço HTTP e nos testes do frontend.
