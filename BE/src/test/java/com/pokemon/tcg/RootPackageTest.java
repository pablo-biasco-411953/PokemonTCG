package com.pokemon.tcg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RootPackageTest {

    @Test
    void testRetreat_main_runsWithoutException() {
        assertDoesNotThrow(() -> TestRetreat.main(new String[]{}));
    }

    @Test
    void backendApplication_constructorInstantiates() {
        BackendApplication app = new BackendApplication();
        assertNotNull(app);
    }
}
