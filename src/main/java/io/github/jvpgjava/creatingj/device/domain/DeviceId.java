package io.github.jvpgjava.creatingj.device.domain;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;

import java.util.UUID;

public record DeviceId(UUID value) {

    public DeviceId {
        if (value == null) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador do dispositivo obrigatorio.");
        }
    }

    public static DeviceId random() {
        return new DeviceId(UUID.randomUUID());
    }

    public static DeviceId parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador do dispositivo obrigatorio.");
        }
        try {
            return new DeviceId(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Identificador do dispositivo invalido.");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
