package br.com.tiagocarvalho.conhecimento.documento;

import java.util.UUID;

/** Um trecho devolvido pela busca semântica, com a similaridade (0 a 1) em relação à pergunta. */
public record TrechoEncontrado(UUID documentoId, String documento, int indice, String conteudo, double similaridade) {
}
