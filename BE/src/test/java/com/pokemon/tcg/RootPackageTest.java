package com.pokemon.tcg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RootPackageTest {

    @Test
    void testRetreat_main_runsWithoutException() {
        assertDoesNotThrow(() -> TestRetreat.main(new String[]{}));
    }
}
