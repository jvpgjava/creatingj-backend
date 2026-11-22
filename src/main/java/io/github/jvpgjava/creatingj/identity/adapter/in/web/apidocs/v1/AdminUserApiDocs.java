package io.github.jvpgjava.creatingj.identity.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.CreateManagedUserRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.CreatedManagedUserResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.UserSummaryResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.UsersPageResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

@Tag(name = "Admin Users", description = "Gestao administrativa de contas CreatingJ (restrito a ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public interface AdminUserApiDocs {

    @Operation(
            operationId = "createManagedUser",
            summary = "Criar conta gerenciada com convite de ativacao",
            description = "Cria usuario pendente de ativacao e retorna token de ativacao de uso unico."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreatedManagedUserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Entrada invalida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Sem privilegio administrativo",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Email ja cadastrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<CreatedManagedUserResponse> createUser(
            CreateManagedUserRequest request,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "listManagedUsers",
            summary = "Listar contas gerenciadas de forma paginada",
            description = "Retorna usuarios filtrados por status e ordenados por campos permitidos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de usuarios",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UsersPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametros de paginacao ou ordenacao invalidos",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Sem privilegio administrativo",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<UsersPageResponse> listUsers(
            @Parameter(description = "Indice da pagina (base 0)", example = "0")
            int page,
            @Parameter(description = "Tamanho da pagina (1..100)", example = "20")
            int size,
            @Parameter(description = "Filtro opcional de status (PENDING_ACTIVATION, ACTIVE, SUSPENDED)",
                    example = "ACTIVE")
            String status,
            @Parameter(description = "Campo de ordenacao permitido (createdAt, email, displayName)",
                    example = "createdAt")
            String sort,
            @Parameter(description = "Direcao da ordenacao (asc ou desc)", example = "desc")
            String direction,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "suspendManagedUser",
            summary = "Suspender conta de usuario e revogar sessoes",
            description = "Bloqueia imediatamente a conta alvo e revoga todas as sessoes ativas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario suspenso",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Sem privilegio administrativo",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Usuario nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Auto-suspensao ou conta ja suspensa",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<UserSummaryResponse> suspendUser(
            @Parameter(description = "Identificador UUID do usuario alvo",
                    example = "550e8400-e29b-41d4-a716-446655440001")
            String userId,
            AuthenticatedUser actor
    );
}
