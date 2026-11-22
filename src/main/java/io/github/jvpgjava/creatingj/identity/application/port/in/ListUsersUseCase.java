package io.github.jvpgjava.creatingj.identity.application.port.in;

import io.github.jvpgjava.creatingj.identity.domain.AccountStatus;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.List;

public interface ListUsersUseCase {

    UsersPage execute(ListUsersQuery query);

    enum UserSortField {
        CREATED_AT,
        EMAIL,
        DISPLAY_NAME,
        STATUS
    }

    record ListUsersQuery(
            UserId actorUserId,
            int page,
            int size,
            AccountStatus status,
            UserSortField sortBy,
            boolean ascending
    ) {
    }

    record UsersPage(
            List<User> items,
            int page,
            int size,
            long totalElements
    ) {
    }
}
