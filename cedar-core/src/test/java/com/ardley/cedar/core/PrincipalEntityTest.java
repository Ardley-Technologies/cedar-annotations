package com.ardley.cedar.core;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PrincipalEntityTest {

    @Test
    void builder_shouldCreatePrincipalWithAllFields() {
        PrincipalEntity principal = PrincipalEntity.builder()
            .userId("user-123")
            .customerId("customer-456")
            .role("admin")
            .email("test@example.com")
            .groups(Set.of("group1", "group2"))
            .attributes(Map.of("department", "Engineering"))
            .build();

        assertEquals("user-123", principal.getUserId());
        assertEquals("customer-456", principal.getCustomerId());
        assertEquals("admin", principal.getRole());
        assertEquals("test@example.com", principal.getEmail());
        assertEquals(Set.of("group1", "group2"), principal.getGroups());
        assertEquals(Map.of("department", "Engineering"), principal.getAttributes());
    }

    @Test
    void builder_shouldUseDefaultsForOptionalFields() {
        PrincipalEntity principal = PrincipalEntity.builder()
            .userId("user-123")
            .customerId("customer-456")
            .build();

        assertNotNull(principal.getGroups());
        assertTrue(principal.getGroups().isEmpty());
        assertNotNull(principal.getAttributes());
        assertTrue(principal.getAttributes().isEmpty());
    }
}
