# ADR 003 — Modelo da resposta escolhido por configuração (Ollama ou Claude), atrás de interfaces

**Status:** aceita

## Contexto

O RAG precisa de dois modelos: um que gera embeddings (usado em todo trecho e em toda pergunta)
e um modelo de linguagem que escreve a resposta. O projeto precisa rodar sem custo para quem quiser testar,
mas também mostrar a integração com um modelo comercial de ponta.

## Decisão

- **Embeddings:** sempre `nomic-embed-text` no **Ollama**, local. É gratuito, não manda o conteúdo dos
  documentos para fora e é chamado muitas vezes (um embedding por trecho).
- **Resposta:** escolhida pela variável `IA_PROVEDOR` (que alimenta `spring.ai.model.chat`):
  - `ollama` (padrão): `qwen2.5:3b` local. Grátis e privado, mas mais lento e menos preciso.
  - `anthropic`: **Claude**. Pago por uso, segue melhor as instruções (citar a fonte, não inventar).
- `ModeloOllama` e `ModeloClaude` usam `@ConditionalOnProperty`, então só o adaptador escolhido é criado.
  A lógica comum fica em `ModeloViaSpringAi`.
- O código de negócio depende só das interfaces `GeradorDeEmbeddings` e `ModeloDeLinguagem`.

## Consequências

- Trocar de provedor é mudar uma variável; serviços e testes não mudam.
- Os testes usam `EmbeddingsFalsos` e `ModeloFalso`: rodam sem internet, sem custo e com resultado previsível.
- Modelos pequenos seguem o formato pior. Por isso o validador aceita `[1]` e `[Trecho 1]` e reconhece
  variações de "não encontrei". Mesmo assim, o modo `SOMENTE_TRECHOS` aparece mais com o modelo local.
- Com o Claude, os trechos enviados saem da infraestrutura local; para documentos sigilosos, o modo `ollama` é o indicado.
