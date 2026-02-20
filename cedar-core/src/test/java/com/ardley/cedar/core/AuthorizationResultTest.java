package com.ardley.cedar.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationResultTest {

    @Test
    void allow_shouldCreateAllowResult() {
        AuthorizationResult result = AuthorizationResult.allow(List.of("policy-1"));

        assertEquals(AuthorizationResult.Decision.ALLOW, result.getDecision());
        assertTrue(result.isAllowed());
        assertFalse(result.isDenied());
        assertEquals(List.of("policy-1"), result.getDeterminingPolicies());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void deny_shouldCreateDenyResult() {
        AuthorizationResult result = AuthorizationResult.deny(
            List.of("policy-2"),
            List.of("Cross-tenant access denied")
        );

        assertEquals(AuthorizationResult.Decision.DENY, result.getDecision());
        assertFalse(result.isAllowed());
        assertTrue(result.isDenied());
        assertEquals(List.of("policy-2"), result.getDeterminingPolicies());
        assertEquals(List.of("Cross-tenant access denied"), result.getErrors());
    }
}
