package io.github.jvpgjava.creatingj.bootstrap.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "creatingj.security.cors")
public record CorsProperties(
        @DefaultValue("") List<String> allowedOrigins
) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : allowedOrigins.stream()
                        .filter(origin -> origin != null && !origin.isBlank() && !"*".equals(origin.trim()))
                        .map(String::trim)
                        .toList();
    }
}
