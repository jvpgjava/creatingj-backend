# CreatingJ Backend

[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.2.0-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.1.0-6DB33F)](https://spring.io/projects/spring-ai)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18%20%2B%20pgvector-336791?logo=postgresql&logoColor=white)](https://github.com/pgvector/pgvector)
[![OpenAPI](https://img.shields.io/badge/OpenAPI-v1-6BA539?logo=openapiinitiative&logoColor=white)](openapi/openapi-v1.yaml)

Backend principal da plataforma **CreatingJ**, desenvolvido como um **monólito modular hexagonal** em Java 25 e Spring Boot 4.2, com chat em tempo real (SSE), RAG vetorial isolado por projeto (`pgvector` + embeddings ONNX locais via Spring AI ou API OpenAI-compatible), orquestração de agente com política determinística de segurança e protocolo de execução remota para dispositivos pareados (`creatingj-agent`).

---

## Sumário

- [Módulos e Funcionalidades](#módulos-e-funcionalidades)
- [Arquitetura e Stack](#arquitetura-e-stack)
- [Pré-requisitos](#pré-requisitos)
- [Como Rodar Localmente (Passo a Passo)](#como-rodar-localmente-passo-a-passo)
- [Troca de Provedor de IA pelo `.env`](#troca-de-provedor-de-ia-pelo-env)
- [Troca de Embeddings pelo `.env`](#troca-de-embeddings-pelo-env)
- [Documentação da API (Swagger / OpenAPI v1)](#documentação-da-api-swagger--openapi-v1)
- [Perfis de Execução](#perfis-de-execução)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Testes e Quality Gates](#testes-e-quality-gates)
- [Segurança por Padrão](#segurança-por-padrão)

---

## Módulos e Funcionalidades

| Módulo | Responsabilidade |
| :--- | :--- |
| **`identity`** | Contas gerenciadas (sem auto-registro), ativação por convite único, Argon2id, login web com cookie `HttpOnly` + CSRF, OAuth 2.0 PKCE (`S256`) com rotação de refresh token e detecção de reuso, gestão de sessões e reset de senha anti-enumeração. |
| **`conversation`** | Conversas isoladas por usuário, histórico persistido, chat síncrono, streaming SSE (`text/event-stream`) e cancelamento de geração em andamento. |
| **`inference`** | Gateway HTTP compatível com OpenAI (`/v1/chat/completions`), medição de tokens/latência, cotas diárias, limite de concorrência, rate limit e métricas Micrometer. |
| **`project`** | CRUD de projetos isolados por proprietário com slug único e exclusão em cascata. |
| **`knowledge`** | RAG híbrido por projeto com `pgvector` (HNSW parcial `384`/`768`/`1024`/`1536`) + Full-Text Search `tsvector` (`GIN`) com fusão RRF (`k=60`), expansão de chunks irmãos em documentos curtos, chunking estruturado com breadcrumbs de seções Markdown (`[Secao: ...]`), avaliador de suficiência de contexto (`SUFFICIENT`/`PARTIAL`/`INSUFFICIENT`), bloqueio de segredos e deduplicação SHA-256. |
| **`agent`** | Planejamento de agente com contexto de workspace e RAG, avaliado por motor determinístico de política de ferramentas (`ALLOW`, `REQUIRE_APPROVAL`, `DENY`) imune a prompt injection. |
| **`device`** | Pareamento de dispositivos com código temporário de uso único, credencial dedicada `X-Device-Token` (`ROLE_RUNNER`), heartbeat de presença e revogação imediata. |
| **`task`** | Fila de tarefas remotas com idempotência, lease temporário, deduplicação de eventos, aprovações pontuais com TTL e stream SSE de progresso. |
| **`maintenance`** | Rotina administrativa (`ROLE_ADMIN`) de retenção e expurgo de sessões, desafios de pareamento, tarefas, conversas arquivadas e auditoria. |
| **`audit` / `common`** | Trilha de auditoria sanitizada, `traceId` por requisição, status operacional e respostas de erro padronizadas em RFC 9457 (`ProblemDetail`). |

---

## Arquitetura e Stack

### Stack Principal

- **Linguagem:** Java 25 (`--release 25`)
- **Framework:** Spring Boot `4.2.0` (WebMVC, Security, Validation, JDBC, Actuator)
- **IA & Embeddings:** Spring AI `2.1.0` (`spring-ai-transformers` ONNX local + cliente HTTP OpenAI-compatible)
- **Banco de Dados:** PostgreSQL 18 + extensão `pgvector` + Full-Text Search `tsvector` + migrations Flyway (`V1` a `V11`)
- **Documentação HTTP:** Springdoc OpenAPI `3.2.1` + snapshot versionado em [`openapi/openapi-v1.yaml`](openapi/openapi-v1.yaml)
- **Criptografia:** BouncyCastle `1.87` (Argon2id para senhas)
- **Qualidade & Governança:** Checkstyle `14.4.0`, ArchUnit `1.5.1`, JaCoCo `0.8.16`, CI/CD via [`Jenkinsfile`](Jenkinsfile)

### Organização Hexagonal por Módulo

```text
src/main/java/io/github/jvpgjava/creatingj/
├── <modulo>/
│   ├── domain/                      # Núcleo puro (zero Spring, JPA, HTTP, Jackson, Swagger ou Spring AI)
│   ├── application/
│   │   ├── port/in/                 # Casos de uso (portas de entrada)
│   │   ├── port/out/                # Contratos de persistência, IA e serviços externos
│   │   └── usecase/                 # Implementação das regras de aplicação
│   └── adapter/
│       ├── in/web/
│       │   ├── controller/v1/       # Controllers finos (zero anotações Swagger)
│       │   ├── apidocs/v1/          # Interfaces OpenAPI/Swagger com exemplos e parâmetros
│       │   └── dto/v1/              # Records de request/response com Bean Validation e @Schema
│       └── out/                     # Persistência JDBC/pgvector, provedores de IA e segurança
└── bootstrap/                       # Configuração Spring, segurança, OpenAPI e exception handler
```

As fronteiras arquiteturais são validadas automaticamente em todo build pela suíte `HexagonalArchitectureTest` (ArchUnit).

---

## Pré-requisitos

- **Java 25 (JDK 25)** instalado e no `PATH` (ex.: Eclipse Temurin 25)
- **PostgreSQL 18** rodando localmente com a extensão **`pgvector`** disponível (o projeto não exige Docker)
- **Git**

Verifique rapidamente no terminal:

```bash
java -version
psql --version
```

---

## Como Rodar Localmente (Passo a Passo)

### 1. Clonar o repositório

```bash
git clone https://github.com/jvpgjava/creatingj-backend.git
cd creatingj-backend
```

### 2. Criar o banco PostgreSQL com `pgvector`

Conecte-se ao PostgreSQL com um usuário com permissão de criação de banco e extensões e execute:

```sql
CREATE DATABASE creatingj;
\c creatingj
CREATE EXTENSION IF NOT EXISTS vector;
```

> As migrations Flyway (`V1` até `V11`) são aplicadas automaticamente na primeira inicialização da aplicação.

### 3. Criar o arquivo `.env` na raiz do projeto

No perfil `local`, o Spring Boot importa automaticamente o arquivo `.env` da raiz do repositório (que já está listado no `.gitignore` e nunca é versionado).

Crie um arquivo `.env` na raiz com o conteúdo abaixo ajustado para o seu ambiente:

```env
# Servidor
SERVER_PORT=8080

# Banco PostgreSQL 18 + pgvector
CREATINGJ_DB_URL=jdbc:postgresql://127.0.0.1:5432/creatingj
CREATINGJ_DB_USER=postgres
CREATINGJ_DB_PASSWORD=sua-senha-do-postgres

# Provedor de Chat / Agente (qualquer API OpenAI-compatible)
CRJ_API_BASE_URL=https://api.seu-provedor.com/v1
CRJ_API_KEY=sua-chave-de-api
CRJ_CHAT_MODEL=seu-modelo-de-chat

# Embeddings (padrão recomendado: Spring AI ONNX local 768 dimensões)
CRJ_EMBEDDING_PROVIDER=onnx-local
CRJ_EMBEDDING_DIMENSIONS=768
```

> Na primeira inicialização com `CRJ_EMBEDDING_PROVIDER=onnx-local`, o backend faz o download do modelo quantizado `Xenova/multilingual-e5-base` INT8 (~278 MB) e salva em cache local (`~/.creatingj/models/embeddings`) para reutilização instantânea nas próximas execuções.

### 4. Executar a aplicação

**Linux / macOS:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Windows (PowerShell):**

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

### 5. Validar que está tudo no ar

Com a aplicação rodando na porta `8080`:

- **Status da API v1:** `http://localhost:8080/api/v1/system/status`
- **Health Check:** `http://localhost:8080/actuator/health`
- **Swagger UI (com exemplos prontos para teste):** `http://localhost:8080/api-docs/ui`
- **OpenAPI JSON v1:** `http://localhost:8080/api-docs/v1`
- **OpenAPI YAML v1:** `http://localhost:8080/api-docs.yaml/v1`

---

## Troca de Provedor de IA pelo `.env`

O gateway de inferência (`OpenAiCompatibleModelGatewayAdapter`) é totalmente desacoplado de provedor. Para trocar o provedor de chat e planejamento de agente, basta atualizar `CRJ_API_BASE_URL`, `CRJ_API_KEY` e `CRJ_CHAT_MODEL` no `.env` e reiniciar:

| Provedor | `CRJ_API_BASE_URL` | Exemplo de `CRJ_CHAT_MODEL` |
| :--- | :--- | :--- |
| **OpenAI** | `https://api.openai.com/v1` | `gpt-4.1-mini` |
| **Google Gemini (OpenAI-compat)** | `https://generativelanguage.googleapis.com/v1beta/openai` | `gemini-2.5-flash` |
| **OpenRouter** | `https://openrouter.ai/api/v1` | `anthropic/claude-sonnet-4` |
| **DeepSeek** | `https://api.deepseek.com/v1` | `deepseek-chat` |
| **Groq** | `https://api.groq.com/openai/v1` | `llama-3.3-70b-versatile` |
| **Ollama local** *(com `CREATINGJ_PROVIDER_AI_STRICT_URL_VALIDATION=false`)* | `http://127.0.0.1:11434/v1` | `qwen2.5-coder:7b` |

---

## Troca de Embeddings pelo `.env`

A coluna `knowledge_chunks.embedding` usa dimensão dinâmica (`V10__dynamic_embedding_dimensions.sql`) com **índices HNSW parciais prontos para `384`, `768`, `1024` e `1536` dimensões** (aceitando de `16` até `4096`). É possível trocar provedor, modelo e dimensão apenas editando o `.env`:

### Opção A — Spring AI ONNX Local `768` dims (padrão recomendado, multilíngue PT/EN)

```env
CRJ_EMBEDDING_PROVIDER=onnx-local
CRJ_EMBEDDING_DIMENSIONS=768
```

### Opção B — Outro modelo ONNX local `384` dims (ex.: `all-MiniLM-L6-v2`)

```env
CRJ_EMBEDDING_PROVIDER=onnx-local
CRJ_EMBEDDING_DIMENSIONS=384
CRJ_EMBEDDING_ONNX_MODEL_URI=https://huggingface.co/Xenova/all-MiniLM-L6-v2/resolve/main/onnx/model_quantized.onnx
CRJ_EMBEDDING_ONNX_TOKENIZER_URI=https://huggingface.co/Xenova/all-MiniLM-L6-v2/resolve/main/tokenizer.json
CRJ_EMBEDDING_ONNX_VERSION_TAG=onnx:all-minilm-l6-v2-q8:384
CRJ_EMBEDDING_ONNX_DOCUMENT_PREFIX=
CRJ_EMBEDDING_ONNX_QUERY_PREFIX=
```

### Opção C — Provedor HTTP OpenAI-compatible (OpenAI, Jina, Voyage, etc.)

```env
CRJ_EMBEDDING_PROVIDER=openai-compatible
CRJ_EMBEDDING_BASE_URL=https://api.openai.com/v1
CRJ_EMBEDDING_API_KEY=sua-chave-de-embeddings
CRJ_EMBEDDING_MODEL=text-embedding-3-small
CRJ_EMBEDDING_DIMENSIONS=1536
```

### Opção D — Ollama local (`http://127.0.0.1:11434/v1`)

Como o backend aplica validação anti-SSRF por padrão (bloqueando `http://` e `127.0.0.1`), para apontar para um Ollama local inclua a flag de relaxamento de URL:

```env
CRJ_EMBEDDING_PROVIDER=openai-compatible
CRJ_EMBEDDING_BASE_URL=http://127.0.0.1:11434/v1
CRJ_EMBEDDING_API_KEY=ollama
CRJ_EMBEDDING_MODEL=nomic-embed-text
CRJ_EMBEDDING_DIMENSIONS=768
CREATINGJ_KNOWLEDGE_EMBEDDING_STRICT_URL_VALIDATION=false
```

---

## Documentação da API (Swagger / OpenAPI v1)

- Em perfil `local`, o Swagger UI fica acessível em `http://localhost:8080/api-docs/ui`.
- Todos os DTOs de request/response e parâmetros de path/query possuem descrições e exemplos prontos para uso com **Try it out**.
- **Autenticação no Swagger UI:**
  1. Execute `POST /api/v1/auth/login` para obter um `accessToken`.
  2. Clique em **Authorize** e cole o token em `bearerAuth`.
  3. Para o fluxo web por cookie (`POST /api/v1/auth/web/login`), envie o header `X-XSRF-TOKEN` em mutações.
  4. Endpoints `/api/v1/runner/**` utilizam exclusivamente `deviceTokenAuth` (`X-Device-Token`).
- O contrato versionado fica congelado em [`openapi/openapi-v1.yaml`](openapi/openapi-v1.yaml) e validado pelo `OpenApiContractTest`. Ao evoluir intencionalmente o contrato v1, atualize o snapshot com:

```bash
./mvnw -B test -Dtest=OpenApiContractTest -Dcreatingj.openapi.updateSnapshot=true
```

---

## Perfis de Execução

| Perfil | Uso | Swagger UI / `/api-docs` | Cookie `Secure` | `CRJ_API_KEY` obrigatória |
| :--- | :--- | :--- | :--- | :--- |
| `local` | Desenvolvimento local (importa `.env`) | Habilitado e público | `false` | Não |
| `test` | Testes unitários e de contrato WebMVC | Habilitado | `false` | Não |
| `postgres-it` | Testes de integração com PostgreSQL real | Restrito | `false` | Não |
| `prod` | Produção (bind `127.0.0.1` atrás de proxy TLS) | **Desabilitado** | **`true`** | **Sim** |

---

## Variáveis de Ambiente

| Variável | Obrigatória | Padrão | Descrição |
| :--- | :---: | :--- | :--- |
| `SERVER_PORT` | Não | `8080` | Porta HTTP da aplicação |
| `CREATINGJ_DB_URL` | Sim | — | URL JDBC do PostgreSQL (ex.: `jdbc:postgresql://127.0.0.1:5432/creatingj`) |
| `CREATINGJ_DB_USER` | Sim | — | Usuário do PostgreSQL |
| `CREATINGJ_DB_PASSWORD` | Sim | — | Senha do PostgreSQL |
| `CRJ_API_BASE_URL` | Não | Configurável | URL base OpenAI-compatible do provedor de chat/agente |
| `CRJ_API_KEY` | Em `prod` | — | Chave de API do provedor de IA |
| `CRJ_CHAT_MODEL` | Não | Configurável | Modelo padrão de chat e planejamento de agente |
| `CRJ_EMBEDDING_PROVIDER` | Não | `local-hash` (`onnx-local` em `prod`) | Provedor de embeddings: `onnx-local`, `openai-compatible` ou `local-hash` |
| `CRJ_EMBEDDING_DIMENSIONS` | Não | `768` | Dimensão dos vetores (`16` a `4096`; HNSW otimizado para `384`, `768`, `1024`, `1536`) |
| `CRJ_EMBEDDING_BASE_URL` | Não | `CRJ_API_BASE_URL` | URL base do endpoint `/embeddings` quando `openai-compatible` |
| `CRJ_EMBEDDING_API_KEY` | Não | `CRJ_API_KEY` | Chave do provedor de embeddings quando `openai-compatible` |
| `CRJ_EMBEDDING_MODEL` | Quando `openai-compatible` | — | Modelo de embedding enviado no payload `/embeddings` |
| `CRJ_EMBEDDING_ONNX_MODEL_URI` | Não | `multilingual-e5-base` INT8 | URI `https://` ou `file://` do arquivo `.onnx` |
| `CRJ_EMBEDDING_ONNX_TOKENIZER_URI` | Não | `tokenizer.json` do modelo | URI `https://` ou `file://` do `tokenizer.json` |
| `CRJ_EMBEDDING_ONNX_CACHE_DIR` | Não | `~/.creatingj/models/embeddings` | Diretório local de cache dos modelos ONNX |
| `CRJ_EMBEDDING_ONNX_VERSION_TAG` | Não | `onnx:multilingual-e5-base-q8:768` | Identificador da versão gravada nos chunks vetoriais |
| `CRJ_EMBEDDING_ONNX_DOCUMENT_PREFIX` | Não | `"passage: "` | Prefixo aplicado aos trechos de documento no ONNX |
| `CRJ_EMBEDDING_ONNX_QUERY_PREFIX` | Não | `"query: "` | Prefixo aplicado às consultas de busca no ONNX |

---

## Testes e Quality Gates

### Build completo (Checkstyle + Compilação Java 25 + Unitários + ArchUnit + Contrato OpenAPI + JaCoCo)

```bash
./mvnw -B clean verify
```

### Incluindo testes de integração reais no PostgreSQL 18 + `pgvector`

Crie um banco de testes separado (`creatingj_test`) com `CREATE EXTENSION IF NOT EXISTS vector;` e exporte as variáveis do banco antes de rodar o `verify`:

**Linux / macOS:**

```bash
export CREATINGJ_DB_URL=jdbc:postgresql://127.0.0.1:5432/creatingj_test
export CREATINGJ_DB_USER=postgres
export CREATINGJ_DB_PASSWORD=postgres
./mvnw -B clean verify
```

**Windows (PowerShell):**

```powershell
$env:CREATINGJ_DB_URL="jdbc:postgresql://127.0.0.1:5432/creatingj_test"
$env:CREATINGJ_DB_USER="postgres"
$env:CREATINGJ_DB_PASSWORD="postgres"
.\mvnw.cmd -B clean verify
```

### Teste E2E Live (ONNX neural 768 + Provedor de IA real)

O teste `LiveOnnxRagAndStacE2EIT` é *opt-in* e executa busca semântica cross-lingual real + chat + orquestração de agente contra o provedor configurado em `CRJ_API_KEY`:

```powershell
$env:CRJ_RUN_LIVE_E2E="true"
$env:CREATINGJ_DB_URL="jdbc:postgresql://127.0.0.1:5432/creatingj_test"
$env:CREATINGJ_DB_USER="postgres"
$env:CREATINGJ_DB_PASSWORD="postgres"
.\mvnw.cmd -B failsafe:integration-test failsafe:verify "-Dit.test=LiveOnnxRagAndStacE2EIT"
```

### Quality Gates configurados no Maven

- **Checkstyle:** `0` violações obrigatórias (`mvn checkstyle:check`)
- **ArchUnit:** 8 regras arquiteturais verificando isolamento hexagonal, ausência de Swagger nos controllers e proibição de execução local de processos/filesystem em `agent`, `device` e `task`
- **JaCoCo:** cobertura mínima de **85% de linhas** e **75% de branches** nas camadas `domain` e `application`
- **OpenAPI Snapshot:** `OpenApiContractTest` garante sincronia entre o código e [`openapi/openapi-v1.yaml`](openapi/openapi-v1.yaml)

---

## Segurança por Padrão

- **Autenticação:** hashing **Argon2id**, tokens opacos com hash SHA-256 no banco, proteção contra brute force e respostas anti-enumeração.
- **Web & Multi-Client:** cookie `HttpOnly` + `SameSite` + proteção CSRF (`X-XSRF-TOKEN`) na web; OAuth 2.0 PKCE (`S256`) com rotação de refresh token e revogação automática da família em caso de reuso.
- **Isolamento de Runner:** credenciais `X-Device-Token` (`ROLE_RUNNER`) são aceitas estritamente em `/api/v1/runner/**`.
- **Anti-SSRF & Guardrails Multicamada de IA:**
  - **Guarda de Entrada L0 (`PromptSecurityGuard`):** bloqueio determinístico (com normalização NFKD sem acentos e anti-leetspeak) de prompt injection, sequestro de persona (`DAN` / `developer mode`), extração de system prompt, payloads ofuscados (Base64, binário, hexadecimal, letras espaçadas, caracteres invisíveis Unicode) e solicitações de malware/exploits ofensivos — curto-circuitando antes de consumir requisição do provedor.
  - **Guarda de Saída e Streaming em Blocos (`OutputSecurityGuard` + `StreamingBlockOutputGuard`):** redação automática de segredos/chaves (`[REDACTED_SECRET]`), remoção de referências internas de RAG (`[T1]`), neutralização de comandos destrutivos (`rm -rf /`, fork bombs) e retenção adaptativa em blocos durante o streaming SSE para impedir vazamento antes de exibir na tela.
  - **RAG Híbrido, Reescrita Multi-Turno e Suficiência (`HybridRagRanker` + `StandaloneQueryBuilder` + `RagSufficiencyEvaluator` + `FollowUpSuggestionEngine`):** fusão Reciprocal Rank Fusion (`pgvector` + `tsvector`), expansão de perguntas curtas de continuação, nota de suficiência de evidências e sugestões automáticas de follow-up.
  - **Política Determinística de Ferramentas (`ToolExecutionPolicyEngine`):** proteção contra escrita em scripts executáveis, wrappers de build (`mvnw`, `gradlew`), `Dockerfile` e pipelines CI/CD (`Jenkinsfile`, `.github/**`), bloqueio de execução inline de interpretadores (`python -c`, `node -e`, `-Dexec.`) e inspeção de `diffPreview` contra segredos e `ProcessBuilder`/`Runtime.exec`.
  - **Anti-SSRF & Ingestão Segura (`KnowledgeIngestionPolicy`):** validação estrita de URLs HTTPS externas, bloqueio de arquivos sensíveis e detecção de segredos antes da vetorização no `pgvector`.
- **Observabilidade segura:** erros sanitizados em RFC 9457 (`application/problem+json`) sem stacktraces, `traceId` correlacionado nos logs e `/api-docs/**` desabilitado em `prod`.
