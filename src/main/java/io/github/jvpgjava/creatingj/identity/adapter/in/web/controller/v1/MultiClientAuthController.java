package io.github.jvpgjava.creatingj.identity.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.apidocs.v1.MultiClientAuthApiDocs;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.CurrentUserResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.LoginRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PkceAuthorizeRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PkceAuthorizeResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PkceTokenRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.RefreshTokenRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.SessionSummaryResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.TokenPairResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.WebLoginResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.mapper.v1.IdentityHttpMapper;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthCookieWriter;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthenticateWithPasswordUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthorizePkceUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.ExchangePkceCodeUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.GetCurrentUserUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListOwnSessionsUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.LogoutSessionUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.LogoutSessionUseCase.LogoutSessionCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.RefreshSessionUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.RevokeOwnSessionUseCase;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public final class MultiClientAuthController implements MultiClientAuthApiDocs {

    private final AuthenticateWithPasswordUseCase authenticateWithPassword;
    private final GetCurrentUserUseCase getCurrentUser;
    private final LogoutSessionUseCase logoutSession;
    private final ListOwnSessionsUseCase listOwnSessions;
    private final RevokeOwnSessionUseCase revokeOwnSession;
    private final AuthorizePkceUseCase authorizePkce;
    private final ExchangePkceCodeUseCase exchangePkceCode;
    private final RefreshSessionUseCase refreshSession;
    private final IdentityHttpMapper mapper;
    private final AuthCookieWriter cookieWriter;
    private final Duration sessionTtl;

    public MultiClientAuthController(
            AuthenticateWithPasswordUseCase authenticateWithPassword,
            GetCurrentUserUseCase getCurrentUser,
            LogoutSessionUseCase logoutSession,
            ListOwnSessionsUseCase listOwnSessions,
            RevokeOwnSessionUseCase revokeOwnSession,
            AuthorizePkceUseCase authorizePkce,
            ExchangePkceCodeUseCase exchangePkceCode,
            RefreshSessionUseCase refreshSession,
            IdentityHttpMapper mapper,
            AuthCookieWriter cookieWriter,
            @Value("${creatingj.security.session.ttl:8h}") Duration sessionTtl
    ) {
        this.authenticateWithPassword = authenticateWithPassword;
        this.getCurrentUser = getCurrentUser;
        this.logoutSession = logoutSession;
        this.listOwnSessions = listOwnSessions;
        this.revokeOwnSession = revokeOwnSession;
        this.authorizePkce = authorizePkce;
        this.exchangePkceCode = exchangePkceCode;
        this.refreshSession = refreshSession;
        this.mapper = mapper;
        this.cookieWriter = cookieWriter;
        this.sessionTtl = sessionTtl;
    }

    @Override
    @PostMapping("/web/login")
    public ResponseEntity<WebLoginResponse> webLogin(@Valid @RequestBody LoginRequest request) {
        var session = authenticateWithPassword.execute(mapper.toWebLoginCommand(request));
        return ResponseEntity.ok()
                .headers(cookieWriter.buildSessionHeaders(session.accessToken(), sessionTtl))
                .body(mapper.toWebLoginResponse(session));
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal AuthenticatedUser actor) {
        var profile = getCurrentUser.execute(actor.id(), actor.sessionId());
        return ResponseEntity.ok(mapper.toCurrentUserResponse(profile));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser actor) {
        logoutSession.execute(new LogoutSessionCommand(actor.id(), actor.sessionId()));
        return ResponseEntity.noContent()
                .headers(cookieWriter.buildClearCookieHeaders())
                .build();
    }

    @Override
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionSummaryResponse>> listSessions(@AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(mapper.toSessionSummaryList(listOwnSessions.execute(actor.id())));
    }

    @Override
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        revokeOwnSession.execute(mapper.toRevokeSessionCommand(actor.id(), sessionId));
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/pkce/authorize")
    public ResponseEntity<PkceAuthorizeResponse> authorizePkce(
            @Valid @RequestBody PkceAuthorizeRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = authorizePkce.execute(mapper.toPkceAuthorizeCommand(actor.id(), request));
        return ResponseEntity.ok(mapper.toPkceAuthorizeResponse(result));
    }

    @Override
    @PostMapping("/pkce/token")
    public ResponseEntity<TokenPairResponse> exchangePkceToken(@Valid @RequestBody PkceTokenRequest request) {
        var result = exchangePkceCode.execute(mapper.toPkceTokenCommand(request));
        return ResponseEntity.ok(mapper.toTokenPairResponse(result));
    }

    @Override
    @PostMapping("/token/refresh")
    public ResponseEntity<TokenPairResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        var result = refreshSession.execute(mapper.toRefreshCommand(request));
        return ResponseEntity.ok(mapper.toTokenPairResponse(result));
    }
}
