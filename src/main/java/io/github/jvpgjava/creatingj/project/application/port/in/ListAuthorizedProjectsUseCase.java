package io.github.jvpgjava.creatingj.project.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.UserId;
import io.github.jvpgjava.creatingj.project.domain.Project;

import java.util.List;

public interface ListAuthorizedProjectsUseCase {

    Result execute(Query query);

    record Query(UserId actorUserId, int page, int size) {
    }

    record Result(List<Project> items, int page, int size, long totalElements) {
    }
}
