# Executar a estrutura inicial

Requisitos: JDK 21, Maven 3.6.3 ou superior e PostgreSQL acessível.
Spring Boot: 4.1.1. Pacote base: `br.com.desafiovotacao`.
O README.md original contém o enunciado do desafio.

## Banco de dados

Crie previamente um banco PostgreSQL e forneça as variáveis de ambiente:

| Variável | Valor padrão | Finalidade |
| --- | --- | --- |
| DB_URL | jdbc:postgresql://localhost:5432/desafio_votacao | URL JDBC do banco existente |
| DB_USERNAME | postgres | Usuário do banco |
| DB_PASSWORD | Sem padrão; obrigatória | Senha do banco |

Não salve credenciais reais no repositório. Arquivos `.env` não são carregados
automaticamente pelo Spring Boot; forneça as variáveis pelo terminal ou pela IDE.
A aplicação precisa conectar ao PostgreSQL ao iniciar. A criação automática de
tabelas está desativada (`ddl-auto=none`).

## Comandos

Execute na raiz do repositório, com as variáveis acima disponíveis:

```text
mvn clean verify
mvn spring-boot:run
```

Para executar o JAR após o build:

```text
java -jar target/desafio-votacao-0.0.1-SNAPSHOT.jar
```

O servidor utiliza a porta padrão 8080. Não há endpoints de negócio nesta etapa.

## Organização

A classe principal está no pacote raiz. Os diretórios `controller`, `dto`,
`entity`, `repository`, `service`, `impl`, `client`, `exception` e `config`
estão vazios intencionalmente. O Git não versiona diretórios vazios; eles
passarão a integrar o repositório quando receberem implementações reais.
O diretório `src/test/java/br/com/desafiovotacao` também está reservado e vazio.

Spring Boot Test inclui JUnit Jupiter, Mockito e AssertJ. Ainda não existem
casos de teste: não há regras de negócio implementadas nesta estrutura.
Lombok está configurado como processador de anotações e excluído do JAR executável.

## Validação nesta preparação

O XML do POM e a estrutura foram inspecionados. A compilação e os testes não
puderam ser executados na sessão de preparação porque o Java não estava
acessível e o Maven informou JAVA_HOME inválido. Execute `mvn clean verify`
no ambiente com JDK 21 disponível antes de considerar o build validado.
