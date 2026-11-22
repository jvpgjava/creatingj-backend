package io.github.jvpgjava.creatingj.identity.adapter.in.web.mapper.v1;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.ActivateAccountRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.ConfirmPasswordResetRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.CreateManagedUserRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.CreatedManagedUserResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.CurrentUserResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.LoginRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.LoginResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PasswordResetRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PkceAuthorizeRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PkceAuthorizeResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PkceTokenRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.RefreshTokenRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.SessionSummaryResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.TokenPairResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.UserSummaryResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.UsersPageResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.WebLoginResponse;
import io.github.jvpgjava.creatingj.identity.application.port.in.ActivateAccountUseCase.ActivateAccountCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthenticateWithPasswordUseCase.AuthenticatedSession;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthenticateWithPasswordUseCase.LoginCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthorizePkceUseCase.AuthorizePkceCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthorizePkceUseCase.PkceAuthorizationResult;
import io.github.jvpgjava.creatingj.identity.application.port.in.ConfirmPasswordResetUseCase.ConfirmPasswordResetCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.CreateManagedUserUseCase.CreateManagedUserCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.CreateManagedUserUseCase.CreatedManagedUser;
import io.github.jvpgjava.creatingj.identity.application.port.in.ExchangePkceCodeUseCase.ExchangePkceCodeCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.ExchangePkceCodeUseCase.TokenPairResult;
import io.github.jvpgjava.creatingj.identity.application.port.in.GetCurrentUserUseCase.CurrentUserProfile;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase.ListUsersQuery;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase.UserSortField;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase.UsersPage;
import io.github.jvpgjava.creatingj.identity.application.port.in.RefreshSessionUseCase.RefreshSessionCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.RequestPasswordResetUseCase.RequestPasswordResetCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.RevokeOwnSessionUseCase.RevokeOwnSessionCommand;
import io.github.jvpgjava.creatingj.identity.application.port.in.SuspendUserUseCase.SuspendUserCommand;
import io.github.jvpgjava.creatingj.identity.domain.AccountStatus;
import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public final class IdentityHttpMapper {

    public CreateManagedUserCommand toCommand(CreateManagedUserRequest request, UserId actorId) {
        return new CreateManagedUserCommand(actorId, request.email(), request.displayName(), request.roles());
    }

    public ListUsersQuery toListQuery(
            UserId actorId,
            int page,
            int size,
            String statusRaw,
            String sortRaw,
            String directionRaw
    ) {
        AccountStatus status = parseStatus(statusRaw);
        UserSortField sortBy = parseSortField(sortRaw);
        boolean ascending = parseDirection(directionRaw);
        return new ListUsersQuery(actorId, page, size, status, sortBy, ascending);
    }

    public SuspendUserCommand toSuspendCommand(UserId actorId, String targetUserId) {
        return new SuspendUserCommand(actorId, UserId.fromString(targetUserId));
    }

    public ActivateAccountCommand toActivateCommand(ActivateAccountRequest request) {
        return new ActivateAccountCommand(request.token(), request.password());
    }

    public LoginCommand toLoginCommand(LoginRequest request) {
        return new LoginCommand(request.email(), request.password());
    }

    public LoginCommand toWebLoginCommand(LoginRequest request) {
        return new LoginCommand(request.email(), request.password(), "creatingj-web", "WEB_COOKIE");
    }

    public AuthorizePkceCommand toPkceAuthorizeCommand(UserId actorId, PkceAuthorizeRequest request) {
        return new AuthorizePkceCommand(
                actorId,
                request.clientId(),
                request.redirectUri(),
                request.codeChallenge(),
                request.codeChallengeMethod(),
                request.state()
        );
    }

    public ExchangePkceCodeCommand toPkceTokenCommand(PkceTokenRequest request) {
        return new ExchangePkceCodeCommand(
                request.code(),
                request.clientId(),
                request.redirectUri(),
                request.codeVerifier()
        );
    }

    public RefreshSessionCommand toRefreshCommand(RefreshTokenRequest request) {
        return new RefreshSessionCommand(request.refreshToken());
    }

    public RevokeOwnSessionCommand toRevokeSessionCommand(UserId actorId, String sessionIdRaw) {
        try {
            return new RevokeOwnSessionCommand(actorId, UUID.fromString(sessionIdRaw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "sessionId invalido.", ex);
        }
    }

    public WebLoginResponse toWebLoginResponse(AuthenticatedSession session) {
        Set<String> roleNames = session.roles().stream()
                .map(Role::name)
                .collect(Collectors.toUnmodifiableSet());
        return new WebLoginResponse(
                session.userId().value().toString(),
                session.email(),
                roleNames,
                session.expiresAt()
        );
    }

    public CurrentUserResponse toCurrentUserResponse(CurrentUserProfile profile) {
        User user = profile.user();
        UserSession session = profile.session();
        Set<String> roleNames = user.roles().stream()
                .map(Role::name)
                .collect(Collectors.toUnmodifiableSet());
        return new CurrentUserResponse(
                user.id().value().toString(),
                user.email().value(),
                user.displayName(),
                user.status().name(),
                roleNames,
                session.id().toString(),
                session.clientId(),
                session.expiresAt()
        );
    }

    public List<SessionSummaryResponse> toSessionSummaryList(List<UserSession> sessions) {
        return sessions.stream()
                .map(s -> new SessionSummaryResponse(
                        s.id().toString(),
                        s.clientId(),
                        s.authMethod(),
                        s.createdAt(),
                        s.expiresAt()
                ))
                .toList();
    }

    public PkceAuthorizeResponse toPkceAuthorizeResponse(PkceAuthorizationResult result) {
        return new PkceAuthorizeResponse(
                result.authorizationCode(),
                result.redirectUri(),
                result.state(),
                result.expiresAt()
        );
    }

    public TokenPairResponse toTokenPairResponse(TokenPairResult result) {
        Set<String> roleNames = result.roles().stream()
                .map(Role::name)
                .collect(Collectors.toUnmodifiableSet());
        return new TokenPairResponse(
                result.accessToken(),
                "Bearer",
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresAt(),
                result.userId().value().toString(),
                result.email(),
                roleNames
        );
    }

    public RequestPasswordResetCommand toResetRequestCommand(PasswordResetRequest request) {
        return new RequestPasswordResetCommand(request.email());
    }

    public ConfirmPasswordResetCommand toConfirmResetCommand(ConfirmPasswordResetRequest request) {
        return new ConfirmPasswordResetCommand(request.token(), request.newPassword());
    }

    public CreatedManagedUserResponse toCreatedResponse(CreatedManagedUser created) {
        return new CreatedManagedUserResponse(
                toSummary(created.user()),
                created.activationToken(),
                created.activationExpiresAt()
        );
    }

    public UsersPageResponse toPageResponse(UsersPage page) {
        return new UsersPageResponse(
                page.items().stream().map(this::toSummary).toList(),
                page.page(),
                page.size(),
                page.totalElements()
        );
    }

    public UserSummaryResponse toSummary(User user) {
        Set<String> roleNames = user.roles().stream()
                .map(Role::name)
                .collect(Collectors.toUnmodifiableSet());
        return new UserSummaryResponse(
                user.id().value().toString(),
                user.email().value(),
                user.displayName(),
                user.status().name(),
                roleNames,
                user.createdAt(),
                user.suspendedAt()
        );
    }

    public LoginResponse toLoginResponse(AuthenticatedSession session) {
        Set<String> roleNames = session.roles().stream()
                .map(Role::name)
                .collect(Collectors.toUnmodifiableSet());
        return new LoginResponse(
                session.accessToken(),
                "Bearer",
                session.expiresAt(),
                session.userId().value().toString(),
                session.email(),
                roleNames
        );
    }

    private static AccountStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return AccountStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Filtro status invalido.", ex);
        }
    }

    private static UserSortField parseSortField(String raw) {
        if (raw == null || raw.isBlank()) {
            return UserSortField.CREATED_AT;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "created_at", "createdat" -> UserSortField.CREATED_AT;
            case "email" -> UserSortField.EMAIL;
            case "display_name", "displayname" -> UserSortField.DISPLAY_NAME;
            case "status" -> UserSortField.STATUS;
            default -> throw new DomainException(ErrorCode.VALIDATION_ERROR, "Parametro sort invalido.");
        };
    }

    private static boolean parseDirection(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "asc" -> true;
            case "desc" -> false;
            default -> throw new DomainException(ErrorCode.VALIDATION_ERROR, "Parametro direction invalido.");
        };
    }
}
