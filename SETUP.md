# Setup

O guia atualizado está no [README](README.md#configuração-e-execução).

Requer Java 21, Maven 3.6.3+ e PostgreSQL. O padrão de DB_URL é
`jdbc:postgresql://localhost:5432/postgres`; DB_USERNAME é `postgres`;
DB_PASSWORD deve ser fornecida pelo ambiente, sem gravar credenciais no projeto.
Flyway aplica as migrations no schema `votacao`; JPA valida a estrutura existente.

Na raiz, execute `mvn clean test` e depois `mvn spring-boot:run`.
Swagger UI: `http://localhost:8080/swagger-ui.html`.
Para empacotamento, exemplos de requests, benchmark e decisões de contrato,
consulte o README, que é a referência de setup mantida do projeto.