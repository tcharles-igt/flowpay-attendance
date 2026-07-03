# flowpay-attendance

Sistema Full Stack desenvolvido para o desafio tecnico FlowPay. A proposta da solucao e distribuir atendimentos entre times especializados, controlar filas quando os atendentes atingem sua capacidade maxima e disponibilizar um dashboard para monitoramento operacional em tempo real.

## Objetivo

Construir uma aplicacao para:

- criar novos atendimentos;
- classificar automaticamente o assunto;
- direcionar o atendimento para o time responsavel;
- respeitar a capacidade maxima de atendimentos simultaneos por atendente;
- manter fila de espera quando nao houver capacidade disponivel;
- exibir um dashboard com visao operacional da central.

## Escopo da solucao

A entrega esta organizada para cobrir os principais blocos do desafio:

- API REST para criacao, distribuicao, listagem e finalizacao de atendimentos;
- dashboard web para acompanhamento operacional;
- banco de dados para persistencia das informacoes;
- dados iniciais para facilitar validacao local;
- documentacao com instrucoes de execucao, regras implementadas e decisoes tecnicas.

Autenticacao e controle de acesso nao fazem parte do escopo inicial, porque o foco da prova esta na regra de distribuicao, na API e no dashboard.

## Stack utilizada

### Backend

- Java 21
- Spring Boot
- Maven
- JUnit
- Mockito

### Frontend

- Angular
- TypeScript
- RxJS
- SCSS
- npm

### Infraestrutura

- PostgreSQL
- Docker
- Docker Compose

## Estrutura do repositorio

```txt
flowpay-attendance/
├── backend/
│   ├── src/
│   ├── pom.xml
│   └── mvnw
├── frontend/
│   ├── src/
│   ├── package.json
│   └── angular.json
└── README.md
```

## Portas padrao

- backend: `8080`
- frontend: `4200`
- PostgreSQL: `5432`

## Regras de negocio

### Times de atendimento

- `CARDS`: atendimentos relacionados a problemas com cartao.
- `LOANS`: atendimentos relacionados a solicitacoes de emprestimo.
- `OTHERS`: demais assuntos.

### Capacidade dos atendentes

Cada atendente pode atender no maximo `3` clientes simultaneamente.

Quando todos os atendentes de um time estiverem ocupados, novos atendimentos desse time devem permanecer em fila. Assim que um atendimento for finalizado, o proximo item da fila do mesmo time deve ser redistribuido automaticamente.

## Fluxo funcional esperado

### Criacao de atendimento

```txt
1. O sistema recebe uma nova solicitacao.
2. O assunto do atendimento e identificado.
3. O time responsavel e definido automaticamente.
4. O sistema procura um atendente ativo com capacidade disponivel.
5. Se houver disponibilidade, o atendimento e atribuido e passa para IN_PROGRESS.
6. Se nao houver disponibilidade, o atendimento permanece em WAITING na fila do time.
```

### Finalizacao de atendimento

```txt
1. O atendimento em andamento e finalizado.
2. A vaga do atendente e liberada.
3. O sistema procura o proximo atendimento WAITING do mesmo time.
4. Se existir item em fila, ele e atribuido automaticamente ao atendente liberado.
```

## Modelagem principal prevista

### Attendant

```txt
id
name
team
active
createdAt
updatedAt
```

### Attendance

```txt
id
customerName
subject
team
status
attendant
createdAt
startedAt
finishedAt
```

## Execucao com Docker

O caminho principal de avaliacao e subir tudo com um unico comando:

```bash
cp .env.example .env
docker compose up --build
```

Servicos expostos:

- frontend: [http://localhost:4200](http://localhost:4200)
- backend: [http://localhost:8080](http://localhost:8080)
- swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- PostgreSQL: `localhost:5432`

O `docker-compose.yml` sobe:

- `postgres` com volume persistente e `healthcheck`;
- `backend` Spring Boot apontando para o banco containerizado;
- `frontend` Angular servido por Nginx e integrado ao backend por proxy em `/api`.

As migrations do Flyway rodam automaticamente no bootstrap do backend, entao o banco sobe pronto para uso.

## Variaveis de ambiente

Os valores padrao estao em [.env.example](/Users/tcharlesbarreto/Dev%20Repos/flowpay-attendance/.env.example).

Variaveis principais:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_PORT`
- `BACKEND_PORT`
- `FRONTEND_PORT`
- `SPRING_JPA_SHOW_SQL`

## Execucao local sem Docker

1. Suba apenas o banco:

```bash
docker compose up -d postgres
```

2. Rode o backend:

```bash
cd backend
./mvnw -Dmaven.repo.local=.m2 spring-boot:run
```

3. Rode o frontend:

```bash
cd frontend
npm install
npm start
```

## Comandos uteis

### Backend

```bash
cd backend
./mvnw -Dmaven.repo.local=.m2 test
./mvnw -Dmaven.repo.local=.m2 spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

### Banco de dados

```bash
docker compose up --build
docker compose logs -f
```

## Observacoes

- O frontend foi gerado com Angular CLI e depende de uma versao de Node compativel com a configuracao do projeto.
- O backend usa PostgreSQL em execucao normal e H2 apenas nos testes.
- Se quiser reproduzir o ambiente de avaliacao, prefira `docker compose up --build`.
