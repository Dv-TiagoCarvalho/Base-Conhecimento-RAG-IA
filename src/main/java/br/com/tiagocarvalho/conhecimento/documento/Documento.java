package br.com.tiagocarvalho.conhecimento.documento;

import br.com.tiagocarvalho.conhecimento.comum.Excecoes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documento")
public class Documento {

    private static final int TAMANHO_MAXIMO_ERRO = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDocumento tipo;

    @Column(nullable = false)
    private byte[] conteudo;

    @Column(name = "hash_conteudo", nullable = false, length = 64)
    private String hashConteudo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDocumento status;

    @Column(name = "mensagem_erro", length = TAMANHO_MAXIMO_ERRO)
    private String mensagemErro;

    @Column(name = "quantidade_trechos")
    private Integer quantidadeTrechos;

    @Column(name = "enviado_por", nullable = false)
    private UUID enviadoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "processado_em")
    private Instant processadoEm;

    @Version
    private Long versao;

    protected Documento() {
        // exigido pelo JPA
    }

    public Documento(String titulo, String nomeArquivo, TipoDocumento tipo, byte[] conteudo,
                     String hashConteudo, UUID enviadoPor) {
        this.titulo = titulo;
        this.nomeArquivo = nomeArquivo;
        this.tipo = tipo;
        this.conteudo = conteudo;
        this.hashConteudo = hashConteudo;
        this.enviadoPor = enviadoPor;
        this.status = StatusDocumento.PENDENTE;
        this.criadoEm = Instant.now();
    }

    // ---- Transições de estado: a regra fica na entidade, não espalhada pelos serviços ----

    public void marcarProcessando() {
        this.status = StatusDocumento.PROCESSANDO;
        this.mensagemErro = null;
    }

    public void marcarPronto(int quantidadeTrechos) {
        this.status = StatusDocumento.PRONTO;
        this.quantidadeTrechos = quantidadeTrechos;
        this.processadoEm = Instant.now();
        this.mensagemErro = null;
    }

    public void marcarFalha(String motivo) {
        this.status = StatusDocumento.FALHOU;
        this.processadoEm = Instant.now();
        String texto = motivo == null || motivo.isBlank() ? "Erro desconhecido" : motivo;
        this.mensagemErro = texto.length() > TAMANHO_MAXIMO_ERRO ? texto.substring(0, TAMANHO_MAXIMO_ERRO) : texto;
    }

    public void voltarParaFila() {
        if (status == StatusDocumento.PRONTO) {
            throw new Excecoes.RegraDeNegocio("O documento já está pronto; não há o que reprocessar.");
        }
        this.status = StatusDocumento.PENDENTE;
        this.mensagemErro = null;
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public TipoDocumento getTipo() {
        return tipo;
    }

    public byte[] getConteudo() {
        return conteudo;
    }

    public String getHashConteudo() {
        return hashConteudo;
    }

    public StatusDocumento getStatus() {
        return status;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public Integer getQuantidadeTrechos() {
        return quantidadeTrechos;
    }

    public UUID getEnviadoPor() {
        return enviadoPor;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getProcessadoEm() {
        return processadoEm;
    }
}
