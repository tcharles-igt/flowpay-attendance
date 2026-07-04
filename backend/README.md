# Backend

Aplicacao Spring Boot responsavel pela API REST e pela regra de distribuicao de atendimentos do projeto FlowPay Attendance.

## Stack

- Java 21
- Spring Boot
- Maven
- JUnit
- Mockito

## Comportamento implementado

- `POST /api/attendances` identifica o time pelo assunto e tenta distribuicao imediata.
- Se houver capacidade no time, o atendimento nasce em `IN_PROGRESS`.
- Se o time estiver lotado, o atendimento nasce em `WAITING`.
- `PATCH /api/attendances/{id}/finish` finaliza o atendimento e promove automaticamente o `WAITING` mais antigo do mesmo time quando houver capacidade.
- A selecao de capacidade considera apenas atendentes ativos com menos de 3 atendimentos `IN_PROGRESS`.

## Comandos uteis

```bash
./mvnw -Dmaven.repo.local=.m2 test
./mvnw -Dmaven.repo.local=.m2 spring-boot:run
```

## Porta padrao

O backend esta configurado para subir na porta `8080`.

## Variaveis de ambiente

- `SERVER_PORT`: porta HTTP da API. Padrao `8080`.
- `SPRING_DATASOURCE_URL`: URL JDBC do PostgreSQL.
- `SPRING_DATASOURCE_USERNAME`: usuario do banco.
- `SPRING_DATASOURCE_PASSWORD`: senha do banco.

As migrations do Flyway rodam automaticamente na subida da aplicacao.

## Endpoints principais

- `POST /api/attendances`
- `GET /api/attendances`
- `GET /api/attendances/{id}`
- `PATCH /api/attendances/{id}/finish`
- `GET /api/dashboard`
- `GET /api/dashboard/events`
