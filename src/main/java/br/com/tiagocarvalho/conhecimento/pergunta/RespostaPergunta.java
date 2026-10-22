package br.com.tiagocarvalho.conhecimento.pergunta;

import java.util.List;
import java.util.UUID;

/**
 * Resposta devolvida ao usuário.
 *
 * @param resposta texto da resposta (ou aviso, quando o modelo não pôde responder)
 * @param modo     como a resposta foi produzida
 * @param fontes   trechos usados, numerados como no prompt ([1], [2]...)
 * @param doCache  true se veio do cache, sem chamar o modelo
 */
public record RespostaPergunta(String resposta, Modo modo, List<Fonte> fontes, boolean doCache) {

    public enum Modo {
        /** O Claude respondeu e a resposta passou na validação. */
        COMPLETA,
        /** O modelo falhou ou a resposta foi rejeitada: devolvemos só os trechos encontrados. */
        SOMENTE_TRECHOS,
        /** Nada relevante nos documentos (ou o próprio modelo disse que não encontrou). */
        SEM_CONTEXTO
    }

    public record Fonte(int numero, UUID documentoId, String documento, int trecho, String conteudo,
                        double similaridade, boolean citada) {
    }

    public RespostaPergunta vindaDoCache() {
        return new RespostaPergunta(resposta, modo, fontes, true);
    }
}
