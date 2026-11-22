package io.github.jvpgjava.creatingj.common;

import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.application.usecase.GetSystemStatusInteractor;
import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.common.domain.SystemStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SystemStatusDomainAndUseCaseTest {

    private static final Instant FIXED_TIME = Instant.parse("2026-11-21T23:00:00Z");

    @Test
    void shouldCreateValidSystemStatusAndExposeFields() {
        var status = SystemStatus.up("creatingj-backend", "v1", FIXED_TIME);

        assertThat(status.service()).isEqualTo("creatingj-backend");
        assertThat(status.apiVersion()).isEqualTo("v1");
        assertThat(status.status()).isEqualTo("UP");
        assertThat(status.timestamp()).isEqualTo(FIXED_TIME);
    }

    @Test
    void shouldRejectBlankOrNullFieldsInSystemStatus() {
        assertThatThrownBy(() -> new SystemStatus(null, "v1", "UP", FIXED_TIME))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> new SystemStatus("   ", "v1", "UP", FIXED_TIME))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> new SystemStatus("creatingj-backend", "", "UP", FIXED_TIME))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> new SystemStatus("creatingj-backend", "v1", " ", FIXED_TIME))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);

        assertThatThrownBy(() -> new SystemStatus("creatingj-backend", "v1", "UP", null))
                .isInstanceOf(DomainException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void shouldPreserveCauseAndMetadataInDomainException() {
        var rootCause = new IllegalStateException("cause");
        var exception = new DomainException(ErrorCode.INVALID_STATE, "Estado invalido", rootCause);

        assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_STATE);
        assertThat(exception.getMessage()).isEqualTo("Estado invalido");
        assertThat(exception.getCause()).isSameAs(rootCause);
    }

    @Test
    void shouldRejectNullErrorCodeOrBlankMessageInDomainException() {
        assertThatThrownBy(() -> new DomainException(null, "msg"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DomainException(ErrorCode.VALIDATION_ERROR, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldReturnDeterministicSystemStatusFromInteractor() {
        ClockPort fakeClock = () -> FIXED_TIME;
        var interactor = new GetSystemStatusInteractor(fakeClock);

        var result = interactor.execute();

        assertThat(result.service()).isEqualTo("creatingj-backend");
        assertThat(result.apiVersion()).isEqualTo("v1");
        assertThat(result.status()).isEqualTo("UP");
        assertThat(result.timestamp()).isEqualTo(FIXED_TIME);
    }

    @Test
    void shouldRejectNullClockInInteractorConstructor() {
        assertThatThrownBy(() -> new GetSystemStatusInteractor(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
