# flowpay-attendance

Aplicacao full stack para gestao de atendimentos da FlowPay. A ideia do projeto e distribuir automaticamente cada solicitacao para o time correto, respeitar a capacidade maxima de cada atendente, colocar itens em fila quando nao houver vaga e oferecer uma visao operacional simples para acompanhamento da operacao em tempo real.

## Como iniciar com Docker

Para subir tudo do jeito mais facil, rode `docker compose up --build -d` na raiz do projeto. Isso sobe `postgres`, `backend` e `frontend` com os valores padrao do `docker-compose.yml`, sem precisar criar `.env`. Depois disso, o frontend fica em [http://localhost:4200](http://localhost:4200) e o backend com Swagger em [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html). Se quiser derrubar tudo depois, use `docker compose down`.

## Funcionalidades

- **Criacao de atendimentos**: registra novos atendimentos com nome do cliente, mensagem e assunto.
- **Direcionamento automatico**: cada assunto e mapeado para um time especifico (`CARDS`, `LOANS` ou `OTHERS`).
- **Distribuicao por capacidade**: quando existe vaga, o atendimento entra direto em `IN_PROGRESS` para um atendente elegivel.
- **Fila de espera**: quando o time esta lotado, o atendimento entra em `WAITING` e aguarda disponibilidade.
- **Redistribuicao automatica**: ao finalizar um atendimento, o sistema tenta puxar o item mais antigo da fila do mesmo time.
- **Gestao de atendentes**: permite listar, criar, editar e ativar ou desativar atendentes.
- **Dashboard operacional**: mostra resumo geral, fila, atendimentos em andamento e carga por atendente.
- **Atualizacao em tempo real**: o dashboard recebe atualizacoes via SSE sem precisar de refresh manual constante.
