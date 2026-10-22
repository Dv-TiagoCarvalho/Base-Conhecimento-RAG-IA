# Avaliação da qualidade das respostas

Em sistemas com IA, "parece que está funcionando" não basta. Este conjunto de perguntas com respostas
conhecidas mede duas coisas:

- **Busca**: o documento certo apareceu entre os trechos encontrados?
- **Resposta**: o modelo respondeu (modo `COMPLETA`) e o texto contém a informação esperada?

As duas últimas perguntas não têm resposta nos documentos. Nelas, o acerto é o sistema dizer que **não sabe**
(modo `SEM_CONTEXTO`) em vez de inventar.

## Como rodar

1. Suba o sistema e carregue os exemplos (veja o README principal):

   ```bash
   docker compose up -d
   ./scripts/carregar-exemplos.sh
   ```

2. Rode a avaliação. Ela usa o mesmo banco, então os três documentos precisam estar com status `PRONTO`:

   ```bash
   docker compose run --rm app --spring.profiles.active=avaliacao
   ```

   Formato da saída (os números são os que você medir):

   ```
   === Avaliação: 13 perguntas ===
   [busca OK |resposta OK ] COMPLETA         Quantos dias de férias eu tenho por ano?
   ...
   Busca encontrou o documento certo: X/13 (..%)
   Resposta correta:                  Y/13 (..%)
   ```

## Para que serve na prática

Toda vez que mudar algo importante, rode de novo e compare os números:

- `app.rag.tamanho-trecho` e `app.rag.sobreposicao` (como o texto é dividido)
- `app.rag.top-k` (quantos trechos vão para o modelo)
- `app.rag.similaridade-minima` (abaixo disso o trecho é ignorado; alto demais gera "não sei" em excesso,
  baixo demais manda trecho irrelevante)
- o provedor (`IA_PROVEDOR=ollama` ou `anthropic`) e o modelo (`OLLAMA_MODELO_CHAT`, `ANTHROPIC_MODELO`).
  Comparar o modelo local com o Claude no mesmo conjunto de perguntas rende uma ótima tabela no README.

Anote os resultados no README do projeto: mostrar que você mediu é o que diferencia este projeto de um tutorial.
