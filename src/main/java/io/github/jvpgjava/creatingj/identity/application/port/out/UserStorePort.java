package io.github.jvpgjava.creatingj.identity.application.port.out;

import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase.UserSortField;
import io.github.jvpgjava.creatingj.identity.domain.AccountStatus;
import io.github.jvpgjava.creatingj.identity.domain.Email;
import io.github.jvpgjava.creatingj.identity.domain.User;
import io.github.jvpgjava.creatingj.identity.domain.UserId;

import java.util.List;
import java.util.Optional;

public interface UserStorePort {

    User save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    List<User> findPage(int page, int size, AccountStatus status, UserSortField sortBy, boolean ascending);

    long count(AccountStatus status);
}
