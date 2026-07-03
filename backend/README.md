# Backend

Aplicacao Spring Boot responsavel pela API REST e pela regra de distribuicao de atendimentos do projeto FlowPay Attendance.

## Stack

- Java 21
- Spring Boot
- Maven
- JUnit
- Mockito

## Estrutura atual

O modulo ja possui a base minima para evolucao:

- `pom.xml` configurado com Spring Boot;
- classe principal `FlowpayAttendanceBackendApplication`;
- `application.yaml` com o nome da aplicacao;
- teste inicial de contexto para validar o bootstrap da aplicacao.

## Comandos uteis

```bash
./mvnw test
./mvnw spring-boot:run
```

## Porta padrao

O backend esta configurado para subir na porta `8080`.

## Proximos passos

- definir dependencias de persistencia e documentacao da API;
- modelar dominios como `attendant` e `attendance`;
- configurar banco de dados e migracoes;
- implementar a regra de distribuicao dos atendimentos.
