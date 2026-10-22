package br.com.tiagocarvalho.conhecimento.documento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface RepositorioDocumentos extends JpaRepository<Documento, UUID> {

    boolean existsByHashConteudo(String hashConteudo);

    /** Monta a resposta direto na consulta, sem carregar a coluna com o arquivo inteiro. */
    @Query("""
            select new br.com.tiagocarvalho.conhecimento.documento.DocumentoResposta(
                d.id, d.titulo, d.nomeArquivo, d.tipo, d.status, d.quantidadeTrechos,
                d.mensagemErro, d.criadoEm, d.processadoEm)
            from Documento d
            order by d.criadoEm desc
            """)
    List<DocumentoResposta> listarResumos();
}
