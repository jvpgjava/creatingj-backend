package io.github.jvpgjava.creatingj.support;

import io.github.jvpgjava.creatingj.agent.application.port.out.AgentRunStorePort;
import io.github.jvpgjava.creatingj.agent.application.port.out.AgentToolProposalStorePort;
import io.github.jvpgjava.creatingj.agent.domain.AgentRun;
import io.github.jvpgjava.creatingj.agent.domain.AgentRunId;
import io.github.jvpgjava.creatingj.agent.domain.AgentToolProposal;
import io.github.jvpgjava.creatingj.agent.domain.ToolProposalId;
import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.audit.domain.AuditEvent;
import io.github.jvpgjava.creatingj.conversation.application.port.out.ConversationStorePort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.GenerationStorePort;
import io.github.jvpgjava.creatingj.conversation.application.port.out.MessageStorePort;
import io.github.jvpgjava.creatingj.conversation.domain.ChatMessage;
import io.github.jvpgjava.creatingj.conversation.domain.Conversation;
import io.github.jvpgjava.creatingj.conversation.domain.ConversationId;
import io.github.jvpgjava.creatingj.conversation.domain.Generation;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceCredentialStorePort;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceStorePort;
import io.github.jvpgjava.creatingj.device.application.port.out.PairingChallengeStorePort;
import io.github.jvpgjava.creatingj.device.domain.Device;
import io.github.jvpgjava.creatingj.device.domain.DeviceCredential;
import io.github.jvpgjava.creatingj.device.domain.DeviceId;
import io.github.jvpgjava.creatingj.device.domain.PairingChallenge;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase.UserSortField;
import io.github.jvpgjava.creatingj.identity.application.port.out.AuthorizationCodeStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.InvitationStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.SessionStorePort;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.AccountStatus;
import io.github.jvpgjava.creatingj.identity.domain.AuthorizationCode;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.Invitation;
import io.github.jvpgjava.creatingj.identity.domain.InvitationPurpose;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.identity.domain.UserSession;
import io.github.jvpgjava.creatingj.inference.application.port.out.UsageRecordStorePort;
import io.github.jvpgjava.creatingj.inference.application.port.out.UserQuotaStorePort;
import io.github.jvpgjava.creatingj.inference.domain.UsageRecord;
import io.github.jvpgjava.creatingj.inference.domain.UserQuota;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeIndexJobStorePort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.KnowledgeSourceStorePort;
import io.github.jvpgjava.creatingj.knowledge.application.port.out.VectorKnowledgePort;
import io.github.jvpgjava.creatingj.knowledge.domain.EmbeddingVector;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeChunk;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeIndexJob;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSearchMatch;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSource;
import io.github.jvpgjava.creatingj.knowledge.domain.KnowledgeSourceId;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.project.domain.Project;
import io.github.jvpgjava.creatingj.project.domain.ProjectId;
import io.github.jvpgjava.creatingj.task.application.port.out.RemoteTaskStorePort;
import io.github.jvpgjava.creatingj.task.application.port.out.TaskEventStorePort;
import io.github.jvpgjava.creatingj.task.application.port.out.ToolApprovalStorePort;
import io.github.jvpgjava.creatingj.task.domain.RemoteTask;
import io.github.jvpgjava.creatingj.task.domain.TaskEvent;
import io.github.jvpgjava.creatingj.task.domain.TaskId;
import io.github.jvpgjava.creatingj.task.domain.ToolApproval;
import io.github.jvpgjava.creatingj.task.domain.ToolApprovalId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@TestConfiguration
public class TestInMemoryStoresConfig {

    @Bean
    public UserStorePort testUserStorePort() {
        return new UserStorePort() {
            private final Map<UserId, User> users = new ConcurrentHashMap<>();

            @Override
            public User save(User user) {
                users.put(user.id(), user);
                return user;
            }

            @Override
            public Optional<User> findById(UserId id) {
                return Optional.ofNullable(users.get(id));
            }

            @Override
            public Optional<User> findByEmail(Email email) {
                return users.values().stream().filter(u -> u.email().equals(email)).findFirst();
            }

            @Override
            public boolean existsByEmail(Email email) {
                return findByEmail(email).isPresent();
            }

            @Override
            public List<User> findPage(
                    int page,
                    int size,
                    AccountStatus status,
                    UserSortField sortBy,
                    boolean ascending
            ) {
                return users.values().stream()
                        .filter(u -> status == null || u.status() == status)
                        .skip((long) page * size)
                        .limit(size)
                        .toList();
            }

            @Override
            public long count(AccountStatus status) {
                return users.values().stream().filter(u -> status == null || u.status() == status).count();
            }
        };
    }

    @Bean
    public InvitationStorePort testInvitationStorePort() {
        return new InvitationStorePort() {
            private final Map<String, Invitation> store = new ConcurrentHashMap<>();

            @Override
            public Invitation save(Invitation invitation) {
                store.put(invitation.tokenHash(), invitation);
                return invitation;
            }

            @Override
            public Optional<Invitation> findByTokenHash(String tokenHash) {
                return Optional.ofNullable(store.get(tokenHash));
            }

            @Override
            public void invalidatePendingByUserAndPurpose(UserId userId, InvitationPurpose purpose, Instant now) {
                store.replaceAll((k, v) -> v.userId().equals(userId) && v.purpose() == purpose && v.usedAt() == null
                        ? new Invitation(v.id(), v.userId(), v.purpose(), v.tokenHash(), v.expiresAt(), now,
                                v.createdAt())
                        : v);
            }
        };
    }

    @Bean
    public SessionStorePort testSessionStorePort() {
        return new SessionStorePort() {
            private final Map<UUID, UserSession> store = new ConcurrentHashMap<>();

            @Override
            public UserSession save(UserSession session) {
                store.put(session.id(), session);
                return session;
            }

            @Override
            public Optional<UserSession> findById(UUID sessionId) {
                return Optional.ofNullable(store.get(sessionId));
            }

            @Override
            public Optional<UserSession> findByTokenHash(String tokenHash) {
                return store.values().stream().filter(s -> s.tokenHash().equals(tokenHash)).findFirst();
            }

            @Override
            public Optional<UserSession> findByRefreshTokenHash(String refreshTokenHash) {
                return store.values().stream()
                        .filter(s -> refreshTokenHash.equals(s.refreshTokenHash()))
                        .findFirst();
            }

            @Override
            public List<UserSession> findActiveByUserId(UserId userId, Instant now) {
                return store.values().stream()
                        .filter(s -> s.userId().equals(userId) && s.isActiveAt(now))
                        .toList();
            }

            @Override
            public void revokeAllByUserId(UserId userId, Instant revokedAt) {
                store.replaceAll((k, v) -> v.userId().equals(userId) && v.revokedAt() == null
                        ? v.revoke(revokedAt)
                        : v);
            }

            @Override
            public void revokeByFamilyId(UUID familyId, Instant revokedAt) {
                store.replaceAll((k, v) -> v.familyId().equals(familyId) && v.revokedAt() == null
                        ? v.revoke(revokedAt)
                        : v);
            }
        };
    }

    @Bean
    public AuthorizationCodeStorePort testAuthorizationCodeStorePort() {
        return new AuthorizationCodeStorePort() {
            private final Map<String, AuthorizationCode> store = new ConcurrentHashMap<>();

            @Override
            public AuthorizationCode save(AuthorizationCode code) {
                store.put(code.codeHash(), code);
                return code;
            }

            @Override
            public Optional<AuthorizationCode> findByCodeHash(String codeHash) {
                return Optional.ofNullable(store.get(codeHash));
            }
        };
    }

    @Bean
    public AuditPort testAuditPort() {
        List<AuditEvent> events = new CopyOnWriteArrayList<>();
        return events::add;
    }

    @Bean
    public ConversationStorePort testConversationStorePort() {
        return new ConversationStorePort() {
            private final Map<ConversationId, Conversation> store = new ConcurrentHashMap<>();

            @Override
            public Conversation save(Conversation conversation) {
                store.put(conversation.id(), conversation);
                return conversation;
            }

            @Override
            public Optional<Conversation> findByIdAndOwner(ConversationId id, UserId ownerUserId) {
                return Optional.ofNullable(store.get(id)).filter(c -> c.isOwnedBy(ownerUserId));
            }

            @Override
            public List<Conversation> findPageByOwner(UserId ownerUserId, int page, int size) {
                return store.values().stream()
                        .filter(c -> c.isOwnedBy(ownerUserId))
                        .skip((long) page * size)
                        .limit(size)
                        .toList();
            }

            @Override
            public long countByOwner(UserId ownerUserId) {
                return store.values().stream().filter(c -> c.isOwnedBy(ownerUserId)).count();
            }
        };
    }

    @Bean
    public MessageStorePort testMessageStorePort() {
        return new MessageStorePort() {
            private final List<ChatMessage> messages = new CopyOnWriteArrayList<>();

            @Override
            public ChatMessage save(ChatMessage message) {
                messages.add(message);
                return message;
            }

            @Override
            public List<ChatMessage> findByConversationOrderBySequenceAsc(ConversationId conversationId) {
                return messages.stream().filter(m -> m.conversationId().equals(conversationId)).toList();
            }

            @Override
            public int nextSequenceNumber(ConversationId conversationId) {
                return (int) messages.stream().filter(m -> m.conversationId().equals(conversationId)).count() + 1;
            }
        };
    }

    @Bean
    public GenerationStorePort testGenerationStorePort() {
        return new GenerationStorePort() {
            private final Map<UUID, Generation> store = new ConcurrentHashMap<>();

            @Override
            public Generation save(Generation generation) {
                store.put(generation.id(), generation);
                return generation;
            }

            @Override
            public Optional<Generation> findByIdAndConversationAndOwner(
                    UUID generationId,
                    ConversationId conversationId,
                    UserId ownerUserId
            ) {
                return Optional.ofNullable(store.get(generationId))
                        .filter(g -> g.conversationId().equals(conversationId) && g.ownerUserId().equals(ownerUserId));
            }

            @Override
            public List<Generation> findByConversationOrderByCreatedAtDesc(ConversationId conversationId) {
                return store.values().stream().filter(g -> g.conversationId().equals(conversationId)).toList();
            }
        };
    }

    @Bean
    public UserQuotaStorePort testUserQuotaStorePort() {
        return new UserQuotaStorePort() {
            private final Map<UserId, UserQuota> store = new ConcurrentHashMap<>();

            @Override
            public Optional<UserQuota> findByUserId(UserId userId) {
                return Optional.ofNullable(store.get(userId));
            }

            @Override
            public UserQuota save(UserQuota quota) {
                store.put(quota.userId(), quota);
                return quota;
            }
        };
    }

    @Bean
    public UsageRecordStorePort testUsageRecordStorePort() {
        return new UsageRecordStorePort() {
            private final List<UsageRecord> records = new CopyOnWriteArrayList<>();

            @Override
            public UsageRecord save(UsageRecord record) {
                records.add(record);
                return record;
            }

            @Override
            public long sumTokensByUserBetween(UserId userId, Instant startInclusive, Instant endExclusive) {
                return records.stream()
                        .filter(r -> r.userId().equals(userId)
                                && !r.recordedAt().isBefore(startInclusive)
                                && r.recordedAt().isBefore(endExclusive))
                        .mapToLong(UsageRecord::totalTokens)
                        .sum();
            }

            @Override
            public long countRequestsByUserBetween(UserId userId, Instant startInclusive, Instant endExclusive) {
                return records.stream()
                        .filter(r -> r.userId().equals(userId)
                                && !r.recordedAt().isBefore(startInclusive)
                                && r.recordedAt().isBefore(endExclusive))
                        .count();
            }

            @Override
            public List<UsageRecord> findRecentByUser(UserId userId, int limit) {
                return records.stream().filter(r -> r.userId().equals(userId)).limit(limit).toList();
            }

            @Override
            public long sumAllTokensBetween(Instant startInclusive, Instant endExclusive) {
                return records.stream()
                        .filter(r -> !r.recordedAt().isBefore(startInclusive) && r.recordedAt().isBefore(endExclusive))
                        .mapToLong(UsageRecord::totalTokens)
                        .sum();
            }

            @Override
            public long countAllRequestsBetween(Instant startInclusive, Instant endExclusive) {
                return records.stream()
                        .filter(r -> !r.recordedAt().isBefore(startInclusive) && r.recordedAt().isBefore(endExclusive))
                        .count();
            }
        };
    }

    @Bean
    public ProjectStorePort testProjectStorePort() {
        return new ProjectStorePort() {
            private final Map<ProjectId, Project> store = new ConcurrentHashMap<>();

            @Override
            public Project save(Project project) {
                store.put(project.id(), project);
                return project;
            }

            @Override
            public Optional<Project> findById(ProjectId projectId) {
                return Optional.ofNullable(store.get(projectId));
            }

            @Override
            public Optional<Project> findByOwnerAndSlug(UserId ownerUserId, String slug) {
                return store.values().stream()
                        .filter(p -> p.ownerUserId().equals(ownerUserId) && p.slug().equals(slug))
                        .findFirst();
            }

            @Override
            public List<Project> findByOwner(UserId ownerUserId, int offset, int limit) {
                return store.values().stream()
                        .filter(p -> p.ownerUserId().equals(ownerUserId))
                        .skip(offset)
                        .limit(limit)
                        .toList();
            }

            @Override
            public long countByOwner(UserId ownerUserId) {
                return store.values().stream().filter(p -> p.ownerUserId().equals(ownerUserId)).count();
            }

            @Override
            public void deleteById(ProjectId projectId) {
                store.remove(projectId);
            }
        };
    }

    @Bean
    public KnowledgeSourceStorePort testKnowledgeSourceStorePort() {
        return new KnowledgeSourceStorePort() {
            private final Map<KnowledgeSourceId, KnowledgeSource> store = new ConcurrentHashMap<>();

            @Override
            public KnowledgeSource save(KnowledgeSource source) {
                store.put(source.id(), source);
                return source;
            }

            @Override
            public Optional<KnowledgeSource> findById(KnowledgeSourceId sourceId) {
                return Optional.ofNullable(store.get(sourceId));
            }

            @Override
            public Optional<KnowledgeSource> findByProjectAndPath(ProjectId projectId, String sourcePath) {
                return store.values().stream()
                        .filter(s -> s.projectId().equals(projectId) && s.sourcePath().equals(sourcePath))
                        .findFirst();
            }

            @Override
            public List<KnowledgeSource> findByProjectAndOwner(ProjectId projectId, UserId ownerUserId) {
                return store.values().stream()
                        .filter(s -> s.belongsTo(projectId, ownerUserId))
                        .toList();
            }

            @Override
            public void deleteById(KnowledgeSourceId sourceId) {
                store.remove(sourceId);
            }

            @Override
            public void deleteByProject(ProjectId projectId, UserId ownerUserId) {
                store.entrySet().removeIf(e -> e.getValue().belongsTo(projectId, ownerUserId));
            }
        };
    }

    @Bean
    public VectorKnowledgePort testVectorKnowledgePort(KnowledgeSourceStorePort sourceStore) {
        return new VectorKnowledgePort() {
            private final List<KnowledgeChunk> chunks = new CopyOnWriteArrayList<>();

            @Override
            public void replaceSourceChunks(KnowledgeSourceId sourceId, List<KnowledgeChunk> newChunks) {
                chunks.removeIf(c -> c.sourceId().equals(sourceId));
                chunks.addAll(newChunks);
            }

            @Override
            public List<KnowledgeSearchMatch> searchSimilar(
                    ProjectId projectId,
                    UserId ownerUserId,
                    EmbeddingVector queryVector,
                    String embeddingVersion,
                    int topK,
                    double minScore
            ) {
                return chunks.stream()
                        .filter(c -> c.projectId().equals(projectId)
                                && c.ownerUserId().equals(ownerUserId)
                                && c.embeddingVersion().equals(embeddingVersion))
                        .map(c -> {
                            KnowledgeSource source = sourceStore.findById(c.sourceId()).orElse(null);
                            if (source == null) {
                                return null;
                            }
                            double score = c.embedding().cosineSimilarity(queryVector);
                            return new KnowledgeSearchMatch(
                                    c.sourceId(),
                                    source.sourcePath(),
                                    source.title(),
                                    c.chunkIndex(),
                                    c.content(),
                                    score,
                                    c.embeddingVersion()
                            );
                        })
                        .filter(m -> m != null && m.score() >= minScore)
                        .sorted(Comparator.comparingDouble(KnowledgeSearchMatch::score).reversed())
                        .limit(topK)
                        .toList();
            }

            @Override
            public void deleteBySource(KnowledgeSourceId sourceId) {
                chunks.removeIf(c -> c.sourceId().equals(sourceId));
            }

            @Override
            public void deleteByProject(ProjectId projectId, UserId ownerUserId) {
                chunks.removeIf(c -> c.projectId().equals(projectId) && c.ownerUserId().equals(ownerUserId));
            }
        };
    }

    @Bean
    public KnowledgeIndexJobStorePort testKnowledgeIndexJobStorePort() {
        return new KnowledgeIndexJobStorePort() {
            private final List<KnowledgeIndexJob> jobs = new CopyOnWriteArrayList<>();

            @Override
            public KnowledgeIndexJob save(KnowledgeIndexJob job) {
                jobs.removeIf(existing -> existing.id().equals(job.id()));
                jobs.add(job);
                return job;
            }

            @Override
            public List<KnowledgeIndexJob> findRecentByProjectAndOwner(
                    ProjectId projectId,
                    UserId ownerUserId,
                    int limit
            ) {
                return jobs.stream()
                        .filter(j -> j.projectId().equals(projectId) && j.ownerUserId().equals(ownerUserId))
                        .sorted(Comparator.comparing(KnowledgeIndexJob::createdAt).reversed())
                        .limit(limit)
                        .toList();
            }
        };
    }

    @Bean
    public AgentRunStorePort testAgentRunStorePort() {
        return new AgentRunStorePort() {
            private final Map<AgentRunId, AgentRun> store = new ConcurrentHashMap<>();

            @Override
            public AgentRun save(AgentRun run) {
                store.put(run.id(), run);
                return run;
            }

            @Override
            public Optional<AgentRun> findById(AgentRunId runId) {
                return Optional.ofNullable(store.get(runId));
            }

            @Override
            public List<AgentRun> findRecentByProjectAndOwner(ProjectId projectId, UserId ownerUserId, int limit) {
                return store.values().stream()
                        .filter(r -> r.belongsTo(projectId, ownerUserId))
                        .sorted(Comparator.comparing(AgentRun::createdAt).reversed())
                        .limit(limit)
                        .toList();
            }
        };
    }

    @Bean
    public AgentToolProposalStorePort testAgentToolProposalStorePort() {
        return new AgentToolProposalStorePort() {
            private final Map<ToolProposalId, AgentToolProposal> store = new ConcurrentHashMap<>();

            @Override
            public AgentToolProposal save(AgentToolProposal proposal) {
                store.put(proposal.id(), proposal);
                return proposal;
            }

            @Override
            public List<AgentToolProposal> saveAll(List<AgentToolProposal> proposals) {
                proposals.forEach(this::save);
                return proposals;
            }

            @Override
            public Optional<AgentToolProposal> findById(ToolProposalId proposalId) {
                return Optional.ofNullable(store.get(proposalId));
            }

            @Override
            public List<AgentToolProposal> findByRunOrderByStepAsc(AgentRunId runId) {
                return store.values().stream()
                        .filter(p -> p.runId().equals(runId))
                        .sorted(Comparator.comparingInt(AgentToolProposal::stepIndex))
                        .toList();
            }
        };
    }

    @Bean
    public DeviceStorePort testDeviceStorePort() {
        return new DeviceStorePort() {
            private final Map<DeviceId, Device> store = new ConcurrentHashMap<>();

            @Override
            public Device save(Device device) {
                store.put(device.id(), device);
                return device;
            }

            @Override
            public Optional<Device> findById(DeviceId deviceId) {
                return Optional.ofNullable(store.get(deviceId));
            }

            @Override
            public List<Device> findByOwner(UserId ownerUserId) {
                return store.values().stream().filter(d -> d.isOwnedBy(ownerUserId)).toList();
            }
        };
    }

    @Bean
    public PairingChallengeStorePort testPairingChallengeStorePort() {
        return new PairingChallengeStorePort() {
            private final Map<String, PairingChallenge> store = new ConcurrentHashMap<>();

            @Override
            public PairingChallenge save(PairingChallenge challenge) {
                store.put(challenge.codeHash(), challenge);
                return challenge;
            }

            @Override
            public Optional<PairingChallenge> findByCodeHash(String codeHash) {
                return Optional.ofNullable(store.get(codeHash));
            }
        };
    }

    @Bean
    public DeviceCredentialStorePort testDeviceCredentialStorePort() {
        return new DeviceCredentialStorePort() {
            private final Map<String, DeviceCredential> store = new ConcurrentHashMap<>();

            @Override
            public DeviceCredential save(DeviceCredential credential) {
                store.put(credential.tokenHash(), credential);
                return credential;
            }

            @Override
            public Optional<DeviceCredential> findByTokenHash(String tokenHash) {
                return Optional.ofNullable(store.get(tokenHash));
            }

            @Override
            public void revokeAllByDeviceId(DeviceId deviceId, Instant revokedAt) {
                store.replaceAll((k, v) -> v.deviceId().equals(deviceId) ? v.revoke(revokedAt) : v);
            }
        };
    }

    @Bean
    public RemoteTaskStorePort testRemoteTaskStorePort() {
        return new RemoteTaskStorePort() {
            private final Map<TaskId, RemoteTask> store = new ConcurrentHashMap<>();

            @Override
            public RemoteTask save(RemoteTask task) {
                store.put(task.id(), task);
                return task;
            }

            @Override
            public Optional<RemoteTask> findById(TaskId taskId) {
                return Optional.ofNullable(store.get(taskId));
            }

            @Override
            public Optional<RemoteTask> findByIdempotency(
                    UserId ownerUserId,
                    DeviceId deviceId,
                    String idempotencyKey
            ) {
                return store.values().stream()
                        .filter(t -> t.isOwnedBy(ownerUserId)
                                && t.isAssignedTo(deviceId)
                                && idempotencyKey.equals(t.idempotencyKey()))
                        .findFirst();
            }

            @Override
            public List<RemoteTask> findRecentByOwner(UserId ownerUserId, int limit) {
                return store.values().stream().filter(t -> t.isOwnedBy(ownerUserId)).limit(limit).toList();
            }

            @Override
            public Optional<RemoteTask> findNextLeasableForDevice(DeviceId deviceId, Instant now) {
                return store.values().stream()
                        .filter(t -> t.isAssignedTo(deviceId) && t.isLeasableAt(now))
                        .sorted(Comparator.comparing(RemoteTask::createdAt))
                        .findFirst();
            }
        };
    }

    @Bean
    public TaskEventStorePort testTaskEventStorePort() {
        return new TaskEventStorePort() {
            private final List<TaskEvent> events = new CopyOnWriteArrayList<>();

            @Override
            public Optional<TaskEvent> findByTaskAndEventKey(TaskId taskId, String eventKey) {
                return events.stream()
                        .filter(e -> e.taskId().equals(taskId) && e.eventKey().equals(eventKey))
                        .findFirst();
            }

            @Override
            public TaskEvent save(TaskEvent event) {
                if (findByTaskAndEventKey(event.taskId(), event.eventKey()).isEmpty()) {
                    events.add(event);
                }
                return event;
            }

            @Override
            public List<TaskEvent> findByTaskOrderByCreatedAsc(TaskId taskId) {
                return events.stream().filter(e -> e.taskId().equals(taskId)).toList();
            }
        };
    }

    @Bean
    public ToolApprovalStorePort testToolApprovalStorePort() {
        return new ToolApprovalStorePort() {
            private final Map<ToolApprovalId, ToolApproval> store = new ConcurrentHashMap<>();

            @Override
            public ToolApproval save(ToolApproval approval) {
                store.put(approval.id(), approval);
                return approval;
            }

            @Override
            public Optional<ToolApproval> findById(ToolApprovalId approvalId) {
                return Optional.ofNullable(store.get(approvalId));
            }

            @Override
            public List<ToolApproval> findByTaskOrderByCreatedAsc(TaskId taskId) {
                return store.values().stream().filter(a -> a.taskId().equals(taskId)).toList();
            }
        };
    }
}
