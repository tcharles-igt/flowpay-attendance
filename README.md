# flowpay-attendance

Solucao Full Stack para o desafio tecnico FlowPay. O sistema distribui atendimentos entre times especializados, respeita a capacidade maxima por atendente, coloca casos em fila quando necessario e expõe um dashboard operacional para acompanhamento em tempo real.

## O que o avaliador consegue validar

- criacao de novos atendimentos;
- direcionamento automatico por assunto;
- atribuicao imediata quando existe capacidade;
- fila `WAITING` quando o time esta lotado;
- redistribuicao automatica ao finalizar um atendimento;
- dashboard com metricas, fila, atendentes e atendimentos em andamento.
- filtros por time e status para leitura operacional segmentada;
- metricas de tempo medio em fila e tempo medio de atendimento;
- feedback visual mais claro quando a API falha ou quando o painel exibe o ultimo snapshot valido.

## Pre-requisitos para execucao local

Para rodar sem Docker:

- Java 21
- Node.js `>=20.19.0` ou `>=22.12.0`
- npm `>=10`
- Docker e Docker Compose

O Docker continua sendo necessario mesmo no modo local se voce quiser subir apenas o PostgreSQL por container.

## Subida completa com Docker Compose

O fluxo principal de avaliacao e este:

```bash
cp .env.example .env
docker compose up --build
```

A aplicacao fica disponivel em:

- frontend: [http://localhost:4200](http://localhost:4200)
- backend: [http://localhost:8080](http://localhost:8080)
- swagger: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

O `docker-compose.yml` sobe:

- `postgres`
- `backend`
- `frontend`

As migrations do Flyway rodam automaticamente no bootstrap do backend, entao o banco sobe pronto para uso sem script manual adicional.

## Execucao local por modulo

### 1. Banco de dados

```bash
docker compose up -d postgres
```

### 2. Backend

```bash
cd backend
./mvnw -Dmaven.repo.local=.m2 spring-boot:run
```

API disponivel em `http://localhost:8080`.

### 3. Frontend

```bash
cd frontend
npm install
npm start
```

Dashboard disponivel em `http://localhost:4200`.

Em desenvolvimento, o frontend usa proxy para `/api` apontando para `http://localhost:8080`.

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

## Endpoints principais

### Atendimento

- `POST /api/attendances`
  Cria um novo atendimento.

Exemplo:

```json
{
  "customerName": "Maria Souza",
  "subject": "CARD_PROBLEM"
}
```

- `GET /api/attendances`
  Lista atendimentos por ordem de criacao.

- `GET /api/attendances/{id}`
  Busca um atendimento especifico.

- `PATCH /api/attendances/{id}/finish`
  Finaliza um atendimento em andamento e tenta redistribuir o proximo item elegivel da fila do mesmo time.

### Dashboard

- `GET /api/dashboard`
  Retorna metricas agregadas, tempos medios, resumo por time, atendentes, fila e atendimentos ativos.

### Atendentes

- `GET /api/attendants`
  Lista atendentes cadastrados.

## Regra de distribuicao implementada

### Mapeamento de assunto para time

- `CARD_PROBLEM` -> `CARDS`
- `LOAN_REQUEST` -> `LOANS`
- `OTHER` -> `OTHERS`

### Regra de atribuicao

1. O sistema identifica o time responsavel pelo assunto.
2. Procura um atendente ativo desse time com menos de 3 atendimentos `IN_PROGRESS`.
3. Se existir vaga, o atendimento entra como `IN_PROGRESS`.
4. Se nao existir vaga, o atendimento entra como `WAITING`.

### Regra de fila

- cada time consome apenas sua propria fila;
- uma finalizacao libera capacidade apenas para o mesmo time;
- o proximo item redistribuido e o `WAITING` mais antigo daquele time.

## Dados iniciais disponiveis

Ao subir a aplicacao, o banco recebe atendentes iniciais via migration. Eles existem para permitir validacao imediata da regra sem cadastro manual.

Times com atendentes seeded:

- `CARDS`
- `LOANS`
- `OTHERS`

Isso permite testar de imediato:

- criacao de atendimento com distribuicao direta;
- saturacao de capacidade;
- comportamento de fila;
- redistribuicao apos finalizacao.

## Decisoes tecnicas

- Backend e frontend separados.
  Facilita evolucao independente, isolamento de responsabilidades e avaliacao tecnica de cada camada.

- Regra centralizada no backend.
  A distribuicao, fila e redistribuicao ficam em um unico ponto confiavel, sem depender do frontend.

- Dashboard com polling.
  O frontend consulta a API periodicamente para manter a operacao atualizada sem complexidade extra de WebSocket.

- Swagger/OpenAPI com exemplos e contratos de erro.
  A navegacao da API fica autoexplicativa para validacao manual e para entendimento rapido do fluxo principal.

- PostgreSQL com Flyway.
  Garante ambiente reproduzivel, schema versionado e subida automatica do banco.

- Docker Compose como caminho principal.
  Reduz atrito para avaliacao e deixa a execucao completa reproduzivel com um unico comando.

## Como validar o fluxo principal

1. Suba tudo com `docker compose up --build`.
2. Abra o dashboard em [http://localhost:4200](http://localhost:4200).
3. Crie novos atendimentos pelo formulario da tela.
4. Observe o atendimento entrar em `IN_PROGRESS` quando houver vaga.
5. Gere volume suficiente para lotar um time e veja novos casos entrarem em `WAITING`.
6. Finalize um atendimento em andamento e confirme a redistribuicao automatica do item mais antigo da fila do mesmo time.

## Testes uteis

### Backend

```bash
cd backend
./mvnw -Dmaven.repo.local=.m2 test
```

### Frontend

```bash
cd frontend
npm test
```

## Limites da entrega

- nao existe autenticacao nem controle de acesso;
- o dashboard usa polling em vez de atualizacao em tempo real via push;
- nao ha observabilidade avancada, tracing ou metricas de producao;
- a estrategia de distribuicao atual prioriza capacidade disponivel e ordem de fila, sem heuristicas adicionais.

## Evolucoes futuras

- autenticacao e perfis operacionais;
- WebSocket ou SSE para atualizacao em tempo real;
- filtros e ordenacao avancada no dashboard;
- testes E2E cobrindo o fluxo completo via interface;
- observabilidade e healthchecks mais ricos no backend.

## SSE vs WebSocket

Para a proxima iteracao, a melhor evolucao inicial e SSE.

- SSE encaixa melhor no problema atual porque o dashboard e essencialmente somente leitura.
- A API pode publicar eventos de criacao, redistribuicao e finalizacao sem manter protocolo bidirecional.
- O frontend fica mais simples do que com WebSocket, inclusive para reconnect e degradacao para polling.

WebSocket passa a fazer sentido se a operacao evoluir para comandos colaborativos em tempo real, presenca de operadores ou sincronizacao bidirecional mais intensa.
