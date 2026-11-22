package io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1;

import io.github.jvpgjava.creatingj.identity.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Payload administrativo para criar um usuario gerenciado sem auto-registro")
public record CreateManagedUserRequest(
        @Schema(description = "E-mail corporativo ou pessoal unico do usuario", example = "joao@creatingj.dev")
        @NotBlank @Email @Size(max = 254) String email,
        @Schema(description = "Nome de exibicao do usuario", example = "Joao Grando")
        @NotBlank @Size(min = 2, max = 120) String displayName,
        @Schema(description = "Papeis atribuidos (MEMBER ou ADMIN; padrao MEMBER se omitido)",
                example = "[\"MEMBER\"]")
        Set<Role> roles
) {
}
