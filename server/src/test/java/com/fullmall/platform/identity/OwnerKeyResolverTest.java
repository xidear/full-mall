package com.fullmall.platform.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnerKeyResolverTest {

    private final OwnerKeyResolver resolver = new OwnerKeyResolver();

    private static UsernamePasswordAuthenticationToken loggedIn(String name) {
        return new UsernamePasswordAuthenticationToken(name, null, List.of());
    }

    @Test
    void authenticatedUserMapsToMemberKey() {
        assertThat(resolver.ownerKey(loggedIn("13800000001"), null)).isEqualTo("member:13800000001");
    }

    @Test
    void guestRequiresToken() {
        assertThatThrownBy(() -> resolver.ownerKey(null, null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("未登录");
        assertThatThrownBy(() -> resolver.ownerKey(null, "  "))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class);
    }

    @Test
    void guestTokenIsValidated() {
        assertThat(resolver.ownerKey(null, "abc-123_X")).isEqualTo("guest:abc-123_X");
        assertThatThrownBy(() -> resolver.ownerKey(null, "bad token!"))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("只允许");
        assertThatThrownBy(() -> resolver.ownerKey(null, "x".repeat(129)))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("过长");
    }

    @Test
    void anonymousPrincipalIsTreatedAsGuest() {
        var anonymous = new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of());
        assertThat(resolver.ownerKey(anonymous, "tok")).isEqualTo("guest:tok");
    }

    @Test
    void mergeRequiresMemberIdentity() {
        assertThatThrownBy(() -> resolver.memberKey(null))
                .isInstanceOf(com.fullmall.platform.error.ApiException.class)
                .hasMessageContaining("缺少登录身份");
    }
}
