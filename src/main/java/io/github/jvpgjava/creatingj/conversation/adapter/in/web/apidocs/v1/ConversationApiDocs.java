package io.github.jvpgjava.creatingj.conversation.adapter.in.web.apidocs.v1;

import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.ConversationHistoryResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.ConversationSummaryResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.ConversationsPageResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.CreateConversationRequest;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.GenerationSummaryResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.MessageExchangeResponse;
import io.github.jvpgjava.creatingj.conversation.adapter.in.web.dto.v1.SendMessageRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Conversations", description = "Conversas, historico, chat sincrono, streaming SSE e cancelamento")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "cookieAuth")
public interface ConversationApiDocs {

    @Operation(
            operationId = "createConversation",
            summary = "Criar nova conversa do usuario autenticado"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conversa criada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Entrada invalida",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ConversationSummaryResponse> create(CreateConversationRequest request, AuthenticatedUser actor);

    @Operation(
            operationId = "listOwnConversations",
            summary = "Listar conversas do usuario autenticado de forma paginada"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de conversas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationsPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ConversationsPageResponse> list(int page, int size, AuthenticatedUser actor);

    @Operation(
            operationId = "getConversationHistory",
            summary = "Consultar historico completo de mensagens e geracoes de uma conversa"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historico retornado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationHistoryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Conversa nao encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ConversationHistoryResponse> history(String conversationId, AuthenticatedUser actor);

    @Operation(
            operationId = "sendMessage",
            summary = "Enviar mensagem e receber resposta sincrona do modelo"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resposta gerada e persistida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MessageExchangeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conversa nao encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "429", description = "Limite de taxa do provedor excedido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "Erro sanitizado de integracao com provedor",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Provedor temporariamente indisponivel",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "504", description = "Tempo limite do provedor excedido",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<MessageExchangeResponse> sendMessage(
            String conversationId,
            SendMessageRequest request,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "streamMessageGeneration",
            summary = "Enviar mensagem e receber resposta em streaming SSE",
            description = "Emite eventos SSE com id sequencial: generation.started, generation.delta, "
                    + "generation.completed, generation.cancelled ou generation.failed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stream SSE iniciado",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
            @ApiResponse(responseCode = "404", description = "Conversa nao encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    SseEmitter streamMessage(
            String conversationId,
            SendMessageRequest request,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "cancelGeneration",
            summary = "Cancelar geracao em andamento da conversa"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status da geracao apos cancelamento",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = GenerationSummaryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conversa ou geracao nao encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<GenerationSummaryResponse> cancelGeneration(
            String conversationId,
            String generationId,
            AuthenticatedUser actor
    );
}
