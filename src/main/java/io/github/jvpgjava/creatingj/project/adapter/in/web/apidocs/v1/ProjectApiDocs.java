package io.github.jvpgjava.creatingj.project.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.CreateProjectRequest;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.ProjectResponse;
import io.github.jvpgjava.creatingj.project.adapter.in.web.dto.v1.ProjectsPageResponse;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(operationId = "createProject", summary = "Criar um projeto proprio")
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

    @Operation(operationId = "listProjects", summary = "Listar projetos do usuario autenticado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de projetos",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProjectsPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ProjectsPageResponse> list(int page, int size, AuthenticatedUser actor);

    @Operation(operationId = "getProject", summary = "Obter detalhes de um projeto proprio")
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
    ResponseEntity<ProjectResponse> getById(String projectId, AuthenticatedUser actor);

    @Operation(operationId = "deleteProject", summary = "Excluir projeto proprio e respectivos vetores RAG")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Projeto excluido"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<Void> deleteById(String projectId, AuthenticatedUser actor);
}
