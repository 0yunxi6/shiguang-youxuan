package com.ecommerce.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() throws Exception {
        jwtTokenProvider = new JwtTokenProvider();
        // Use reflection to set the private fields
        var secretField = JwtTokenProvider.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(jwtTokenProvider, "test-secret-key-for-unit-testing-must-be-at-least-sixty-four-bytes-long");

        var expirationField = JwtTokenProvider.class.getDeclaredField("jwtExpiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtTokenProvider, 86400000L);
    }

    @Test
    void generateToken_and_extractUsername() {
        String token = jwtTokenProvider.generateToken("testuser");

        assertNotNull(token);
        assertFalse(token.isEmpty());

        String username = jwtTokenProvider.getUsernameFromToken(token);
        assertEquals("testuser", username);
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtTokenProvider.generateToken("testuser");

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }
}
