# Perguntas de entrevista sobre este projeto

Se um projeto está no currículo, o entrevistador pode perguntar sobre qualquer parte dele.
Tente responder cada pergunta **com suas palavras, sem olhar a resposta**. Onde travar, volte ao código indicado.

---

## Visão geral

**1. Explique o projeto em 30 segundos.**
> Uma API em Spring Boot onde o admin envia documentos e os usuários fazem perguntas sobre eles. Os documentos
> são divididos em trechos, cada trecho vira um vetor (embedding) guardado no PostgreSQL com pgvector. Na
> pergunta, eu busco os trechos mais parecidos e mando para um modelo de linguagem (local no Ollama ou o Claude)
> responder só com base neles, citando a fonte.

**2. O que é RAG e por que não mandar o documento inteiro para o modelo?**
> Retrieval-Augmented Generation: buscar o contexto relevante antes de gerar a resposta. Mandar tudo é caro
> (paga por token), lento, esbarra no limite de contexto e piora a precisão, porque o modelo se perde em texto irrelevante.

**3. O que é um embedding?**
> Um vetor de números que representa o significado de um texto. Textos com sentido parecido geram vetores
> próximos, mesmo com palavras diferentes ("férias" e "dias de descanso").

## Busca vetorial

**4. Como a busca funciona no banco?** (`RepositorioTrechos.buscarSemelhantes`)
> O operador `<=>` do pgvector calcula a distância de cosseno. Ordeno pela distância, pego os `top-k` e
> converto em similaridade (`1 - distância`). Trechos abaixo da similaridade mínima são descartados.

**5. Por que distância de cosseno?**
> Ela compara a direção dos vetores, não o tamanho. Para embeddings de texto, a direção é o que carrega o significado.

**6. Para que serve o índice HNSW?**
> Sem índice, o banco compara a pergunta com todos os vetores (busca exata, lenta com muitos dados). O HNSW é
> um grafo que encontra vizinhos próximos rapidamente, de forma aproximada: troca um pouco de precisão por muita velocidade.

**7. Por que pgvector e não Pinecone/Qdrant?** → [ADR 001](adr/001-pgvector-em-vez-de-banco-vetorial-dedicado.md)

**8. Por que dividir o texto em trechos? Por que sobreposição?** (`DivisorDeTexto`)
> Um embedding de um documento inteiro "dilui" o significado; trechos menores dão buscas mais precisas.
> A sobreposição evita que uma informação que cai na fronteira entre dois trechos perca o contexto.

**9. Como você escolheu 800 caracteres, top-k 4 e similaridade mínima 0,5?**
> Foram valores iniciais. Para ajustar, uso o conjunto de avaliação em `avaliacao/`: mudo um parâmetro,
> rodo de novo e comparo as taxas de acerto. (Diga os números que você mediu!)

## Assíncrono e consistência

**10. Por que usar fila em vez de processar no upload?** → [ADR 002](adr/002-processamento-assincrono-com-rabbitmq.md)

**11. O que acontece se o processamento falhar?**
> O documento fica `FALHOU` com a mensagem de erro, a mensagem é rejeitada sem voltar para a fila e vai para a
> DLQ (fila de mensagens mortas). O admin pode reprocessar.

**12. Por que publicar a mensagem só depois do commit?**
> Para o processador nunca receber a mensagem de um documento que ainda não foi gravado (ou cuja transação falhou).

**13. E se o RabbitMQ cair logo depois do commit?**
> A mensagem se perde e o documento fica `PENDENTE`. Hoje dá para reprocessar manualmente. A solução completa é o
> padrão Transactional Outbox.

**14. O que é idempotência e onde ela aparece aqui?**
> Executar a mesma operação várias vezes dá o mesmo resultado. O RabbitMQ pode entregar a mesma mensagem duas vezes;
> o processador ignora documento `PRONTO` e apaga os trechos antigos antes de gravar. O upload recusa arquivo repetido pelo hash SHA-256.

**15. Por que as chamadas ao Ollama ficam fora da transação?**
> Transação aberta segura uma conexão do pool. Se ela ficasse aberta durante chamadas de rede lentas, poucas
> requisições simultâneas esgotariam as conexões.

## IA e confiabilidade

**16. Como você evita que o modelo invente?** → [ADR 004](adr/004-nao-confiar-cegamente-no-modelo.md)

**17. O que é prompt injection e como você se protege?**
> Texto dentro de um documento tentando dar ordens ao modelo ("ignore as instruções e..."). Os trechos vão
> delimitados e as instruções dizem que são material de consulta. Além disso, só admin envia documentos.
> Isso reduz, mas não elimina o risco.

**18. Por que usar interfaces (`ModeloDeLinguagem`, `GeradorDeEmbeddings`)?**
> Para não acoplar a regra de negócio a um fornecedor e para testar com versões falsas, sem rede e sem custo.

**19. Como o sistema troca entre o modelo local e o Claude? Quais as vantagens de cada um?** → [ADR 003](adr/003-modelo-de-resposta-configuravel.md)
> Uma variável (`IA_PROVEDOR`) ativa um dos dois adaptadores com `@ConditionalOnProperty`. Local: grátis e privado,
> porém mais lento e menos preciso. Claude: pago, mais rápido e segue melhor as instruções.

## Segurança

**20. Como funciona a autenticação?** (`ServicoJwt`, `FiltroJwt`, `SegurancaConfig`)
> No login, gero um JWT assinado com HMAC-SHA256 contendo id, e-mail e papel. Em cada requisição, o filtro valida a
> assinatura e a expiração e coloca o usuário no contexto do Spring Security. As regras de acesso ficam no `SegurancaConfig`.

**21. Por que CSRF está desligado?**
> CSRF explora cookies enviados automaticamente pelo navegador. Aqui não há sessão nem cookie: o token vai no cabeçalho
> `Authorization`, que um site malicioso não consegue adicionar.

**22. Por que BCrypt e por que o limite de 72 na senha?**
> BCrypt é lento de propósito e tem salt, o que dificulta ataques de força bruta. Ele só considera os primeiros 72 bytes.

**23. Qual a diferença entre 401 e 403?**
> 401: não sei quem você é (sem token ou token inválido). 403: sei quem você é, mas você não tem permissão
> (usuário comum tentando enviar documento).

## Testes e operação

**24. Como você testa algo que depende de IA?**
> Unitários para as partes puras (divisor, validador, cache, limitador). Integração com Testcontainers (Postgres e
> RabbitMQ reais), trocando só a IA por implementações falsas determinísticas. E o conjunto de avaliação para a qualidade real.

**25. Por que Testcontainers e não H2?**
> O H2 não tem pgvector nem o mesmo SQL do PostgreSQL. Testar contra o banco real evita "passa no teste, quebra em produção".

**26. Como você saberia que o sistema está lento em produção?**
> Métricas no `/actuator/prometheus`: `rag.embedding.tempo`, `rag.busca.tempo`, `rag.llm.tempo`,
> `rag.respostas{modo}` e `documentos.processados{resultado}`.

**27. O que muda se a aplicação tiver 3 instâncias?**
> O cache e o limitador ficam em memória por instância, então cada uma teria seu próprio cache e limite. Precisaria
> de Redis. O processamento já escala: várias instâncias consomem a mesma fila.

**28. O que você faria diferente ou melhoraria?** → seção "Limitações conhecidas" do README.

---

### Desafio final

Abra `ServicoPerguntas.responder` e explique em voz alta, linha por linha, o que acontece. Depois tente
implementar sozinho uma melhoria pequena, por exemplo guardar o histórico de perguntas numa tabela nova (migração
`V2`), e escrever o teste dela. Isso transforma "projeto feito com IA" em "projeto que eu domino".
