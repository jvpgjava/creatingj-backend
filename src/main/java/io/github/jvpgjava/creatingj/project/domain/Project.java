package io.github.jvpgjava.creatingj.project.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

public record Project(
        ProjectId id,
        UserId ownerUserId,
        String name,
        String slug,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_SLUG_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final Pattern SLUG_PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    public Project {
        if (id == null || ownerUserId == null || createdAt == null || updatedAt == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Campos obrigatorios de projeto ausentes.");
        }
        if (name == null || name.isBlank() || name.trim().length() > MAX_NAME_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Nome do projeto invalido.");
        }
        if (slug == null || slug.isBlank() || slug.trim().length() > MAX_SLUG_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Slug do projeto invalido.");
        }
        String normalizedSlug = slug.trim().toLowerCase(Locale.ROOT);
        if (!SLUG_PATTERN.matcher(normalizedSlug).matches()) {
            throw new DomainException(
                    ErrorCode.VALIDATION_ERROR,
                    "Slug do projeto deve conter apenas letras, numeros e hifen."
            );
        }
        String normalizedDescription = description == null ? "" : description.trim();
        if (normalizedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Descricao do projeto excede o limite permitido.");
        }
        name = name.trim();
        slug = normalizedSlug;
        description = normalizedDescription;
    }

    public static Project create(
            ProjectId id,
            UserId ownerUserId,
            String name,
            String rawSlug,
            String description,
            Instant now
    ) {
        String effectiveSlug = (rawSlug == null || rawSlug.isBlank()) ? deriveSlug(name) : rawSlug;
        return new Project(id, ownerUserId, name, effectiveSlug, description, now, now);
    }

    public boolean isOwnedBy(UserId candidateUserId) {
        return ownerUserId.equals(candidateUserId);
    }

    public static String deriveSlug(String name) {
        if (name == null || name.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Nome do projeto invalido.");
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (normalized.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Nao foi possivel gerar slug valido para o projeto.");
        }
        if (normalized.length() > MAX_SLUG_LENGTH) {
            normalized = normalized.substring(0, MAX_SLUG_LENGTH).replaceAll("-+$", "");
        }
        return normalized;
    }
}
