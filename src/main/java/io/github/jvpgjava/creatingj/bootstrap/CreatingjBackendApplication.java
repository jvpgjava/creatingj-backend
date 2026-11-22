package io.github.jvpgjava.creatingj.bootstrap;

import io.github.jvpgjava.creatingj.bootstrap.config.provider.AiProviderProperties;
import io.github.jvpgjava.creatingj.bootstrap.config.provider.EmbeddingProperties;
import io.github.jvpgjava.creatingj.bootstrap.config.security.CorsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
        scanBasePackages = "io.github.jvpgjava.creatingj",
        exclude = UserDetailsServiceAutoConfiguration.class
)
@EnableConfigurationProperties({AiProviderProperties.class, EmbeddingProperties.class, CorsProperties.class})
public class CreatingjBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CreatingjBackendApplication.class, args);
    }
}
