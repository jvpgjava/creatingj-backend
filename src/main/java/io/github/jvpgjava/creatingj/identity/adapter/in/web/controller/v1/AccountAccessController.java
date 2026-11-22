package io.github.jvpgjava.creatingj.identity.adapter.in.web.controller.v1;

import io.github.jvpgjava.creatingj.identity.adapter.in.web.apidocs.v1.AccountAccessApiDocs;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.ActivateAccountRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.ConfirmPasswordResetRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.LoginRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.LoginResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PasswordResetRequest;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.dto.v1.PasswordResetRequestedResponse;
import io.github.jvpgjava.creatingj.identity.adapter.in.web.mapper.v1.IdentityHttpMapper;
import io.github.jvpgjava.creatingj.identity.application.port.in.ActivateAccountUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.AuthenticateWithPasswordUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.ConfirmPasswordResetUseCase;
import io.github.jvpgjava.creatingj.identity.application.port.in.RequestPasswordResetUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public final class AccountAccessController implements AccountAccessApiDocs {

    private static final String RESET_ACCEPTED_MESSAGE =
            "Se o email existir e estiver ativo, as instrucoes de redefinicao serao processadas.";

    private final ActivateAccountUseCase activateAccount;
    private final AuthenticateWithPasswordUseCase authenticateWithPassword;
    private final RequestPasswordResetUseCase requestPasswordReset;
    private final ConfirmPasswordResetUseCase confirmPasswordReset;
    private final IdentityHttpMapper mapper;

    public AccountAccessController(
            ActivateAccountUseCase activateAccount,
            AuthenticateWithPasswordUseCase authenticateWithPassword,
            RequestPasswordResetUseCase requestPasswordReset,
            ConfirmPasswordResetUseCase confirmPasswordReset,
            IdentityHttpMapper mapper
    ) {
        this.activateAccount = activateAccount;
        this.authenticateWithPassword = authenticateWithPassword;
        this.requestPasswordReset = requestPasswordReset;
        this.confirmPasswordReset = confirmPasswordReset;
        this.mapper = mapper;
    }

    @Override
    @PostMapping("/activate")
    public ResponseEntity<Void> activateAccount(@Valid @RequestBody ActivateAccountRequest request) {
        activateAccount.execute(mapper.toActivateCommand(request));
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var session = authenticateWithPassword.execute(mapper.toLoginCommand(request));
        return ResponseEntity.ok(mapper.toLoginResponse(session));
    }

    @Override
    @PostMapping("/password-reset/request")
    public ResponseEntity<PasswordResetRequestedResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        requestPasswordReset.execute(mapper.toResetRequestCommand(request));
        return ResponseEntity.accepted().body(new PasswordResetRequestedResponse(RESET_ACCEPTED_MESSAGE));
    }

    @Override
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody ConfirmPasswordResetRequest request) {
        confirmPasswordReset.execute(mapper.toConfirmResetCommand(request));
        return ResponseEntity.noContent().build();
    }
}
