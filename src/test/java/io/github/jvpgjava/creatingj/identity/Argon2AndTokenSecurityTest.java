package io.github.jvpgjava.creatingj.identity;

import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.identity.adapter.out.security.Argon2PasswordHasherAdapter;
import io.github.jvpgjava.creatingj.identity.adapter.out.security.InMemoryLoginAttemptGuardAdapter;
import io.github.jvpgjava.creatingj.identity.adapter.out.security.SecureOpaqueTokenAdapter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class Argon2AndTokenSecurityTest {

    @Test
    void shouldHashAndVerifyPasswordsUsingArgon2idWithUniqueSalts() {
        var hasher = new Argon2PasswordHasherAdapter();

        String hash1 = hasher.hash("Str0ng!Pass2026");
        String hash2 = hasher.hash("Str0ng!Pass2026");

        assertThat(hash1).startsWith("$argon2id$");
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(hash1).doesNotContain("Str0ng!Pass2026");
        assertThat(hasher.matches("Str0ng!Pass2026", hash1)).isTrue();
        assertThat(hasher.matches("Wrong!Pass2026", hash1)).isFalse();
    }

    @Test
    void shouldGenerateHighEntropyOpaqueTokensAndSha256Hash() {
        var generator = new SecureOpaqueTokenAdapter();

        String token1 = generator.generateToken();
        String token2 = generator.generateToken();
        String hash1 = generator.hash(token1);

        assertThat(token1).hasSizeGreaterThanOrEqualTo(40);
        assertThat(token1).isNotEqualTo(token2);
        assertThat(hash1).hasSize(64).matches("^[0-9a-f]{64}$");
        assertThat(hash1).isEqualTo(generator.hash(token1));
        assertThat(hash1).isNotEqualTo(token1);
    }

    @Test
    void shouldBlockAfterThresholdAndUnblockAfterWindowExpires() {
        var now = new AtomicReference<>(Instant.parse("2026-11-21T23:00:00Z"));
        ClockPort clock = now::get;
        var guard = new InMemoryLoginAttemptGuardAdapter(clock, 3, Duration.ofMinutes(15));

        assertThat(guard.isBlocked("user@creatingj.dev")).isFalse();
        guard.recordFailure("user@creatingj.dev");
        guard.recordFailure("user@creatingj.dev");
        assertThat(guard.isBlocked("user@creatingj.dev")).isFalse();
        guard.recordFailure("user@creatingj.dev");
        assertThat(guard.isBlocked("user@creatingj.dev")).isTrue();

        now.set(now.get().plus(Duration.ofMinutes(16)));
        assertThat(guard.isBlocked("user@creatingj.dev")).isFalse();
    }
}
