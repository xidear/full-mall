package com.fullmall.logistics;

import com.fullmall.logistics.app.Kuaidi100Query;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 快递100 请求构造：签名确定性 + 参数校验（无 AK 时也能验证的部分）。 */
class Kuaidi100QueryTest {

    @Test
    void signIsDeterministicUppercaseMd5() {
        var form = Kuaidi100Query.build("cust-1", "key-1", "SF", "SF123");

        assertThat(form.customer()).isEqualTo("cust-1");
        assertThat(form.param()).contains("\"com\":\"sf\"").contains("\"num\":\"SF123\"");
        assertThat(form.sign()).hasSize(32).isUpperCase();
        // 同一输入必须得到同一签名
        assertThat(Kuaidi100Query.build("cust-1", "key-1", "SF", "SF123").sign())
                .isEqualTo(form.sign());
    }

    @Test
    void differentInputsProduceDifferentSignatures() {
        String a = Kuaidi100Query.build("c", "k", "SF", "N1").sign();
        String b = Kuaidi100Query.build("c", "k", "SF", "N2").sign();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void requiresAllInputs() {
        assertThatThrownBy(() -> Kuaidi100Query.build("", "k", "SF", "N1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("customer");
        assertThatThrownBy(() -> Kuaidi100Query.build("c", " ", "SF", "N1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("key");
        assertThatThrownBy(() -> Kuaidi100Query.build("c", "k", "", "N1"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("承运商");
        assertThatThrownBy(() -> Kuaidi100Query.build("c", "k", "SF", ""))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("运单号");
    }
}
