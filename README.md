# Desafio Votação

API REST em Java para criar pautas, abrir sessões, receber votos de associados e contabilizar resultados, com PostgreSQL, elegibilidade simulada e contratos JSON para cliente mobile.

## 1. Sobre o projeto

Cada pauta possui no máximo uma sessão. Durante a sessão, associados identificados por `associadoId` podem votar uma vez, com `SIM` ou `NAO`. Antes de persistir o voto, um `FakeAssociadoClient` simula validade do CPF e elegibilidade. A API é versionada em `/api/v1`.

## 2. Aplicação disponível na nuvem

O serviço está publicado e funcionando no Render como **ambiente de demonstração/homologação**, não como infraestrutura production-grade.

- API: https://desafio-votacao-v66k.onrender.com
- Swagger UI: https://desafio-votacao-v66k.onrender.com/swagger-ui.html
- OpenAPI JSON: https://desafio-votacao-v66k.onrender.com/v3/api-docs

O Swagger é a forma mais simples de testar a aplicação. `GET /` retornar `404 Not Found` é esperado porque não há endpoint na raiz; isso não significa que o serviço esteja fora do ar. Dependendo do plano, o primeiro acesso após inatividade pode sofrer cold start. Planos gratuitos, quando usados, também seguem os limites e a política de expiração do banco do provedor.

## 3. Tecnologias

- Java 21; Spring Boot 4.1.1; Spring Web MVC
- Spring Data JPA, Hibernate e Bean Validation
- PostgreSQL e Flyway
- Maven e Lombok
- springdoc OpenAPI 3.1.1
- Docker; Render Web Service e Render PostgreSQL
- JUnit Jupiter, Mockito, AssertJ e MockMvc
- SLF4J e Logback

## 4. Arquitetura

```text
HTTP → Controller → Service → Repository → JPA/Hibernate → PostgreSQL
                         ↘ AssociadoClient (fake local)
```

- `controller`: contratos HTTP e status.
- `dto`: requests, responses e telas mobile.
- `service`: regras e transações.
- `repository`: persistência e agregação.
- `entity`: mapeamento do schema `votacao`.
- `client`: elegibilidade fake.
- `exception`: erros uniformes.
- `config`: relógio e OpenAPI.

```text
br/com/desafiovotacao/
├── client/       ├── config/       ├── controller/
├── dto/          ├── entity/       ├── exception/
├── repository/   └── service/
```

## 5. Regras de negócio

- Pauta: título e descrição obrigatórios, com até 150 e 1.000 caracteres.
- Sessão: pauta existente, uma sessão por pauta e duração mínima de 1 minuto. Campo omitido ou `null` usa 1 minuto.
- Voto: pauta e sessão existentes, sessão aberta, `associadoId` obrigatório com até 100 caracteres e opção exatamente `SIM` ou `NAO`.
- Um associado vota uma vez por pauta. No instante do encerramento ou depois dele, o voto é rejeitado.
- Resultado: agregação no PostgreSQL, inclusive durante a sessão; sem votos retorna zeros.

O `FakeAssociadoClient` não faz HTTP nem valida matematicamente CPF. A cada chamada sorteia validade e elegibilidade; a mesma entrada pode variar:

- CPF inválido → `404 Not Found`.
- `ABLE_TO_VOTE` → voto persistido, `201 Created`.
- `UNABLE_TO_VOTE` → `403 Forbidden`, sem persistência.

O `403` é uma decisão desta implementação diante da ambiguidade do enunciado. Pauta, sessão, encerramento e duplicidade são verificados antes do fake.

## 6. Pré-requisitos

- Git
- Java 21
- Maven 3.6.3+
- PostgreSQL

Docker não é obrigatório para execução local tradicional; atende ao empacotamento e ao deploy no Render.

## 7. Clonando o projeto

A implementação está na branch **`desafio-votacao`**:

```shell
git clone https://github.com/JulioMottaJr/desafio-votacao.git
cd desafio-votacao
git checkout desafio-votacao
git branch --show-current
```

O último comando deve retornar `desafio-votacao`.

## 8. PostgreSQL local

O database deve existir antes da inicialização. Defaults: host `localhost`, porta `5432`, database `postgres` e usuário `postgres`.

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/postgres}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}
```

`DB_PASSWORD` não possui default. O usuário precisa criar o schema `votacao`; Flyway cria as tabelas. Não há massa inicial obrigatória.

## 9. Variáveis de ambiente

| Variável | Obrigatória | Default local | Finalidade |
|---|---:|---|---|
| `DB_URL` | No Render | `jdbc:postgresql://localhost:5432/postgres` | URL JDBC |
| `DB_USERNAME` | No Render | `postgres` | Usuário do banco |
| `DB_PASSWORD` | Sim | Nenhum | Senha do banco |
| `PORT` | Não | `8080` | Porta HTTP; Render fornece automaticamente |

Exemplo seguro: `DB_PASSWORD=<senha-local>`. Arquivos `.env` são ignorados, mas não carregados automaticamente.

## 10. Executando localmente

PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/postgres'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = Read-Host 'Senha do PostgreSQL' -MaskInput
mvn clean spring-boot:run
```

`-MaskInput` requer PowerShell 7.1+. Linux/macOS:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/postgres'
export DB_USERNAME='postgres'
read -s -p 'Senha do PostgreSQL: ' DB_PASSWORD && export DB_PASSWORD
mvn clean spring-boot:run
```

A inicialização está correta quando o servidor inicia, Flyway conclui e Hibernate valida o schema. Acesse `http://localhost:8080/swagger-ui.html`.

## 11. Build e execução pelo JAR

```shell
mvn clean package
java -jar target/desafio-votacao-0.0.1-SNAPSHOT.jar
```

As variáveis do banco devem estar no mesmo processo. O build executa os testes normais e exclui o benchmark.

## 12. Docker

O Dockerfile multi-stage usa Maven/JDK 21 no build e somente Java 21 no runtime, viabilizando a aplicação JVM no Render. Ele roda `mvn clean package` e copia o JAR para `/app/app.jar`.

```shell
docker build -t desafio-votacao .
```

Exemplo contra PostgreSQL no host em Windows/macOS:

```shell
docker run --rm -p 8080:8080 \
  -e DB_URL='jdbc:postgresql://host.docker.internal:5432/postgres' \
  -e DB_USERNAME='postgres' \
  -e DB_PASSWORD='<senha-local>' \
  desafio-votacao
```

No container, `localhost` aponta para o container. Em Linux, `host.docker.internal` pode exigir `--add-host=host.docker.internal:host-gateway`; alternativamente use uma rede Docker com o banco acessível por nome.

## 13. Flyway

Flyway cria o schema `votacao` e executa:

1. `V1__create_pauta.sql` — `pauta`.
2. `V2__create_sessao_votacao.sql` — sessão, FK e unicidade por pauta.
3. `V3__create_voto.sql` — voto, FK, unicidade associado/pauta e check `SIM`/`NAO`.

Hibernate usa `ddl-auto=validate`: **não cria nem altera tabelas**. Flyway evolui o banco; Hibernate valida. Não edite migrations aplicadas.

## 14. Swagger e OpenAPI

| Ambiente | Swagger | OpenAPI |
|---|---|---|
| Local | http://localhost:8080/swagger-ui.html | http://localhost:8080/v3/api-docs |
| Cloud | https://desafio-votacao-v66k.onrender.com/swagger-ui.html | https://desafio-votacao-v66k.onrender.com/v3/api-docs |

No Swagger, escolha a operação, **Try it out**, preencha os dados e use **Execute**. POSTs criam dados reais.

## 15. Endpoints

| Método | Endpoint | Finalidade | Sucesso |
|---|---|---|---:|
| `POST` | `/api/v1/pautas` | Criar pauta | `201` |
| `POST` | `/api/v1/pautas/{pautaId}/sessoes` | Abrir sessão | `201` |
| `POST` | `/api/v1/pautas/{pautaId}/votos` | Registrar voto | `201` |
| `GET` | `/api/v1/pautas/{pautaId}/resultado` | Contabilizar | `200` |
| `GET` | `/api/v1/mobile/pautas/nova` | Formulário de pauta | `200` |
| `GET` | `/api/v1/mobile/pautas/{pautaId}/sessoes/nova` | Formulário de sessão | `200` |
| `GET` | `/api/v1/mobile/pautas/{pautaId}/votos/novo` | Seleção de voto | `200` |

As rotas mobile retornam campos e ações com URLs relativas e não validam a pauta. Não há GET individual de pauta, sessão ou voto.

## 16. Guia completo de teste manual

Use o Swagger e o ID realmente retornado.

### Teste 1 — Criar pauta

```http
POST /api/v1/pautas
Content-Type: application/json

{"titulo":"Aquisição de equipamentos","descricao":"Deliberar sobre a compra."}
```

Esperado `201`, header `Location` e response:

```json
{"id":1,"titulo":"Aquisição de equipamentos","descricao":"Deliberar sobre a compra.","dataCriacao":"2026-09-22T12:00:00"}
```

ID e data são gerados em runtime.

### Teste 2 — Abrir sessão

`POST /api/v1/pautas/{pautaId}/sessoes` com `{"duracaoMinutos":5}` retorna `201` e `id`, `pautaId`, `dataAbertura`, `dataEncerramento`. `{}` ou `{"duracaoMinutos":null}` usa 1 minuto. Segunda sessão retorna `409`.

### Teste 3 — Registrar voto

```http
POST /api/v1/pautas/{pautaId}/votos
Content-Type: application/json

{"associadoId":"00000000000","opcao":"SIM"}
```

O fake pode retornar `201`, `403` ou `404`. Para obter voto aceito, tente associados diferentes enquanto a sessão estiver aberta. Response `201`:

```json
{"id":1,"pautaId":1,"associadoId":"00000000000","opcao":"SIM","dataVoto":"2026-09-22T12:01:00"}
```

### Teste 4 — Duplicidade

Após `201`, repita o mesmo associado/pauta. Esperado `409`. Há verificação na aplicação e `UNIQUE (pauta_id, associado_id)`.

### Teste 5 — Resultado

`GET /api/v1/pautas/{pautaId}/resultado` retorna `200`:

```json
{"pautaId":1,"sim":1,"nao":0,"total":1}
```

### Teste 6 — Sessão encerrada

Crie pauta, abra com `{}`, aguarde ao menos 1 minuto e vote. Esperado `409`.

### Teste 7 — Pauta inexistente

Consulte o resultado de um ID confirmado como inexistente. Esperado `404`.

### Teste 8 — Payload inválido

Envie `{"titulo":"","descricao":""}` ao POST de pautas. Esperado `400`, `Erro de validação` e `fields`.

### Teste 9 — Opção inválida

Envie `{"associadoId":"00000000000","opcao":"TALVEZ"}` ao POST de votos. Esperado `400`, `Corpo da requisição inválido`.

## 17. Exemplos cURL

```bash
BASE_URL='http://localhost:8080'
# BASE_URL='https://desafio-votacao-v66k.onrender.com'

curl -i -X POST "$BASE_URL/api/v1/pautas" -H 'Content-Type: application/json' \
  -d '{"titulo":"Pauta via cURL","descricao":"Teste principal."}'

PAUTA_ID='substitua-pelo-id-retornado'
curl -i -X POST "$BASE_URL/api/v1/pautas/$PAUTA_ID/sessoes" \
  -H 'Content-Type: application/json' -d '{"duracaoMinutos":5}'
curl -i -X POST "$BASE_URL/api/v1/pautas/$PAUTA_ID/votos" \
  -H 'Content-Type: application/json' -d '{"associadoId":"00000000000","opcao":"SIM"}'
curl -i "$BASE_URL/api/v1/pautas/$PAUTA_ID/resultado"
```

No PowerShell, use `$BASE_URL`, `$PAUTA_ID` e `curl.exe` para preservar a sintaxe.

## 18. Códigos HTTP

| Código | Uso |
|---:|---|
| `200` | Resultado e contratos mobile |
| `201` | Recurso criado, com `Location` |
| `400` | Validação, JSON/enum ou parâmetro inválido |
| `403` | CPF válido com `UNABLE_TO_VOTE` |
| `404` | Pauta/sessão ausente, CPF inválido ou raiz sem rota |
| `405` | Método não suportado |
| `409` | Sessão repetida, voto duplicado ou sessão encerrada |
| `500` | Falha inesperada sem detalhes internos |

## 19. Formato de erro

```json
{"timestamp":"2026-09-22T12:00:00","status":403,"error":"Forbidden","message":"Associado não habilitado para votar.","path":"/api/v1/pautas/1/votos"}
```

Bean Validation também inclui `fields`:

```json
{"timestamp":"2026-09-22T12:00:00","status":400,"error":"Bad Request","message":"Erro de validação","path":"/api/v1/pautas","fields":{"titulo":"não deve estar em branco"}}
```

`fields` é omitido quando vazio. Stack trace e classe da exceção não são expostos.

## 20. Testes automatizados

```shell
mvn clean test
```

Na validação final desta documentação, 93 testes passaram; o número pode mudar conforme a suíte evoluir. Ela cobre services, controllers, validações, erros, fake, mobile, OpenAPI e Swagger, não exige PostgreSQL e exclui `VotoPerformanceTest`.

## 21. Benchmark

Com PostgreSQL real e migrations aplicadas:

```shell
mvn test -Dtest=VotoPerformanceTest
mvn test -Dtest=VotoPerformanceTest -Dperformance.votes=500000
```

O padrão é 100.000 votos, gerados por `generate_series`, metade `SIM` e metade `NAO`. O teste usa a agregação real, sempre faz rollback, não inicia API/Flyway, não simula usuários simultâneos, não é benchmark HTTP e não define SLA.

Uma execução local anterior de referência contabilizou 100.000 votos (50.000/50.000) em aproximadamente 153 ms. Isso depende de ambiente e cache e não é garantia.

## 22. Concorrência e integridade

A aplicação verifica duplicidade; `@Transactional` e `saveAndFlush` permitem traduzir violações em `409`. A `UNIQUE (pauta_id, associado_id)` é a defesa definitiva contra corrida concorrente. Uma constraint equivalente protege a sessão única por pauta.

## 23. Deploy no Render

1. Crie PostgreSQL e Web Service na mesma região.
2. Conecte o GitHub e selecione a branch `desafio-votacao`.
3. Selecione runtime Docker.
4. Configure `DB_URL=jdbc:postgresql://<internal-host>:5432/<database>`.
5. Configure `DB_USERNAME` e `DB_PASSWORD` sem versioná-los.
6. Deixe o Render fornecer `PORT`.
7. Inicie o deploy.

O Dockerfile roda Maven/testes, gera o JAR e inicia `java -jar /app/app.jar`. Flyway migra e Hibernate valida; Swagger fica público.

## 24. Persistência

Crie pauta/sessão/voto, consulte, reinicie ou faça redeploy somente do Web Service e consulte novamente. Os dados permanecem no PostgreSQL, não no filesystem efêmero do container.

## 25. Troubleshooting

- **Banco:** revise `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, database e rede.
- **Flyway:** confira conectividade e permissão no schema; não edite migrations aplicadas.
- **Porta:** local 8080; Render fornece `PORT`.
- **Raiz 404:** esperado; use Swagger/OpenAPI.
- **Primeiro acesso lento:** possível cold start.
- **Voto 403/404:** possível decisão do fake.
- **409:** sessão repetida, voto duplicado ou sessão encerrada.

## 26. Decisões técnicas

- Spring Boot → API e configuração.
- PostgreSQL → persistência e integridade.
- JPA/Hibernate → mapeamento e transações.
- Flyway → evolução determinística.
- Bean Validation → entradas válidas.
- Agregação no banco → contagem eficiente.
- `UNIQUE` → defesa concorrente.
- Docker → Java 21 reproduzível no Render.
- OpenAPI → contrato e teste navegável.
- `/api/v1` → versionamento por URL.

## 27. Limitações e escopo

- Elegibilidade fake e aleatória, sem serviço externo ou validação matemática.
- Sem autenticação/autorização, conforme permitido.
- Render apenas para demonstração/homologação.
- Cold start, disponibilidade e expiração dependem do plano.
- Benchmark não é carga HTTP nem SLA.
- Sem listagem ou GET individual de pauta, sessão ou voto.
- Logs em arquivo no container não são persistentes.
