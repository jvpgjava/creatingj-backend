package io.github.jvpgjava.creatingj.bootstrap.config.security;

import io.github.jvpgjava.creatingj.bootstrap.config.web.ProblemDetailsFactory;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.SessionAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class SecurityConfig {

    private final ProblemDetailsFactory problemDetailsFactory;
    private final ObjectMapper objectMapper;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final SessionAuthenticationFilter sessionAuthenticationFilter;
    private final CsrfCookieProtectionFilter csrfCookieProtectionFilter;
    private final CorsProperties corsProperties;
    private final boolean publicDocsEnabled;

    public SecurityConfig(
            ProblemDetailsFactory problemDetailsFactory,
            ObjectMapper objectMapper,
            AuthRateLimitFilter authRateLimitFilter,
            SessionAuthenticationFilter sessionAuthenticationFilter,
            CsrfCookieProtectionFilter csrfCookieProtectionFilter,
            CorsProperties corsProperties,
            @Value("${creatingj.docs.public-access:false}") boolean publicDocsEnabled
    ) {
        this.problemDetailsFactory = problemDetailsFactory;
        this.objectMapper = objectMapper;
        this.authRateLimitFilter = authRateLimitFilter;
        this.sessionAuthenticationFilter = sessionAuthenticationFilter;
        this.csrfCookieProtectionFilter = csrfCookieProtectionFilter;
        this.corsProperties = corsProperties;
        this.publicDocsEnabled = publicDocsEnabled;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(sessionAuthenticationFilter, AuthRateLimitFilter.class)
                .addFilterAfter(csrfCookieProtectionFilter, SessionAuthenticationFilter.class)
                .headers(headers -> headers
                        .contentTypeOptions(contentType -> { })
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, ex) ->
                                writeProblem(request, response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                                        "Autenticacao obrigatoria."))
                        .accessDeniedHandler((request, response, ex) ->
                                writeProblem(request, response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN,
                                        "Acesso negado."))
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/system/status").permitAll();
                    auth.requestMatchers("/api/v1/system/status").permitAll();
                    auth.requestMatchers(
                            HttpMethod.POST,
                            "/api/v1/auth/activate",
                            "/api/v1/auth/login",
                            "/api/v1/auth/web/login",
                            "/api/v1/auth/pkce/token",
                            "/api/v1/auth/token/refresh",
                            "/api/v1/auth/password-reset/request",
                            "/api/v1/auth/password-reset/confirm"
                    ).permitAll();
                    auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN");
                    if (publicDocsEnabled) {
                        auth.requestMatchers(HttpMethod.GET, "/api-docs", "/api-docs/**", "/api-docs.yaml/**")
                                .permitAll();
                    }
                    auth.anyRequest().authenticated();
                });

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Trace-Id"));
        config.setExposedHeaders(List.of("X-Trace-Id"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private void writeProblem(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            ErrorCode code,
            String detail
    ) throws IOException {
        ProblemDetail problem = problemDetailsFactory.create(status, code, detail, request);
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}
