package com.fullmall;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithStructureTest {

    @Test
    void verifiesModuleDependencies() {
        ApplicationModules.of(FullMallApplication.class).verify();
    }
}