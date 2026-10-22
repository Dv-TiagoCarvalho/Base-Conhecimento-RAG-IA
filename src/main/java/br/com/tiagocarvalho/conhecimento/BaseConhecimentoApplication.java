package br.com.tiagocarvalho.conhecimento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BaseConhecimentoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaseConhecimentoApplication.class, args);
    }
}
