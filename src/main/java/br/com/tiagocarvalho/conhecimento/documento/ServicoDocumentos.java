package br.com.tiagocarvalho.conhecimento.documento;

import br.com.tiagocarvalho.conhecimento.comum.Excecoes;
import br.com.tiagocarvalho.conhecimento.pergunta.CacheDeRespostas;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class ServicoDocumentos {

    private final RepositorioDocumentos documentos;
    private final ApplicationEventPublisher eventos;
    private final CacheDeRespostas cache;

    public ServicoDocumentos(RepositorioDocumentos documentos, ApplicationEventPublisher eventos, CacheDeRespostas cache) {
        this.documentos = documentos;
        this.eventos = eventos;
        this.cache = cache;
    }

    /**
     * Salva o arquivo e responde na hora. O processamento pesado (extrair texto, gerar embeddings)
     * acontece depois, no ProcessadorDeDocumentos, que recebe a mensagem pela fila.
     */
    @Transactional
    public DocumentoResposta enviar(String titulo, MultipartFile arquivo, UUID enviadoPor) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new Excecoes.RegraDeNegocio("Envie um arquivo que não esteja vazio.");
        }
        String nomeArquivo = arquivo.getOriginalFilename();
        TipoDocumento tipo = TipoDocumento.pelaExtensao(nomeArquivo);
        byte[] bytes = lerBytes(arquivo);

        String hash = sha256(bytes);
        if (documentos.existsByHashConteudo(hash)) {
            throw new Excecoes.Conflito("Este arquivo já foi enviado antes.");
        }

        String tituloFinal = titulo == null || titulo.isBlank() ? nomeArquivo : titulo.trim();
        if (tituloFinal.length() > 200) {
            throw new Excecoes.RegraDeNegocio("O título pode ter no máximo 200 caracteres.");
        }

        Documento documento = documentos.save(new Documento(tituloFinal, nomeArquivo, tipo, bytes, hash, enviadoPor));
        eventos.publishEvent(new Mensagens.DocumentoAguardandoProcessamento(documento.getId()));
        return DocumentoResposta.de(documento);
    }

    @Transactional(readOnly = true)
    public List<DocumentoResposta> listar() {
        return documentos.listarResumos();
    }

    @Transactional(readOnly = true)
    public DocumentoResposta buscar(UUID id) {
        return DocumentoResposta.de(encontrar(id));
    }

    @Transactional
    public DocumentoResposta reprocessar(UUID id) {
        Documento documento = encontrar(id);
        documento.voltarParaFila();
        eventos.publishEvent(new Mensagens.DocumentoAguardandoProcessamento(documento.getId()));
        return DocumentoResposta.de(documento);
    }

    @Transactional
    public void remover(UUID id) {
        documentos.delete(encontrar(id));   // os trechos saem junto (ON DELETE CASCADE)
        cache.limpar();                     // respostas antigas podem citar o documento removido
    }

    private Documento encontrar(UUID id) {
        return documentos.findById(id)
                .orElseThrow(() -> new Excecoes.NaoEncontrado("Documento " + id + " não encontrado."));
    }

    private static byte[] lerBytes(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException e) {
            throw new Excecoes.RegraDeNegocio("Não foi possível ler o arquivo enviado.");
        }
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
