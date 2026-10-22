# ADR 001 — pgvector em vez de um banco vetorial dedicado

**Status:** aceita

## Contexto

O sistema precisa guardar vetores (embeddings) e buscar os mais parecidos com o vetor da pergunta.
Existem bancos feitos só para isso (Pinecone, Qdrant, Weaviate) e existe a extensão `pgvector` para PostgreSQL.

## Decisão

Usar **PostgreSQL com pgvector**, com índice HNSW e distância de cosseno.

## Motivos

- O sistema já precisa de um banco relacional (usuários, documentos, status). Com pgvector, é **um banco só** para operar.
- Trechos e documentos ficam na mesma transação: apagar o documento apaga os trechos (`ON DELETE CASCADE`),
  e o processador grava trechos e muda o status de forma atômica.
- A busca pode filtrar por dados relacionais no mesmo SQL (`WHERE d.status = 'PRONTO'`).
- Para o volume esperado (milhares de trechos), o desempenho é mais do que suficiente.

## Consequências

- O JPA não conhece o tipo `vector`; a tabela `trecho` é acessada com `JdbcTemplate` e SQL escrito à mão.
- Com milhões de vetores ou muitas buscas por segundo, um banco vetorial dedicado pode valer a pena.
- A dimensão do vetor (768) está fixada na migração; trocar de modelo de embeddings exige nova migração e reprocessar tudo.
