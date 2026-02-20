package com.ardley.cedar.jaxrs;

import com.ardley.cedar.core.CedarSchemaGenerator;
import com.ardley.cedar.core.ResourceEntity;
import com.ardley.cedar.core.ResourceExtractionContext;
import com.ardley.cedar.core.ResourceExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ResourceExtractorRegistryTest {

    static class TestUserExtractor implements ResourceExtractor<String> {
        @Override
        public String getResourceType() {
            return "User";
        }

        @Override
        public ResourceEntity extract(String input, ResourceExtractionContext context) {
            return ResourceEntity.builder()
                .entityType("User")
                .entityId(input)
                .customerId("customer-123")
                .build();
        }
    }

    static class TestDocumentExtractor implements ResourceExtractor<String> {
        @Override
        public String getResourceType() {
            return "Document";
        }

        @Override
        public ResourceEntity extract(String input, ResourceExtractionContext context) {
            return ResourceEntity.builder()
                .entityType("Document")
                .entityId(input)
                .customerId("customer-123")
                .build();
        }

        @Override
        public Map<String, AttributeType> getAttributeSchema() {
            Map<String, AttributeType> schema = new HashMap<>();
            schema.put("title", AttributeType.STRING);
            schema.put("status", AttributeType.STRING);
            return schema;
        }
    }

    @Test
    void builder_shouldRegisterExtractors() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new TestUserExtractor())
            .register(new TestDocumentExtractor())
            .build();

        assertTrue(registry.hasExtractor("User"));
        assertTrue(registry.hasExtractor("Document"));
        assertFalse(registry.hasExtractor("NonExistent"));
    }

    @Test
    void getExtractor_shouldReturnRegisteredExtractor() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new TestUserExtractor())
            .build();

        ResourceExtractor<?> extractor = registry.getExtractor("User");
        assertNotNull(extractor);
        assertEquals("User", extractor.getResourceType());
    }

    @Test
    void getExtractor_shouldThrowExceptionForUnregisteredType() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new TestUserExtractor())
            .build();

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> registry.getExtractor("NonExistent")
        );

        assertTrue(exception.getMessage().contains("No ResourceExtractor registered"));
        assertTrue(exception.getMessage().contains("NonExistent"));
    }

    @Test
    void builder_shouldThrowExceptionForDuplicateType() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> ResourceExtractorRegistry.builder()
                .register(new TestUserExtractor())
                .register(new TestUserExtractor())
                .build()
        );

        assertTrue(exception.getMessage().contains("already registered"));
    }

    @Test
    void getRegisteredTypes_shouldReturnAllTypes() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new TestUserExtractor())
            .register(new TestDocumentExtractor())
            .build();

        assertEquals(2, registry.getRegisteredTypes().size());
        assertTrue(registry.getRegisteredTypes().contains("User"));
        assertTrue(registry.getRegisteredTypes().contains("Document"));
    }

    @Test
    void registry_shouldBeImmutable() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new TestUserExtractor())
            .build();

        // Attempt to modify returned set should not affect registry
        assertThrows(
            UnsupportedOperationException.class,
            () -> registry.getRegisteredTypes().add("NewType")
        );
    }

    @Test
    void generateSchema_shouldReturnValidCedarSchema() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new TestUserExtractor())
            .register(new TestDocumentExtractor())
            .build();

        String schema = registry.generateSchema(
            "TestNamespace",
            Set.of("ViewUser", "ViewDocument"),
            "User"
        );

        assertNotNull(schema);
        assertTrue(schema.contains("TestNamespace"));
        assertTrue(schema.contains("User"));
        assertTrue(schema.contains("Document"));
        assertTrue(schema.contains("ViewUser"));
        assertTrue(schema.contains("ViewDocument"));
    }

    @Test
    void generateSchema_shouldIncludeExtractorSchemas() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new TestDocumentExtractor())
            .build();

        String schema = registry.generateSchema(
            "TestApp",
            Set.of("ViewDocument"),
            "User"
        );

        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        JsonNode docShape = root.get("TestApp").get("entityTypes").get("Document").get("shape");
        JsonNode attributes = docShape.get("attributes");

        assertNotNull(attributes);
        assertTrue(attributes.has("title"));
        assertTrue(attributes.has("status"));
    }

    @Test
    void getExtractors_shouldReturnImmutableMap() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new TestUserExtractor())
            .build();

        Map<String, ResourceExtractor<?>> extractors = registry.getExtractors();
        assertNotNull(extractors);
        assertEquals(1, extractors.size());

        // Attempt to modify should throw
        assertThrows(
            UnsupportedOperationException.class,
            () -> extractors.put("NewType", new TestUserExtractor())
        );
    }
}
