package io.github.jvpgjava.creatingj.identity;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.domain.AuthorizationCode;
import io.github.jvpgjava.creatingj.identity.domain.PkceChallenge;
import io.github.jvpgjava.creatingj.identity.domain.PublicClientRegistry;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PkceAndSessionDomainTest {

    private static final Instant NOW = Instant.parse("2026-11-21T23:55:00Z");

    @Test
    void shouldValidatePkceS256ChallengeAndVerifyCodeVerifier() throws Exception {
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk-extra-entropy-12345";
        String challenge = computeS256(verifier);

        PkceChallenge pkce = PkceChallenge.s256(challenge, "S256");
        assertThat(pkce.method()).isEqualTo("S256");
        assertThat(pkce.matchesVerifier(verifier)).isTrue();
        assertThat(pkce.matchesVerifier(verifier + "x")).isFalse();
        assertThat(pkce.matchesVerifier("short")).isFalse();
        assertThat(pkce.matchesVerifier(null)).isFalse();
    }

    @Test
    void shouldRejectPlainOrMalformedPkceChallenge() {
        String validChallenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
        assertThatThrownBy(() -> PkceChallenge.s256(validChallenge, "plain"))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> PkceChallenge.s256("too-short", "S256"))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> PkceChallenge.s256(null, "S256"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldEnforceExactPublicClientAndRedirectAllowlist() {
        assertThatCode(() -> PublicClientRegistry.validate("creatingj-vscode", "http://127.0.0.1/callback"))
                .doesNotThrowAnyException();
        assertThatCode(() -> PublicClientRegistry.validate("creatingj-mobile", "creatingj://auth/callback"))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> PublicClientRegistry.validate("unknown-client", "http://127.0.0.1/callback"))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> PublicClientRegistry.validate("creatingj-vscode", "https://evil.example/steal"))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> PublicClientRegistry.validate(null, "http://127.0.0.1/callback"))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void shouldEnforceAuthorizationCodeSingleUseExpirationAndBinding() {
        String challenge = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";
        AuthorizationCode code = AuthorizationCode.create(
                UUID.randomUUID(),
                UserId.newId(),
                "creatingj-vscode",
                "http://127.0.0.1/callback",
                "a".repeat(64),
                PkceChallenge.s256(challenge, "S256"),
                NOW,
                NOW.plusSeconds(120)
        );

        assertThat(code.matchesClientAndRedirect("creatingj-vscode", "http://127.0.0.1/callback")).isTrue();
        assertThat(code.matchesClientAndRedirect("creatingj-cli", "http://127.0.0.1/callback")).isFalse();

        AuthorizationCode used = code.markUsed(NOW.plusSeconds(10));
        assertThat(used.usedAt()).isEqualTo(NOW.plusSeconds(10));

        assertThatThrownBy(() -> used.markUsed(NOW.plusSeconds(20)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONFLICT);

        assertThatThrownBy(() -> code.markUsed(NOW.plusSeconds(121)))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void shouldTrackRefreshExpirationAndRotationOnUserSession() {
        UUID familyId = UUID.randomUUID();
        UserSession session = UserSession.createWithRefresh(
                UUID.randomUUID(),
                UserId.newId(),
                "creatingj-vscode",
                "PKCE",
                familyId,
                "a".repeat(64),
                NOW,
                NOW.plusSeconds(900),
                "b".repeat(64),
                NOW.plusSeconds(86400)
        );

        assertThat(session.isRefreshableAt(NOW.plusSeconds(1000))).isTrue();
        assertThat(session.isRotated()).isFalse();

        UserSession rotated = session.markRotated(NOW.plusSeconds(1000));
        assertThat(rotated.isRotated()).isTrue();
        assertThat(rotated.isRefreshableAt(NOW.plusSeconds(1001))).isFalse();
        assertThat(rotated.isActiveAt(NOW.plusSeconds(1001))).isFalse();
    }

    private static String computeS256(String verifier) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(verifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
