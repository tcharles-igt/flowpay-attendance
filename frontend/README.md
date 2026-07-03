# Frontend

Aplicacao Angular responsavel pelo dashboard operacional do projeto FlowPay Attendance.

## Stack

- Angular
- TypeScript
- RxJS
- SCSS
- npm

## Estrutura atual

O modulo ja foi inicializado com a base necessaria para evolucao:

- configuracao do Angular CLI em `angular.json`;
- aplicacao base em `src/`;
- scripts de desenvolvimento, build e teste em `package.json`;
- configuracao de TypeScript pronta para evolucao do dashboard.

## Comandos uteis

```bash
npm install
npm start
npm run build
npm test
```

## Porta padrao

O frontend esta configurado para rodar na porta `4200`.

## Requisito de ambiente

Use Node.js `>=20.19.0` ou `>=22.12.0`, conforme exigido pela versao atual do Angular CLI configurada neste projeto.

## Proximos passos

- construir layout inicial do dashboard;
- integrar consumo da API do backend;
- criar componentes para fila, atendimentos em andamento e indicadores;
- definir estrategia visual e estados de carregamento.
