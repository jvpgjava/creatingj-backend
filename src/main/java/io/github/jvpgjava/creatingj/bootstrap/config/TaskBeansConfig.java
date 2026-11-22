package io.github.jvpgjava.creatingj.bootstrap.config;

import io.github.jvpgjava.creatingj.audit.application.port.out.AuditPort;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.device.application.port.out.DeviceStorePort;
import io.github.jvpgjava.creatingj.project.application.port.out.ProjectStorePort;
import io.github.jvpgjava.creatingj.task.adapter.in.web.mapper.v1.TaskHttpMapper;
import io.github.jvpgjava.creatingj.task.adapter.in.web.streaming.SseTaskProgressHub;
import io.github.jvpgjava.creatingj.task.application.port.in.ExecuteRunnerTaskProtocolUseCase;
import io.github.jvpgjava.creatingj.task.application.port.in.ManageUserRemoteTasksUseCase;
import io.github.jvpgjava.creatingj.task.application.port.out.RemoteTaskStorePort;
import io.github.jvpgjava.creatingj.task.application.port.out.TaskEventStorePort;
import io.github.jvpgjava.creatingj.task.application.port.out.TaskProgressPublisherPort;
import io.github.jvpgjava.creatingj.task.application.port.out.ToolApprovalStorePort;
import io.github.jvpgjava.creatingj.task.application.usecase.ExecuteRunnerTaskProtocolInteractor;
import io.github.jvpgjava.creatingj.task.application.usecase.ManageUserRemoteTasksInteractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaskBeansConfig {

    @Bean
    public TaskHttpMapper taskHttpMapper() {
        return new TaskHttpMapper();
    }

    @Bean
    public SseTaskProgressHub sseTaskProgressHub(TaskHttpMapper mapper) {
        return new SseTaskProgressHub(mapper);
    }

    @Bean
    public ManageUserRemoteTasksUseCase manageUserRemoteTasksUseCase(
            DeviceStorePort deviceStore,
            ProjectStorePort projectStore,
            RemoteTaskStorePort taskStore,
            TaskEventStorePort eventStore,
            ToolApprovalStorePort approvalStore,
            TaskProgressPublisherPort progressPublisher,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new ManageUserRemoteTasksInteractor(
                deviceStore, projectStore, taskStore, eventStore, approvalStore, progressPublisher, auditPort, clock
        );
    }

    @Bean
    public ExecuteRunnerTaskProtocolUseCase executeRunnerTaskProtocolUseCase(
            RemoteTaskStorePort taskStore,
            TaskEventStorePort eventStore,
            ToolApprovalStorePort approvalStore,
            TaskProgressPublisherPort progressPublisher,
            AuditPort auditPort,
            ClockPort clock
    ) {
        return new ExecuteRunnerTaskProtocolInteractor(
                taskStore, eventStore, approvalStore, progressPublisher, auditPort, clock
        );
    }
}
