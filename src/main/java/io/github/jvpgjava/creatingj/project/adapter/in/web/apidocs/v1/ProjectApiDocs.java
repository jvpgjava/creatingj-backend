package io.github.jvpgjava.creatingj.project.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.CreateProjectRequest;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.ProjectResponse;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.ProjectsPageResponse;
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

@Tag(name = "Projects", description = "Gestao de projetos isolados por proprietario")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "cookieAuth")
public interface ProjectApiDocs {

    @Operation(
            operationId = "createProject",
            summary = "Criar um projeto proprio",
            description = "Cria um projeto isolado na conta do usuario autenticado para organizar RAG e agentes."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Projeto criado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "400", description = "Entrada invalida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Slug ja utilizado na conta",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ProjectResponse> create(CreateProjectRequest request, AuthenticatedUser actor);

    @Operation(
            operationId = "listProjects",
            summary = "Listar projetos do usuario autenticado",
            description = "Retorna pagina de projetos pertencentes exclusivamente ao usuario autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de projetos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProjectsPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ProjectsPageResponse> list(
            @Parameter(description = "Indice da pagina (base 0)", example = "0")
            int page,
            @Parameter(description = "Tamanho da pagina (1..100)", example = "20")
            int size,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "getProject",
            summary = "Obter detalhes de um projeto proprio",
            description = "Consulta metadados de um projeto pertencente ao usuario autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Projeto encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProjectResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ProjectResponse> getById(
            @Parameter(description = "Identificador UUID do projeto",
                    example = "550e8400-e29b-41d4-a716-446655440010")
            String projectId,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "deleteProject",
            summary = "Excluir projeto proprio e respectivos vetores RAG",
            description = "Remove o projeto e, em cascata, todas as fontes de conhecimento, chunks e jobs vinculados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Projeto excluido"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<Void> deleteById(
            @Parameter(description = "Identificador UUID do projeto",
                    example = "550e8400-e29b-41d4-a716-446655440010")
            String projectId,
            AuthenticatedUser actor
    );
}
