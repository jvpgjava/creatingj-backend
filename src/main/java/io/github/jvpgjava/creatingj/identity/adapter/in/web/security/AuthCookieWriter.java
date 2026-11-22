package io.github.jvpgjava.creatingj.identity.adapter.in.web.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Component
public final class AuthCookieWriter {

    public static final String XSRF_COOKIE_NAME = "XSRF-TOKEN";

    private static final int CSRF_TOKEN_BYTES = 24;

    private final SecureRandom secureRandom = new SecureRandom();
    private final boolean secureCookie;

    public AuthCookieWriter(@Value("${creatingj.security.cookie.secure:true}") boolean secureCookie) {
        this.secureCookie = secureCookie;
    }

    public HttpHeaders buildSessionHeaders(String rawSessionToken, Duration maxAge) {
        ResponseCookie sessionCookie = ResponseCookie.from(
                        SessionAuthenticationFilter.SESSION_COOKIE_NAME, rawSessionToken)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/v1")
                .maxAge(maxAge)
                .build();

        ResponseCookie xsrfCookie = ResponseCookie.from(XSRF_COOKIE_NAME, generateCsrfToken())
                .httpOnly(false)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, sessionCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, xsrfCookie.toString());
        return headers;
    }

    public HttpHeaders buildClearCookieHeaders() {
        ResponseCookie clearSession = ResponseCookie.from(SessionAuthenticationFilter.SESSION_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/v1")
                .maxAge(Duration.ZERO)
                .build();

        ResponseCookie clearXsrf = ResponseCookie.from(XSRF_COOKIE_NAME, "")
                .httpOnly(false)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, clearSession.toString());
        headers.add(HttpHeaders.SET_COOKIE, clearXsrf.toString());
        return headers;
    }

    private String generateCsrfToken() {
        byte[] bytes = new byte[CSRF_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
