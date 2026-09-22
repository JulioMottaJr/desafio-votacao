# Desafio Votação API

API REST para cadastro de pautas, sessões, votos e contabilização, com contratos JSON para aplicativos mobile. O enunciado da empresa está preservado integralmente ao final; as decisões abaixo descrevem a implementação.

## Tecnologias e pré-requisitos

Java 21, Maven 3.6.3+, PostgreSQL acessível, Spring Boot 4.1.1, Spring MVC, JPA/Hibernate, Bean Validation, Flyway, SLF4J/Logback, springdoc OpenAPI 3.1.1, JUnit Jupiter, Mockito e AssertJ.

## Configuração e execução

Execute na raiz do repositório. O database deve existir previamente; o padrão é `postgres`. Configure no terminal ou no IntelliJ:

| Variável | Padrão | Uso |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/postgres` | Database PostgreSQL existente |
| `DB_USERNAME` | `postgres` | Usuário com permissão para migrations |
| `DB_PASSWORD` | Sem padrão, obrigatória | Senha, fornecida fora do repositório |

Exemplo PowerShell, com leitura da senha sem mostrá-la:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5432/postgres'
$env:DB_USERNAME = 'postgres'
$env:DB_PASSWORD = Read-Host 'Senha do PostgreSQL' -MaskInput
mvn clean test
mvn spring-boot:run
```

`-MaskInput` requer PowerShell 7.1+. Em outros ambientes, configure a variável pelo mecanismo seguro do terminal/IDE. Arquivos `.env` não são carregados automaticamente. Variáveis definidas somente no IntelliJ não são herdadas por outros terminais.

Flyway cria o schema `votacao` e aplica `src/main/resources/db/migration` automaticamente ao iniciar. JPA utiliza `ddl-auto=validate`, sem criar tabelas. Não edite migrations já aplicadas.

Alternativa para empacotar e executar:

```shell
mvn clean package
java -jar target/desafio-votacao-0.0.1-SNAPSHOT.jar
```

Escolha uma forma de execução por vez. A porta padrão é 8080. Não há autenticação implementada, conforme a abstração permitida no desafio.

## Endpoints

| Método | Rota | Finalidade |
|---|---|---|
| POST | `/api/v1/pautas` | Criar pauta (201) |
| POST | `/api/v1/pautas/{pautaId}/sessoes` | Abrir sessão (201) |
| POST | `/api/v1/pautas/{pautaId}/votos` | Registrar voto (201) |
| GET | `/api/v1/pautas/{pautaId}/resultado` | Obter `pautaId`, `sim`, `nao`, `total` |
| GET | `/api/v1/mobile/pautas/nova` | FORMULARIO de pauta |
| GET | `/api/v1/mobile/pautas/{pautaId}/sessoes/nova` | FORMULARIO de sessão |
| GET | `/api/v1/mobile/pautas/{pautaId}/votos/novo` | SELECAO de voto |

As telas mobile retornam ações POST com URLs relativas à origem da API. Não validam existência da pauta ao montar a tela. Não existem endpoints GET individuais para pauta, sessão ou voto.

### Exemplos de bodies JSON

Criar pauta:

```json
{"titulo":"Aquisição de equipamentos","descricao":"Deliberar sobre a compra."}
```

Abrir sessão (ou `{}` para usar 1 minuto):

```json
{"duracaoMinutos":5}
```

Votar (identificador fictício; o Fake decide aleatoriamente):

```json
{"associadoId":"00000000000","opcao":"SIM"}
```

## Regras e integridade

Título e descrição são obrigatórios, limitados a 150 e 1000 caracteres. A duração da sessão é um inteiro de pelo menos 1 minuto; ausência/null usa 1 minuto. Existe uma sessão por pauta. No instante de encerramento ou depois dele, novos votos são rejeitados.

A opção aceita exatamente `SIM` ou `NAO`; `associadoId` é obrigatório e possui limite de 100 caracteres. Cada associado vota uma vez por pauta. A UNIQUE `(pauta_id, associado_id)` protege inclusive contra concorrência, além da verificação prévia. `saveAndFlush` permite traduzir a violação dessa constraint dentro da operação transacional.

A contabilização usa agregação no PostgreSQL e não carrega os votos em memória. Pode ser consultada durante a sessão; sem votos retorna zeros.

## CPF e elegibilidade: decisão da implementação

O identificador `associadoId` é enviado como CPF ao `AssociadoClient`, mantendo o JSON e o banco existentes. O Fake é local, sem HTTP externo ou validação matemática, e sorteia validade e elegibilidade. O mesmo identificador pode obter resultados diferentes em chamadas diferentes.

- CPF considerado inválido: 404.
- CPF válido e `ABLE_TO_VOTE`: permite persistir o voto.
- CPF válido e `UNABLE_TO_VOTE`: 403, sem persistir.

O enunciado tem ambiguidade: seu texto diferencia CPF inválido de inaptidão, mas o comentário junto a UNABLE_TO_VOTE menciona 404. A separação 404/403 é uma decisão de contrato desta implementação; o original não determina inequivocamente 403.

Validações locais precedem o Fake: pauta, sessão, encerramento e duplicidade. Por isso, duplicidade ou sessão encerrada retornam 409 antes da consulta de elegibilidade.

## Erros e logs

Respostas de erro utilizam este formato, sem stack trace:

```json
{"timestamp":"2026-09-21T10:00:00","status":403,"error":"Forbidden","message":"Associado não habilitado para votar.","path":"/api/v1/pautas/1/votos"}
```

Erros de validação (400) também podem conter `fields`, um mapa campo/mensagem. Pauta/sessão ausente ou CPF inválido retornam 404; sessão repetida, voto duplicado e sessão encerrada retornam 409. Falhas inesperadas retornam 500 genérico, com diagnóstico no servidor.

Logs usam SLF4J/Logback no console e em `logs/desafio-votacao.log`, com rotação nativa do Spring Boot. `logs/` não é versionado. Eventos de negócio usam INFO, rejeições relevantes WARN e contabilização DEBUG. Não são registrados CPF, associadoId ou credenciais nos logs de negócio.

## Swagger / OpenAPI

Com a aplicação executando na configuração padrão:

- Swagger UI: http://localhost:8080/swagger-ui.html (redireciona para `/swagger-ui/index.html`).
- OpenAPI JSON: http://localhost:8080/v3/api-docs.

A interface permite executar chamadas reais; operações POST podem criar dados. Os endpoints técnicos de documentação não usam o prefixo funcional `/api/v1`.

## Testes

```shell
mvn test
mvn clean test
```

A suíte normal usa testes unitários e MockMvc, incluindo geração OpenAPI e recursos Swagger UI sem servidor HTTP ou PostgreSQL. O benchmark pesado é excluído dessas execuções.

## Estrutura

`controller` e `dto`: contratos HTTP/mobile; `service`: regras; `repository` e `entity`: persistência; `client`: integração fake; `exception`: erros; `config`: Clock e OpenAPI.

## Teste de performance

Com o PostgreSQL configurado e as migrations do projeto já aplicadas, execute:

```shell
mvn test -Dtest=VotoPerformanceTest
mvn test -Dtest=VotoPerformanceTest -Dperformance.votes=500000
```

O padrão é 100.000 votos. Use no terminal as mesmas variáveis `DB_URL`,
`DB_USERNAME` e `DB_PASSWORD` da aplicação (as variáveis do IntelliJ não são
automaticamente compartilhadas com o terminal). Não é necessário iniciar a API.
O teste não inicia servidor HTTP, não executa Flyway e apenas valida o schema existente.

A pauta, sessão e votos são gerados automaticamente por SQL com `generate_series`,
sem cadastro manual, chamadas HTTP ou uma lista de entidades em memória.
A distribuição é determinística: metade SIM e o restante NAO. A contabilização
executa o método JPQL real `VotoRepository.contabilizarPorPautaId`.
O relatório apresenta preparação e contabilização sem limite arbitrário de tempo.
O teste mede uma consulta após inserção, com dados em cache; não é um teste de
throughput HTTP, concorrência ou latência do Client.

Toda a massa permanece em uma transação sem commit e é desfeita por rollback,
inclusive em caso de falha. Nenhum dado anterior é apagado. As sequências de IDs
avançam mesmo com rollback; a execução consome recursos e pode gerar espaço
recuperável pelo autovacuum. Evite executá-la durante medições concorrentes.
`mvn test` e `mvn clean test` normais excluem este benchmark; `-Dtest=VotoPerformanceTest`
o seleciona explicitamente.

## Versionamento da API

A estratégia é versionamento pela URL: `/api/v1/...`. Pautas, sessões, votos,
resultado e contratos mobile usam esse prefixo. Mudanças compatíveis permanecem
na v1; mudanças que quebrem contratos exigirão uma nova versão, com transição
documentada para os consumidores. Nenhuma v2 foi criada. Endpoints técnicos de
infraestrutura não precisam seguir o versionamento funcional.

---

# Enunciado original do desafio (preservado)

# Votação

## Objetivo

No cooperativismo, cada associado possui um voto e as decisões são tomadas em assembleias, por votação. Imagine que você deve criar uma solução para dispositivos móveis para gerenciar e participar dessas sessões de votação.
Essa solução deve ser executada na nuvem e promover as seguintes funcionalidades através de uma API REST:

- Cadastrar uma nova pauta
- Abrir uma sessão de votação em uma pauta (a sessão de votação deve ficar aberta por
  um tempo determinado na chamada de abertura ou 1 minuto por default)
- Receber votos dos associados em pautas (os votos são apenas 'Sim'/'Não'. Cada associado
  é identificado por um id único e pode votar apenas uma vez por pauta)
- Contabilizar os votos e dar o resultado da votação na pauta

Para fins de exercício, a segurança das interfaces pode ser abstraída e qualquer chamada para as interfaces pode ser considerada como autorizada. A solução deve ser construída em java, usando Spring-boot, mas os frameworks e bibliotecas são de livre escolha (desde que não infrinja direitos de uso).

É importante que as pautas e os votos sejam persistidos e que não sejam perdidos com o restart da aplicação.

O foco dessa avaliação é a comunicação entre o backend e o aplicativo mobile. Essa comunicação é feita através de mensagens no formato JSON, onde essas mensagens serão interpretadas pelo cliente para montar as telas onde o usuário vai interagir com o sistema. A aplicação cliente não faz parte da avaliação, apenas os componentes do servidor. O formato padrão dessas mensagens será detalhado no anexo 1.

## Como proceder

Por favor, **CLONE** o repositório e implemente sua solução, ao final, notifique a conclusão e envie o link do seu repositório clonado no GitHub, para que possamos analisar o código implementado.

Lembre de deixar todas as orientações necessárias para executar o seu código.

### Tarefas bônus

- Tarefa Bônus 1 - Integração com sistemas externos
  - Criar uma Facade/Client Fake que retorna aleátoriamente se um CPF recebido é válido ou não.
  - Caso o CPF seja inválido, a API retornará o HTTP Status 404 (Not found). Você pode usar geradores de CPF para gerar CPFs válidos
  - Caso o CPF seja válido, a API retornará se o usuário pode (ABLE_TO_VOTE) ou não pode (UNABLE_TO_VOTE) executar a operação. Essa operação retorna resultados aleatórios, portanto um mesmo CPF pode funcionar em um teste e não funcionar no outro.

```
// CPF Ok para votar
{
    "status": "ABLE_TO_VOTE
}
// CPF Nao Ok para votar - retornar 404 no client tb
{
    "status": "UNABLE_TO_VOTE
}
```

Exemplos de retorno do serviço

### Tarefa Bônus 2 - Performance

- Imagine que sua aplicação possa ser usada em cenários que existam centenas de
  milhares de votos. Ela deve se comportar de maneira performática nesses
  cenários
- Testes de performance são uma boa maneira de garantir e observar como sua
  aplicação se comporta

### Tarefa Bônus 3 - Versionamento da API

○ Como você versionaria a API da sua aplicação? Que estratégia usar?

## O que será analisado

- Simplicidade no design da solução (evitar over engineering)
- Organização do código
- Arquitetura do projeto
- Boas práticas de programação (manutenibilidade, legibilidade etc)
- Possíveis bugs
- Tratamento de erros e exceções
- Explicação breve do porquê das escolhas tomadas durante o desenvolvimento da solução
- Uso de testes automatizados e ferramentas de qualidade
- Limpeza do código
- Documentação do código e da API
- Logs da aplicação
- Mensagens e organização dos commits

## Dicas

- Teste bem sua solução, evite bugs
- Deixe o domínio das URLs de callback passiveis de alteração via configuração, para facilitar
  o teste tanto no emulador, quanto em dispositivos fisicos.
  Observações importantes
- Não inicie o teste sem sanar todas as dúvidas
- Iremos executar a aplicação para testá-la, cuide com qualquer dependência externa e
  deixe claro caso haja instruções especiais para execução do mesmo
  Classificação da informação: Uso Interno

## Anexo 1

### Introdução

A seguir serão detalhados os tipos de tela que o cliente mobile suporta, assim como os tipos de campos disponíveis para a interação do usuário.

### Tipo de tela – FORMULARIO

A tela do tipo FORMULARIO exibe uma coleção de campos (itens) e possui um ou dois botões de ação na parte inferior.

O aplicativo envia uma requisição POST para a url informada e com o body definido pelo objeto dentro de cada botão quando o mesmo é acionado. Nos casos onde temos campos de entrada
de dados na tela, os valores informados pelo usuário são adicionados ao corpo da requisição. Abaixo o exemplo da requisição que o aplicativo vai fazer quando o botão “Ação 1” for acionado:

```
POST http://seudominio.com/ACAO1
{
    “campo1”: “valor1”,
    “campo2”: 123,
    “idCampoTexto”: “Texto”,
    “idCampoNumerico: 999
    “idCampoData”: “01/01/2000”
}
```

Obs: o formato da url acima é meramente ilustrativo e não define qualquer padrão de formato.

### Tipo de tela – SELECAO

A tela do tipo SELECAO exibe uma lista de opções para que o usuário.

O aplicativo envia uma requisição POST para a url informada e com o body definido pelo objeto dentro de cada item da lista de seleção, quando o mesmo é acionado, semelhando ao funcionamento dos botões da tela FORMULARIO.
