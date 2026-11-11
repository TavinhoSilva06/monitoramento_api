# Runbook: diagnóstico do monitor

Use este roteiro quando uma API aparece como `DOWN` no painel, ou quando o monitor parece não estar funcionando. Os exemplos usam `http://localhost:8080`.

## 1. Uma API aparece como DOWN (ou DEGRADED)

**Passo 1: veja o motivo registrado.**

```bash
curl http://localhost:8080/apis/status
```

Em `ultimaChecagem`, o campo `errorMessage` diz o que aconteceu:

| `errorMessage` começa com | Significado | O que checar |
|---|---|---|
| `Status esperado 200, recebido 500` | A API respondeu, mas com outro status | A API alvo está com erro? O `expectedStatus` cadastrado está certo? |
| `HttpConnectTimeoutException` ou `HttpTimeoutException` | A API não respondeu dentro do timeout | A API está lenta ou inacessível. Ajuste `timeout-conexao-ms` / `timeout-leitura-ms` se for esperado |
| `UnknownHostException: <host>` | O nome não resolve (DNS) | Confira a URL cadastrada e a conexão de rede do monitor |
| `ConnectException` ou `ClosedChannelException` | Nada escutando na porta (API caiu, ou porta errada na URL) | Confirme que a API está no ar e que a porta da URL está certa |

**Passo 2: reproduza a checagem à mão.**

```bash
curl -i <url-da-api>
```

Se o `curl` falhar da mesma forma, o problema está na API ou na rede. Se o `curl` funcionar, verifique se o `expectedStatus` cadastrado bate com a resposta.

**Passo 3: confira o histórico.**

```bash
curl "http://localhost:8080/apis/<id>/checks"
```

Uma sequência de falhas seguidas explica o `DOWN`. Uma falha isolada deixa a API em `DEGRADED`, e ela volta a `UP` na primeira checagem com sucesso.

**Passo 4: confira os logs.** Procure `Falha na checagem de <nome>` (nível `WARN`) e `API <nome> mudou de ... para ...` (nível `INFO`). No modo completo:

```bash
docker compose --profile completo logs -f backend
```

## 2. A API está como UNKNOWN

`UNKNOWN` significa que a API ainda não tem nenhuma checagem gravada. Isso é esperado logo depois do cadastro: o agendador roda a cada 5 segundos e checa a API quando o intervalo dela vence.

- Espere um ciclo do intervalo configurado.
- Confirme que a API está ativa (`"active": true` em `GET /apis/<id>`). APIs inativas não são checadas.
- Se a API tem histórico e continua `UNKNOWN` depois de reiniciar, o histórico não foi restaurado. Veja os logs de inicialização procurando `Nao foi possivel restaurar o estado da API`.

## 3. O cadastro falha

| Status | Causa mais comum |
|---|---|
| 415 | Falta `Content-Type: application/json`. No Postman, use Body → `raw` → `JSON`. |
| 400 com "Corpo da requisicao invalido" | JSON malformado, ou campo em snake_case. Os campos são camelCase: `intervalSeconds`, `expectedStatus`. |
| 400 com `errors` | Algum campo não passou na validação. A chave em `errors` diz qual. |
| 409 | Já existe uma API com o mesmo nome. |

## 4. As métricas não aparecem no Grafana

1. Confirme que o backend responde: `curl http://localhost:8080/actuator/health` deve retornar `{"status":"UP"}`.
2. Confirme que a métrica é exposta: `curl http://localhost:8080/actuator/prometheus | grep monitoramento_`.
3. Abra `http://localhost:9090/targets`. O alvo `monitoramento-api` deve estar `UP`. Se estiver `DOWN`, o Prometheus não alcança `backend:8080`: confira `docker compose --profile completo ps`.
4. No Grafana, o datasource "Prometheus" deve estar saudável (Connections → Data sources → Test).
5. Métricas de checagem só aparecem depois da primeira checagem. Um monitor recém-iniciado sem APIs ativas não gera nenhuma.

## 5. O backend não sobe, ou o health está DOWN

```bash
curl http://localhost:8080/actuator/health
```

Se o status for `DOWN`, quase sempre é o banco. Verifique:

```bash
docker compose ps                       # o postgres deve estar healthy
docker compose logs postgres | tail     # erros de inicialização ou senha
```

- Senha: `POSTGRES_PASSWORD` no `.env` precisa ser a mesma usada quando o volume foi criado. Trocar a senha depois de criar o volume não muda a senha do banco. Nesse caso, remova o volume com `docker compose down -v` (apaga os dados) e suba de novo.
- Porta: no modo desenvolvimento, o Spring usa a porta do compose sozinho. No modo completo, o backend conecta por `postgres:5432` dentro da rede do compose.

## 6. O histórico sumiu

A retenção apaga checagens com mais de `monitoramento.retencao.dias` dias (30 por padrão), todo dia às 03:00. Para conferir se a limpeza rodou, procure a linha `Retencao: <n> checagens anteriores a ... foram apagadas` nos logs.

Consultas com período maior que 30 dias são recusadas com 400, e isso é intencional.

## 7. Referência rápida

| Quero saber | Onde |
|---|---|
| Estado atual de todas as APIs | `GET /apis/status` |
| Checagens de uma API | `GET /apis/<id>/checks` |
| Uptime e latência | `GET /apis/<id>/summary` |
| Saúde do próprio monitor | `GET /actuator/health` |
| Métricas | `GET /actuator/prometheus` |
| Documentação da API | `http://localhost:8080/swagger-ui.html` |
| Logs do backend (modo completo) | `docker compose --profile completo logs -f backend` |
