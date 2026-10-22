package br.com.tiagocarvalho.conhecimento.documento;

/**
 * Ciclo de vida de um documento:
 * PENDENTE -> PROCESSANDO -> PRONTO
 *                        \-> FALHOU (pode voltar para PENDENTE com "reprocessar")
 */
public enum StatusDocumento {
    PENDENTE,
    PROCESSANDO,
    PRONTO,
    FALHOU
}
