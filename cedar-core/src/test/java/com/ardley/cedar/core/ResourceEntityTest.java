package com.ardley.cedar.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResourceEntityTest {

    @Test
    void builder_shouldCreateResourceWithAllFields() {
        ResourceEntity resource = ResourceEntity.builder()
            .entityType("LoanApplication")
            .entityId("loan-123")
            .customerId("customer-456")
            .attributes(Map.of("status", "Active", "amount", 100000))
            .build();

        assertEquals("LoanApplication", resource.getEntityType());
        assertEquals("loan-123", resource.getEntityId());
        assertEquals("customer-456", resource.getCustomerId());
        assertEquals(Map.of("status", "Active", "amount", 100000), resource.getAttributes());
    }

    @Test
    void hasAttribute_shouldReturnTrueForExistingAttribute() {
        ResourceEntity resource = ResourceEntity.builder()
            .entityType("User")
            .entityId("user-123")
            .customerId("customer-456")
            .attributes(Map.of("email", "test@example.com"))
            .build();

        assertTrue(resource.hasAttribute("email"));
        assertFalse(resource.hasAttribute("phone"));
    }

    @Test
    void builder_shouldUseEmptyMapForAttributesIfNotSet() {
        ResourceEntity resource = ResourceEntity.builder()
            .entityType("User")
            .entityId("user-123")
            .customerId("customer-456")
            .build();

        assertNotNull(resource.getAttributes());
        assertTrue(resource.getAttributes().isEmpty());
    }
}
