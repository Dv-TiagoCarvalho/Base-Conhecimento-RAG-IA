package br.com.tiagocarvalho.conhecimento.documento;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Acesso à tabela "trecho" com SQL escrito à mão (JdbcTemplate).
 *
 * O JPA não conhece o tipo "vector" do pgvector, então os vetores vão para o banco como texto
 * no formato '[0.1,0.2,...]' e são convertidos com "::vector" dentro do próprio SQL.
 */
@Repository
public class RepositorioTrechos {

    private final JdbcTemplate jdbc;

    public RepositorioTrechos(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void inserir(UUID documentoId, List<String> conteudos, List<float[]> vetores) {
        if (conteudos.size() != vetores.size()) {
            throw new IllegalArgumentException("Quantidade de trechos e de vetores diferente.");
        }
        jdbc.batchUpdate(
                "INSERT INTO trecho (documento_id, indice, conteudo, embedding) VALUES (?, ?, ?, ?::vector)",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setObject(1, documentoId);
                        ps.setInt(2, i);
                        ps.setString(3, conteudos.get(i));
                        ps.setString(4, paraLiteralVetor(vetores.get(i)));
                    }

                    @Override
                    public int getBatchSize() {
                        return conteudos.size();
                    }
                });
    }

    public void removerDoDocumento(UUID documentoId) {
        jdbc.update("DELETE FROM trecho WHERE documento_id = ?", documentoId);
    }

    public int contarDoDocumento(UUID documentoId) {
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM trecho WHERE documento_id = ?", Integer.class, documentoId);
        return total == null ? 0 : total;
    }

    /**
     * Busca os trechos mais parecidos com o vetor da pergunta.
     * O operador <=> do pgvector calcula a distância de cosseno (0 = idênticos); similaridade = 1 - distância.
     */
    public List<TrechoEncontrado> buscarSemelhantes(float[] vetorPergunta, int quantidade, double similaridadeMinima) {
        String vetor = paraLiteralVetor(vetorPergunta);
        List<TrechoEncontrado> encontrados = jdbc.query("""
                        SELECT t.documento_id, d.titulo, t.indice, t.conteudo,
                               1 - (t.embedding <=> ?::vector) AS similaridade
                        FROM trecho t
                        JOIN documento d ON d.id = t.documento_id
                        WHERE d.status = 'PRONTO'
                        ORDER BY t.embedding <=> ?::vector
                        LIMIT ?
                        """,
                (rs, linha) -> new TrechoEncontrado(
                        rs.getObject("documento_id", UUID.class),
                        rs.getString("titulo"),
                        rs.getInt("indice"),
                        rs.getString("conteudo"),
                        rs.getDouble("similaridade")),
                vetor, vetor, quantidade);
        return encontrados.stream()
                .filter(t -> t.similaridade() >= similaridadeMinima)
                .toList();
    }

    static String paraLiteralVetor(float[] vetor) {
        StringBuilder sb = new StringBuilder(vetor.length * 10).append('[');
        for (int i = 0; i < vetor.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vetor[i]);
        }
        return sb.append(']').toString();
    }
}
