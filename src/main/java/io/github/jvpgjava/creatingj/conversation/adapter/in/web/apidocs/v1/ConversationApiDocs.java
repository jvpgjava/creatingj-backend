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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Conversations", description = "Conversas, historico, chat sincrono, streaming SSE e cancelamento")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "cookieAuth")
public interface ConversationApiDocs {

    @Operation(
            operationId = "createConversation",
            summary = "Criar nova conversa do usuario autenticado",
            description = "Cria uma conversa isolada para o usuario autenticado, usando o modelo informado ou o "
                    + "modelo padrao do provedor."
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
            summary = "Listar conversas do usuario autenticado de forma paginada",
            description = "Retorna as conversas proprias ordenadas pela atualizacao mais recente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagina de conversas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ConversationsPageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Nao autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    ResponseEntity<ConversationsPageResponse> list(
            @Parameter(description = "Indice da pagina (base 0)", example = "0")
            int page,
            @Parameter(description = "Tamanho da pagina (1..100)", example = "20")
            int size,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "getConversationHistory",
            summary = "Consultar historico completo de mensagens e geracoes de uma conversa",
            description = "Retorna metadados da conversa, mensagens ordenadas por sequencia e geracoes registradas."
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
    ResponseEntity<ConversationHistoryResponse> history(
            @Parameter(description = "Identificador UUID da conversa",
                    example = "550e8400-e29b-41d4-a716-446655440020")
            String conversationId,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "sendMessage",
            summary = "Enviar mensagem e receber resposta sincrona do modelo",
            description = "Persiste a mensagem do usuario, consulta o provedor configurado respeitando cotas e "
                    + "rate limit e retorna a resposta completa do assistente."
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
            @Parameter(description = "Identificador UUID da conversa",
                    example = "550e8400-e29b-41d4-a716-446655440020")
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
            @Parameter(description = "Identificador UUID da conversa",
                    example = "550e8400-e29b-41d4-a716-446655440020")
            String conversationId,
            SendMessageRequest request,
            AuthenticatedUser actor
    );

    @Operation(
            operationId = "cancelGeneration",
            summary = "Cancelar geracao em andamento da conversa",
            description = "Sinaliza o cancelamento de uma geracao ativa de streaming e retorna seu estado atualizado."
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
            @Parameter(description = "Identificador UUID da conversa",
                    example = "550e8400-e29b-41d4-a716-446655440020")
            String conversationId,
            @Parameter(description = "Identificador UUID da geracao",
                    example = "550e8400-e29b-41d4-a716-446655440021")
            String generationId,
            AuthenticatedUser actor
    );
}
