# Guia operacional

Passos para configurar, executar e diagnosticar a aplicação. O contrato funcional, endpoints e teste manual completo estão no [README](README.md).

## 1. Pré-requisitos

- Git
- Java 21
- Maven 3.6.3+
- PostgreSQL
- Docker somente para container/deploy

```shell
git --version
java -version
mvn -version
docker --version
```

O último comando é opcional para execução tradicional.

## 2. Obter a branch correta

```shell
git clone https://github.com/JulioMottaJr/desafio-votacao.git
cd desafio-votacao
git checkout desafio-votacao
git branch --show-current
```

A implementação está em `desafio-votacao`.

## 3. PostgreSQL e Flyway

O database deve existir. Defaults locais:

```text
host: localhost
port: 5432
database: postgres
username: postgres
```

O usuário deve criar o schema `votacao`. Não crie tabelas nem carregue massa: Flyway cria o schema e executa V1 (pauta), V2 (sessão) e V3 (voto). Hibernate usa `ddl-auto=validate`; ele valida, mas não altera o banco.

## 4. Variáveis

| Variável | Default | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` | URL JDBC |
| `DB_USERNAME` | `postgres` | Usuário |
| `DB_PASSWORD` | Nenhum | Senha obrigatória |
| `PORT` | `8080` | Porta HTTP |

Não salve credenciais em arquivos versionados. `.env` é ignorado, mas não carregado automaticamente.

PowerShell:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/postgres'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = Read-Host 'Senha do PostgreSQL' -MaskInput
```

Linux/macOS:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/postgres'
export DB_USERNAME='postgres'
read -s -p 'Senha do PostgreSQL: ' DB_PASSWORD && export DB_PASSWORD
```

## 5. Execução local

```shell
mvn clean spring-boot:run
```

- Swagger: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs
- `GET /` retornar 404 é esperado.

## 6. JAR

```shell
mvn clean package
java -jar target/desafio-votacao-0.0.1-SNAPSHOT.jar
```

As variáveis devem estar no processo que inicia o JAR. O build executa os testes normais.

## 7. Docker

O Dockerfile usa Maven/JDK 21 no build, executa testes e usa Java 21 no runtime.

```shell
docker build -t desafio-votacao .
```

PostgreSQL no host, Windows/macOS:

```shell
docker run --rm -p 8080:8080 \
  -e DB_URL='jdbc:postgresql://host.docker.internal:5432/postgres' \
  -e DB_USERNAME='postgres' \
  -e DB_PASSWORD='<senha-local>' \
  desafio-votacao
```

Dentro do container, `localhost` não alcança o host. Em Linux, pode ser necessário `--add-host=host.docker.internal:host-gateway` ou uma rede Docker compartilhada.

## 8. Render

O serviço publicado é um ambiente de demonstração/homologação:

- API: https://desafio-votacao-v66k.onrender.com
- Swagger: https://desafio-votacao-v66k.onrender.com/swagger-ui.html
- OpenAPI: https://desafio-votacao-v66k.onrender.com/v3/api-docs

Deploy reproduzível:

1. Crie Render PostgreSQL.
2. Crie Web Service na mesma região.
3. Conecte `https://github.com/JulioMottaJr/desafio-votacao.git`.
4. Selecione branch `desafio-votacao` e runtime Docker.
5. Configure somente no painel:

```text
DB_URL=jdbc:postgresql://<internal-host>:5432/<database>
DB_USERNAME=<usuario-do-render>
DB_PASSWORD=<secret-do-render>
```

6. Deixe o Render fornecer `PORT`.
7. Acompanhe build, testes, Flyway e inicialização nos logs.

Não versione hostname, URL interna, usuário, senha ou token. O primeiro acesso pode sofrer cold start; limites e expiração dependem do plano.

## 9. Testes

```shell
mvn clean test
```

Na última validação documentada, os 93 testes passaram. A suíte normal não exige banco e exclui `VotoPerformanceTest`.

## 10. Benchmark

Exige PostgreSQL já migrado e as variáveis do banco:

```shell
mvn test -Dtest=VotoPerformanceTest
mvn test -Dtest=VotoPerformanceTest -Dperformance.votes=500000
```

O padrão é 100.000 votos. O teste usa `generate_series`, agregação real e rollback. Não inicia servidor/Flyway, não simula usuários simultâneos e não estabelece SLA.

## 11. Persistência

Crie dados, consulte, reinicie/redeploye somente o Web Service e consulte novamente. Os dados ficam no PostgreSQL, não no filesystem efêmero do container.

## 12. Troubleshooting

### Banco não conecta

Revise `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, database e rede. No Render, use conexão interna e mesma região.

### Senha ausente

`DB_PASSWORD` não possui default. Defina-a no processo que inicia Maven/JAR.

### Flyway ou Hibernate falha

Confira permissão para criar `votacao` e conclusão de V1, V2 e V3. Não edite migration aplicada; Hibernate não repara tabelas.

### Porta

Local usa 8080; Render injeta `PORT`.

### Swagger não abre ou primeiro acesso demora

Confirme a inicialização nos logs. Na nuvem, aguarde possível cold start.

### Raiz retorna 404

Esperado: não existe `GET /`. Use Swagger, OpenAPI ou `/api/v1`.

### Voto retorna 403, 404 ou 409

`403` e `404` podem vir do fake aleatório. `409` indica sessão repetida, voto duplicado ou sessão encerrada.

## 13. Checklist

- [ ] Branch `desafio-votacao`
- [ ] Java 21
- [ ] PostgreSQL disponível
- [ ] `DB_PASSWORD` definida
- [ ] Migrations V1–V3 concluídas
- [ ] Testes passando
- [ ] Swagger acessível
- [ ] Nenhum secret versionado
