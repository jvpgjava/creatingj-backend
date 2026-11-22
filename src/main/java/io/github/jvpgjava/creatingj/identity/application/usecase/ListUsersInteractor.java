package io.github.jvpgjava.creatingj.identity.application.usecase;

import io.github.jvpgjava.creatingj.common.domain.DomainException;
import io.github.jvpgjava.creatingj.common.domain.ErrorCode;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.out.UserStorePort;
import io.github.jvpgjava.creatingj.identity.domain.User;

import java.util.List;

public final class ListUsersInteractor implements ListUsersUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserStorePort userStore;

    public ListUsersInteractor(UserStorePort userStore) {
        this.userStore = userStore;
    }

    @Override
    public UsersPage execute(ListUsersQuery query) {
        AdminAccessGuard.requireActiveAdmin(userStore, query.actorUserId());
        if (query.page() < 0) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Parametro page deve ser maior ou igual a zero.");
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new DomainException(ErrorCode.VALIDATION_ERROR, "Parametro size deve estar entre 1 e 100.");
        }
        UserSortField sortBy = query.sortBy() == null ? UserSortField.CREATED_AT : query.sortBy();
        List<User> items = userStore.findPage(query.page(), query.size(), query.status(), sortBy, query.ascending());
        long total = userStore.count(query.status());
        return new UsersPage(items, query.page(), query.size(), total);
    }
}
