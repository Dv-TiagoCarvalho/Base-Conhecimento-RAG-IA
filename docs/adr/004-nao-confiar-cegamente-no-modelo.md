# ADR 004 — Não confiar cegamente no modelo

**Status:** aceita

## Contexto

Modelos de linguagem podem inventar informações, ignorar instruções, ficar fora do ar ou ser manipulados
por texto escondido dentro de um documento (*prompt injection*).

## Decisão

1. **Sem contexto, sem chamada:** se a busca não encontra trecho acima da similaridade mínima, a resposta
   é "não encontrei" e o Claude nem é chamado. Isso reduz custo e corta a principal fonte de invenção.
2. **Instruções explícitas:** responder só com os trechos, citar `[n]`, e uma frase exata para quando não souber.
3. **Trechos delimitados:** os trechos vão dentro de `<trechos>` e as instruções dizem que são material de
   consulta, não ordens.
4. **Validação da resposta** (`ValidadorDeResposta`): recusa resposta vazia, sem citação ou que cite um trecho
   que não existe (por exemplo `[7]` quando só 4 foram enviados).
5. **Plano B:** se o modelo falhar ou a resposta for recusada, a API devolve `SOMENTE_TRECHOS` com os trechos
   encontrados, em vez de erro 500.
6. Só respostas `COMPLETA` entram no cache, para um erro temporário não ficar "grudado".

## Consequências

- A validação de citações é sintática: confirma que a fonte existe, não que a frase está correta. Medir isso
  é papel do conjunto de avaliação (`avaliacao/`).
- As defesas contra prompt injection reduzem o risco, mas não o eliminam. Por isso só administradores enviam documentos.
