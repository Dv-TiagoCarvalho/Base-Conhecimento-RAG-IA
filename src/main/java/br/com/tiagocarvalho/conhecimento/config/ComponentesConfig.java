package br.com.tiagocarvalho.conhecimento.config;

import br.com.tiagocarvalho.conhecimento.documento.DivisorDeTexto;
import br.com.tiagocarvalho.conhecimento.pergunta.CacheDeRespostas;
import br.com.tiagocarvalho.conhecimento.pergunta.LimitadorDeRequisicoes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Duration;

/**
 * Cria os componentes que são classes Java simples (sem anotações do Spring).
 * Assim eles podem ser testados com "new", sem subir o Spring.
 */
@Configuration
public class ComponentesConfig {

    @Bean
    public Clock relogio() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder codificadorDeSenha() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DivisorDeTexto divisorDeTexto(Propriedades.Rag rag) {
        return new DivisorDeTexto(rag.tamanhoTrecho(), rag.sobreposicao());
    }

    @Bean
    public LimitadorDeRequisicoes limitadorDeRequisicoes(Propriedades.Limite limite, Clock relogio) {
        return new LimitadorDeRequisicoes(limite.perguntasPorMinuto(), Duration.ofMinutes(1), relogio);
    }

    @Bean
    public CacheDeRespostas cacheDeRespostas(Propriedades.Cache cache, Clock relogio) {
        return new CacheDeRespostas(Duration.ofMinutes(cache.ttlMinutos()), cache.maximoEntradas(), relogio);
    }
}
