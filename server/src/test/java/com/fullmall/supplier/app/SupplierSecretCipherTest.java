package com.fullmall.supplier.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SupplierSecretCipherTest {

    @Test
    void encryptsAndDecryptsSupplierSecret() {
        SupplierSecretCipher cipher = new SupplierSecretCipher("", "dev");
        String encrypted = cipher.encrypt("sec_test_secret");
        assertNotEquals("sec_test_secret", encrypted);
        assertEquals("sec_test_secret", cipher.decrypt(encrypted));
    }

    @Test
    void rejectsTamperedCiphertext() {
        SupplierSecretCipher cipher = new SupplierSecretCipher("", "dev");
        String encrypted = cipher.encrypt("sec_test_secret");
        String tampered = encrypted.substring(0, encrypted.length() - 2) + "AA";
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(tampered));
    }

    @Test
    void productionRequiresConfiguredKey() {
        assertThrows(IllegalStateException.class, () -> new SupplierSecretCipher("", "prod"));
    }
}
