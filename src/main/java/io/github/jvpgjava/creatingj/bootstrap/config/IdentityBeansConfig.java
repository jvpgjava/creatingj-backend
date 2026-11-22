package io.github.jvpgjava.creatingj.bootstrap.config;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.adapter.out.security.InMemoryLoginAttemptGuardAdapter;
import io.github.jvpgjava.creatingj.identity.application.port.in.ActivateAccountUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthenticateWithPasswordUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthorizePkceUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.ConfirmPasswordResetUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.CreateManagedUserUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.ExchangePkceCodeUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.GetCurrentUserUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListOwnSessionsUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.LogoutSessionUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.RefreshSessionUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.RequestPasswordResetUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.ResolveAuthenticatedSessionUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.RevokeOwnSessionUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.SuspendUserUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.AuthorizationCodeStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.InvitationStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.LoginAttemptGuardPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.OpaqueTokenGeneratorPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.PasswordHasherPort;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.application.usecase.ActivateAccountInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.AuthenticateWithPasswordInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.AuthorizePkceInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.ConfirmPasswordResetInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.CreateManagedUserInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.ExchangePkceCodeInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.GetCurrentUserInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.ListOwnSessionsInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.ListUsersInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.LogoutSessionInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.RefreshSessionInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.RequestPasswordResetInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.ResolveAuthenticatedSessionInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.RevokeOwnSessionInteractor;
import io.github.jvpgjava.creatingj.identity.application.usecase.SuspendUserInteractor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class IdentityBeansConfig {

    @Bean
    public LoginAttemptGuardPort loginAttemptGuardPort(
            ClockPort clockPort,
            @Value("${creatingj.security.login.max-attempts:5}") int maxAttempts,
            @Value("${creatingj.security.login.window:15m}") Duration window
    ) {
        return new InMemoryLoginAttemptGuardAdapter(clockPort, maxAttempts, window);
    }

    @Bean
    public CreateManagedUserUseCase createManagedUserUseCase(
            UserStorePort userStore,
            InvitationStorePort invitationStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clockPort,
            @Value("${creatingj.security.invitation.activation-ttl:24h}") Duration activationTtl
    ) {
        return new CreateManagedUserInteractor(
                userStore, invitationStore, tokenGenerator, auditPort, clockPort, activationTtl);
    }

    @Bean
    public ListUsersUseCase listUsersUseCase(UserStorePort userStore) {
        return new ListUsersInteractor(userStore);
    }

    @Bean
    public SuspendUserUseCase suspendUserUseCase(
            UserStorePort userStore,
            SessionStorePort sessionStore,
            InvitationStorePort invitationStore,
            AuditPort auditPort,
            ClockPort clockPort
    ) {
        return new SuspendUserInteractor(userStore, sessionStore, invitationStore, auditPort, clockPort);
    }

    @Bean
    public ActivateAccountUseCase activateAccountUseCase(
            UserStorePort userStore,
            InvitationStorePort invitationStore,
            PasswordHasherPort passwordHasher,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clockPort
    ) {
        return new ActivateAccountInteractor(
                userStore, invitationStore, passwordHasher, tokenGenerator, auditPort, clockPort);
    }

    @Bean
    public AuthenticateWithPasswordUseCase authenticateWithPasswordUseCase(
            UserStorePort userStore,
            SessionStorePort sessionStore,
            PasswordHasherPort passwordHasher,
            OpaqueTokenGeneratorPort tokenGenerator,
            LoginAttemptGuardPort loginAttemptGuard,
            AuditPort auditPort,
            ClockPort clockPort,
            @Value("${creatingj.security.session.ttl:8h}") Duration sessionTtl
    ) {
        return new AuthenticateWithPasswordInteractor(
                userStore, sessionStore, passwordHasher, tokenGenerator, loginAttemptGuard, auditPort, clockPort,
                sessionTtl);
    }

    @Bean
    public RequestPasswordResetUseCase requestPasswordResetUseCase(
            UserStorePort userStore,
            InvitationStorePort invitationStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clockPort,
            @Value("${creatingj.security.invitation.reset-ttl:30m}") Duration resetTtl
    ) {
        return new RequestPasswordResetInteractor(
                userStore, invitationStore, tokenGenerator, auditPort, clockPort, resetTtl);
    }

    @Bean
    public ConfirmPasswordResetUseCase confirmPasswordResetUseCase(
            UserStorePort userStore,
            InvitationStorePort invitationStore,
            SessionStorePort sessionStore,
            PasswordHasherPort passwordHasher,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clockPort
    ) {
        return new ConfirmPasswordResetInteractor(
                userStore, invitationStore, sessionStore, passwordHasher, tokenGenerator, auditPort, clockPort);
    }

    @Bean
    public ResolveAuthenticatedSessionUseCase resolveAuthenticatedSessionUseCase(
            SessionStorePort sessionStore,
            UserStorePort userStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            ClockPort clockPort
    ) {
        return new ResolveAuthenticatedSessionInteractor(sessionStore, userStore, tokenGenerator, clockPort);
    }

    @Bean
    public AuthorizePkceUseCase authorizePkceUseCase(
            UserStorePort userStore,
            AuthorizationCodeStorePort authorizationCodeStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clockPort,
            @Value("${creatingj.security.pkce.code-ttl:2m}") Duration codeTtl
    ) {
        return new AuthorizePkceInteractor(
                userStore, authorizationCodeStore, tokenGenerator, auditPort, clockPort, codeTtl);
    }

    @Bean
    public ExchangePkceCodeUseCase exchangePkceCodeUseCase(
            UserStorePort userStore,
            AuthorizationCodeStorePort authorizationCodeStore,
            SessionStorePort sessionStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clockPort,
            @Value("${creatingj.security.pkce.access-ttl:15m}") Duration accessTtl,
            @Value("${creatingj.security.pkce.refresh-ttl:14d}") Duration refreshTtl
    ) {
        return new ExchangePkceCodeInteractor(
                userStore, authorizationCodeStore, sessionStore, tokenGenerator, auditPort, clockPort,
                accessTtl, refreshTtl);
    }

    @Bean
    public RefreshSessionUseCase refreshSessionUseCase(
            UserStorePort userStore,
            SessionStorePort sessionStore,
            OpaqueTokenGeneratorPort tokenGenerator,
            AuditPort auditPort,
            ClockPort clockPort,
            @Value("${creatingj.security.pkce.access-ttl:15m}") Duration accessTtl,
            @Value("${creatingj.security.pkce.refresh-ttl:14d}") Duration refreshTtl
    ) {
        return new RefreshSessionInteractor(
                userStore, sessionStore, tokenGenerator, auditPort, clockPort, accessTtl, refreshTtl);
    }

    @Bean
    public GetCurrentUserUseCase getCurrentUserUseCase(
            UserStorePort userStore,
            SessionStorePort sessionStore,
            ClockPort clockPort
    ) {
        return new GetCurrentUserInteractor(userStore, sessionStore, clockPort);
    }

    @Bean
    public LogoutSessionUseCase logoutSessionUseCase(
            SessionStorePort sessionStore,
            AuditPort auditPort,
            ClockPort clockPort
    ) {
        return new LogoutSessionInteractor(sessionStore, auditPort, clockPort);
    }

    @Bean
    public ListOwnSessionsUseCase listOwnSessionsUseCase(SessionStorePort sessionStore, ClockPort clockPort) {
        return new ListOwnSessionsInteractor(sessionStore, clockPort);
    }

    @Bean
    public RevokeOwnSessionUseCase revokeOwnSessionUseCase(
            SessionStorePort sessionStore,
            AuditPort auditPort,
            ClockPort clockPort
    ) {
        return new RevokeOwnSessionInteractor(sessionStore, auditPort, clockPort);
    }
}
