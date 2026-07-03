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

### Infraestrutura prevista

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

## Status atual do repositorio

No momento, a base inicial do projeto ja foi criada com dois aplicativos separados:

- `backend/`: bootstrap Spring Boot com estrutura Maven e teste inicial de contexto;
- `frontend/`: bootstrap Angular com estrutura base da aplicacao;
- `.gitignore` raiz: configurado para ignorar artefatos comuns de build, IDE e sistema operacional.

Os proximos passos da evolucao devem incluir modelagem de dominio, API REST, persistencia, dashboard e conteinerizacao.

## Ordem recomendada de execucao

Para evoluir ou validar a solucao localmente, a ordem recomendada e:

1. Preparar o backend Spring Boot.
2. Preparar o frontend Angular.
3. Configurar banco de dados PostgreSQL.
4. Implementar a regra de distribuicao dos atendimentos.
5. Expor endpoints da API.
6. Construir o dashboard web.
7. Consolidar Docker, dados iniciais e documentacao final.

## Comandos uteis

### Backend

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm start
```

## Observacoes

- O frontend foi gerado com Angular CLI e depende de uma versao de Node compativel com a configuracao do projeto.
- As portas padrao ja foram reservadas para manter consistencia entre execucao local, Docker e futura documentacao operacional.
