package com.fullmall.platform.export;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FieldMaskerTest {

    @Test
    void masksPhoneKeepingPrefixAndSuffix() {
        assertThat(FieldMasker.phone("13800000001")).isEqualTo("138****0001");
        assertThat(FieldMasker.phone("8613800000001")).isEqualTo("861******0001");
        assertThat(FieldMasker.phone(null)).isNull();
    }

    @Test
    void maskTooShortPhoneInsteadOfLeakingIt() {
        assertThat(FieldMasker.phone("12345")).isEqualTo("*****");
    }

    @Test
    void masksEmailKeepingFirstCharAndDomain() {
        assertThat(FieldMasker.email("zhangsan@example.com")).isEqualTo("z***@example.com");
        assertThat(FieldMasker.email(null)).isNull();
    }

    @Test
    void masksMalformedEmailEntirely() {
        assertThat(FieldMasker.email("not-an-email")).isEqualTo("************");
        assertThat(FieldMasker.email("@example.com")).isEqualTo("************");
    }

    @Test
    void masksIdCardKeepingHeadAndTail() {
        assertThat(FieldMasker.idCard("110101199001011234")).isEqualTo("110101********1234");
        assertThat(FieldMasker.idCard("12345")).isEqualTo("*****");
    }

    @Test
    void masksNameKeepingFirstChar() {
        assertThat(FieldMasker.name("张三")).isEqualTo("张*");
        assertThat(FieldMasker.name("欧阳修")).isEqualTo("欧**");
        assertThat(FieldMasker.name(null)).isNull();
    }
}