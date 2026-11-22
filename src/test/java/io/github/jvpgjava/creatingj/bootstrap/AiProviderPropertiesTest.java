package io.github.jvpgjava.creatingj.bootstrap;

import io.github.jvpgjava.creatingj.bootstrap.config.provider.AiProviderProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiProviderPropertiesTest {

    @Test
    void shouldAcceptValidHttpsPublicEndpointAndRedactApiKeyInToString() {
        var props = new AiProviderProperties(
                "https://api.trystac.com/v1",
                "crj-super-secret-value-12345",
                "z-ai/glm-5.3-flash",
                "",
                true,
                true
        );

        assertThatCode(props::validateSecurityPolicy).doesNotThrowAnyException();
        assertThat(props.toString())
                .doesNotContain("crj-super-secret-value-12345")
                .contains("[REDACTED]");
    }

    @Test
    void shouldRequireApiKeyWhenPolicyEnforcesIt() {
        var props = new AiProviderProperties(
                "https://api.trystac.com/v1",
                "   ",
                "z-ai/glm-5.3-flash",
                "",
                true,
                true
        );

        assertThatThrownBy(props::validateSecurityPolicy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CRJ_API_KEY");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://api.trystac.com/v1",
            "https://localhost/v1",
            "https://127.0.0.1/v1",
            "https://169.254.169.254/latest/meta-data",
            "https://10.0.0.1/v1",
            "https://192.168.1.10/v1",
            "https://172.16.0.5/v1",
            "https://[::1]/v1",
            "ftp://api.trystac.com/v1"
    })
    void shouldRejectUnsafeOrInternalUrlsWhenStrictValidationIsEnabled(String unsafeUrl) {
        var props = new AiProviderProperties(
                unsafeUrl,
                "dummy-key",
                "z-ai/glm-5.3-flash",
                "",
                false,
                true
        );

        assertThatThrownBy(props::validateSecurityPolicy)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldAllowLocalMockUrlOnlyWhenStrictValidationIsDisabledForTests() {
        var props = new AiProviderProperties(
                "http://127.0.0.1:18089/v1",
                "",
                "z-ai/glm-5.3-flash",
                "",
                false,
                false
        );

        assertThatCode(props::validateSecurityPolicy).doesNotThrowAnyException();
    }
}
