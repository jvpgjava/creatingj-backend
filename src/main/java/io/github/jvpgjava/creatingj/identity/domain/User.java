package io.github.jvpgjava.creatingj.identity.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.time.Instant;
import java.util.Set;

public record User(
        UserId id,
        Email email,
        String displayName,
        String passwordHash,
        AccountStatus status,
        Set<Role> roles,
        UserId createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant suspendedAt
) {

    public User {
        if (id == null || email == null || status == null || createdAt == null || updatedAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de usuario ausentes.");
        }
        if (displayName == null || displayName.isBlank() || displayName.trim().length() > 120) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Nome de exibicao invalido.");
        }
        if (roles == null || roles.isEmpty()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Usuario deve possuir ao menos um papel.");
        }
        displayName = displayName.trim();
        roles = Set.copyOf(roles);
    }

    public static User pendingActivation(
            UserId id,
            Email email,
            String displayName,
            Set<Role> roles,
            UserId createdBy,
            Instant now
    ) {
        return new User(id, email, displayName, null, AccountStatus.PENDING_ACTIVATION, roles, createdBy, now, now,
                null);
    }

    public static User active(
            UserId id,
            Email email,
            String displayName,
            String passwordHash,
            Set<Role> roles,
            UserId createdBy,
            Instant now
    ) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Hash de senha obrigatorio para conta ativa.");
        }
        return new User(id, email, displayName, passwordHash, AccountStatus.ACTIVE, roles, createdBy, now, now, null);
    }

    public User activate(String newPasswordHash, Instant now) {
        if (status != AccountStatus.PENDING_ACTIVATION) {
            throw new DomainException(ErrorCode.INVALID_STATE, "Conta nao esta pendente de ativacao.");
        }
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Hash de senha obrigatorio.");
        }
        return new User(id, email, displayName, newPasswordHash, AccountStatus.ACTIVE, roles, createdBy, createdAt,
                now, null);
    }

    public User changePassword(String newPasswordHash, Instant now) {
        if (status != AccountStatus.ACTIVE) {
            throw new DomainException(ErrorCode.INVALID_STATE, "Somente contas ativas podem alterar senha.");
        }
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Hash de senha obrigatorio.");
        }
        return new User(id, email, displayName, newPasswordHash, status, roles, createdBy, createdAt, now,
                suspendedAt);
    }

    public User suspend(Instant now) {
        if (status == AccountStatus.SUSPENDED) {
            throw new DomainException(ErrorCode.INVALID_STATE, "Conta ja esta suspensa.");
        }
        return new User(id, email, displayName, passwordHash, AccountStatus.SUSPENDED, roles, createdBy, createdAt,
                now, now);
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return roles.contains(Role.ADMIN);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}
