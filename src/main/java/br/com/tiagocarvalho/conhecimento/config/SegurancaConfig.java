package br.com.tiagocarvalho.conhecimento.config;

import br.com.tiagocarvalho.conhecimento.auth.FiltroJwt;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain cadeiaDeSeguranca(HttpSecurity http, FiltroJwt filtroJwt) throws Exception {
        http
                // API sem sessão e sem cookie: o token vai no cabeçalho, então CSRF não se aplica
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regras -> regras
                        .requestMatchers("/", "/index.html", "/app.js", "/estilo.css", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/api/auth/registrar", "/api/auth/entrar").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/documentos", "/api/documentos/*/reprocessar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/documentos/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Sem token válido: 401 (e não um redirecionamento para tela de login)
                .exceptionHandling(erros -> erros.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * O FiltroJwt é um @Component, então o Spring Boot o registraria também como filtro comum do servidor.
     * Desligamos esse registro para que ele rode só dentro da cadeia do Spring Security.
     */
    @Bean
    public FilterRegistrationBean<FiltroJwt> naoRegistrarFiltroJwtNoServidor(FiltroJwt filtroJwt) {
        FilterRegistrationBean<FiltroJwt> registro = new FilterRegistrationBean<>(filtroJwt);
        registro.setEnabled(false);
        return registro;
    }
}
