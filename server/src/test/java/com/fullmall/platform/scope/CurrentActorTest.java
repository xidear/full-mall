package com.fullmall.platform.scope;

import com.fullmall.platform.error.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentActorTest {

    private final CurrentActor currentActor = new CurrentActor();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void failsWhenNobodyIsAuthenticatedInsteadOfUsingFakeIdentity() {
        assertThatThrownBy(currentActor::requireUsername)
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("没有认证");
    }

    @Test
    void prefersActorDetailsUsername() {
        var authentication = new UsernamePasswordAuthenticationToken(
                "token-name", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        authentication.setDetails(new ActorDetails(UUID.randomUUID(), "xidear", "ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(currentActor.requireUsername()).isEqualTo("xidear");
    }

    @Test
    void fallsBackToAuthenticationNameWhenDetailsMissing() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ops", null,
                        List.of(new SimpleGrantedAuthority("ROLE_OPERATOR"))));

        assertThat(currentActor.requireUsername()).isEqualTo("ops");
    }

    @Test
    void rejectsAnonymousPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of()));

        assertThatThrownBy(currentActor::requireUsername)
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("没有用户名");
    }
}