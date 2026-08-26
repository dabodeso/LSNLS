package com.lsnls.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompatiblePasswordEncoderTest {

    private final CompatiblePasswordEncoder encoder = new CompatiblePasswordEncoder();

    @Test
    void planoAntiguoSigueValiendoYPideRehash() {
        assertTrue(encoder.matches("capote", "capote"));
        assertTrue(encoder.upgradeEncoding("capote"));
        assertFalse(encoder.matches("otra", "capote"));
    }

    @Test
    void bcryptNuevoNoPideRehash() {
        String hash = encoder.encode("capote");
        assertTrue(encoder.matches("capote", hash));
        assertFalse(encoder.upgradeEncoding(hash));
        assertFalse(encoder.matches("otra", hash));
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }
}
