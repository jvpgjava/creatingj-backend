package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.github.jvpgjava.creatingj.identity.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateManagedUserRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 2, max = 120) String displayName,
        Set<Role> roles
) {
}
