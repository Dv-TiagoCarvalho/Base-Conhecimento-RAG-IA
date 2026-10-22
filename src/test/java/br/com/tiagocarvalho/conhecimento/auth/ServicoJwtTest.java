package br.com.tiagocarvalho.conhecimento.auth;

import br.com.tiagocarvalho.conhecimento.config.Propriedades;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicoJwtTest {

    private static final String SEGREDO = "um-segredo-de-teste-com-mais-de-32-caracteres";

    private final ServicoJwt servico = new ServicoJwt(new Propriedades.Jwt(SEGREDO, 60));

    @Test
    void tokenGeradoEhValidadoComOsMesmosDados() {
        Usuario usuario = usuario();
        var autenticado = servico.validar(servico.gerar(usuario)).orElseThrow();

        assertThat(autenticado.id()).isEqualTo(usuario.getId());
        assertThat(autenticado.email()).isEqualTo("ana@teste.com");
        assertThat(autenticado.papel()).isEqualTo(Papel.ADMIN);
    }

    @Test
    void tokenAdulteradoEhRecusado() {
        String token = servico.gerar(usuario());
        String adulterado = token.substring(0, token.length() - 3) + "abc";
        assertThat(servico.validar(adulterado)).isEmpty();
    }

    @Test
    void tokenAssinadoComOutroSegredoEhRecusado() {
        var outro = new ServicoJwt(new Propriedades.Jwt("outro-segredo-diferente-com-mais-de-32-caracteres", 60));
        assertThat(servico.validar(outro.gerar(usuario()))).isEmpty();
    }

    @Test
    void tokenExpiradoEhRecusado() {
        var expirado = new ServicoJwt(new Propriedades.Jwt(SEGREDO, -1));
        assertThat(servico.validar(expirado.gerar(usuario()))).isEmpty();
    }

    @Test
    void segredoCurtoImpedeASubida() {
        assertThatThrownBy(() -> new ServicoJwt(new Propriedades.Jwt("curto", 60)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static Usuario usuario() {
        Usuario usuario = new Usuario("Ana", "ana@teste.com", "hash", Papel.ADMIN);
        ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());   // o id normalmente vem do banco
        return usuario;
    }
}
