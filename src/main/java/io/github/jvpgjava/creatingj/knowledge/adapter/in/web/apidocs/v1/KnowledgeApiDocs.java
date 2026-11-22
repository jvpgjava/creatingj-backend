package io.github.jvpgjava.creatingj.knowledge.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.IngestKnowledgeSourceRequest;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.IngestKnowledgeSourceResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeIndexJobsResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeSearchRequest;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeSearchResponse;
import io.github.jvpgjava.creatingj.knowledge.adapter.in.web.dto.v1.KnowledgeSourcesResponse;
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

@Tag(name = "Knowledge RAG", description = "Ingestao, indexacao vetorial pgvector e busca RAG isolada por projeto")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "cookieAuth")
public interface KnowledgeApiDocs {

    @Operation(
            operationId = "ingestKnowledgeSource",
            summary = "Ingerir ou atualizar fonte de conhecimento de um projeto com deduplicacao e pgvector"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Fonte ingerida ou deduplicada sem alteracao",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IngestKnowledgeSourceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Caminho sensivel, segredo ou payload invalido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<IngestKnowledgeSourceResponse> ingest(
            String projectId,
            IngestKnowledgeSourceRequest request,
            AuthenticatedUser actor
    );

    @Operation(operationId = "listKnowledgeSources", summary = "Listar fontes indexadas de um projeto proprio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de fontes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KnowledgeSourcesResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<KnowledgeSourcesResponse> listSources(String projectId, AuthenticatedUser actor);

    @Operation(operationId = "deleteKnowledgeSource", summary = "Excluir fonte de conhecimento e seus vetores")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Fonte e vetores removidos"),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Projeto ou fonte nao encontrados",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<Void> deleteSource(String projectId, String sourceId, AuthenticatedUser actor);

    @Operation(
            operationId = "searchProjectKnowledge",
            summary = "Executar busca vetorial pgvector filtrada por proprietario e projeto"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trechos mais relevantes com citacao de fonte",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KnowledgeSearchResponse.class))),
            @ApiResponse(responseCode = "400", description = "Consulta invalida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<KnowledgeSearchResponse> search(
            String projectId,
            KnowledgeSearchRequest request,
            AuthenticatedUser actor
    );

    @Operation(operationId = "listKnowledgeIndexJobs", summary = "Listar jobs recentes de indexacao do projeto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de jobs de indexacao",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = KnowledgeIndexJobsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Projeto nao encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<KnowledgeIndexJobsResponse> listJobs(String projectId, int limit, AuthenticatedUser actor);
}
