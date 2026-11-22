package io.github.jvpgjava.creatingj.identity.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.apidocs.v1.AdminUserApiDocs;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.CreateManagedUserRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.CreatedManagedUserResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.UserSummaryResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.UsersPageResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.mapper.v1.IdentityHttpMapper;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.security.AuthenticatedUser;
import io.github.jvpgjava.creatingj.identity.application.port.in.CreateManagedUserUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.ListUsersUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.SuspendUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public final class AdminUserController implements AdminUserApiDocs {

    private final CreateManagedUserUseCase createManagedUser;
    private final ListUsersUseCase listUsers;
    private final SuspendUserUseCase suspendUser;
    private final IdentityHttpMapper mapper;

    public AdminUserController(
            CreateManagedUserUseCase createManagedUser,
            ListUsersUseCase listUsers,
            SuspendUserUseCase suspendUser,
            IdentityHttpMapper mapper
    ) {
        this.createManagedUser = createManagedUser;
        this.listUsers = listUsers;
        this.suspendUser = suspendUser;
        this.mapper = mapper;
    }

    @Override
    @PostMapping
    public ResponseEntity<CreatedManagedUserResponse> createUser(
            @Valid @RequestBody CreateManagedUserRequest request,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = createManagedUser.execute(mapper.toCommand(request, actor.id()));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toCreatedResponse(result));
    }

    @Override
    @GetMapping
    public ResponseEntity<UsersPageResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "created_at") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = listUsers.execute(mapper.toListQuery(actor.id(), page, size, status, sort, direction));
        return ResponseEntity.ok(mapper.toPageResponse(result));
    }

    @Override
    @PostMapping("/{userId}/suspend")
    public ResponseEntity<UserSummaryResponse> suspendUser(
            @PathVariable String userId,
            @AuthenticationPrincipal AuthenticatedUser actor
    ) {
        var result = suspendUser.execute(mapper.toSuspendCommand(actor.id(), userId));
        return ResponseEntity.ok(mapper.toSummary(result));
    }
}
