# ADR 002 — Processamento assíncrono com RabbitMQ, publicando após o commit

**Status:** aceita

## Contexto

Processar um documento (extrair texto, gerar dezenas de embeddings) pode levar de segundos a minutos.
Fazer isso dentro da requisição de upload deixaria o usuário esperando e prenderia uma thread e uma
conexão do banco.

## Decisão

1. O upload só **salva o documento** com status `PENDENTE` e responde `201`.
2. Uma mensagem é publicada no RabbitMQ e um **processador** consome a fila.
3. A publicação acontece em um `@TransactionalEventListener(phase = AFTER_COMMIT)`.
4. Em caso de erro, o documento vira `FALHOU` com o motivo e a mensagem vai para uma **fila de mensagens mortas** (DLQ).
5. O processador é **idempotente**.

## Por que publicar depois do commit

Se a mensagem fosse publicada dentro da transação, duas coisas poderiam dar errado:
- o processador recebe a mensagem antes do commit e não encontra o documento;
- a transação falha depois da publicação e o processador recebe uma mensagem de um documento que nunca existiu.

## Consequências e limitação

- Se o RabbitMQ estiver fora no exato momento após o commit, a mensagem se perde e o documento fica
  `PENDENTE`. Mitigação atual: rota de reprocessar. Solução completa: padrão **Transactional Outbox**
  (gravar a mensagem numa tabela na mesma transação e um publicador separado lê essa tabela).
- O RabbitMQ entrega "pelo menos uma vez"; por isso o processador precisa ser idempotente:
  ignora documento `PRONTO` e apaga trechos antigos antes de gravar novos.
- As chamadas ao Ollama acontecem fora da transação do banco, que fica aberta só para gravar.
