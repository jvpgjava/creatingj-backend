package io.github.jvpgjava.creatingj.bootstrap.config;

import io.github.jvpgjava.creatingj.common.application.port.in.GetSystemStatusUseCase;
import io.github.jvpgjava.creatingj.common.application.port.out.ClockPort;
import io.github.jvpgjava.creatingj.common.application.usecase.GetSystemStatusInteractor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonBeansConfig {

    @Bean
    public GetSystemStatusUseCase getSystemStatusUseCase(ClockPort clockPort) {
        return new GetSystemStatusInteractor(clockPort);
    }
}
