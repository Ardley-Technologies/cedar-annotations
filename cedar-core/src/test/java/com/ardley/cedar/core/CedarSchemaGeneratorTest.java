package com.ardley.cedar.core;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CedarSchemaGeneratorTest {

    @Test
    void generateSchema_shouldCreateValidCedarSchema() {
        // Create mock extractors
        Map<String, ResourceExtractor<?>> extractors = new HashMap<>();
        extractors.put("User", new UserExtractor());
        extractors.put("Document", new DocumentExtractor());

        CedarSchemaGenerator generator = new CedarSchemaGenerator("MyApp");
        String schema = generator.generateSchema(
            extractors,
            Set.of("ViewUser", "CreateUser", "ViewDocument", "DeleteDocument"),
            "User"
        );

        assertNotNull(schema);
        assertTrue(schema.contains("MyApp"));
        assertTrue(schema.contains("User"));
        assertTrue(schema.contains("Document"));
        assertTrue(schema.contains("ViewUser"));
        assertTrue(schema.contains("CreateUser"));

        // Verify it's valid JSON
        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        assertNotNull(root);
        assertTrue(root.has("MyApp"));
    }

    @Test
    void generateSchema_shouldIncludeEntityTypes() {
        Map<String, ResourceExtractor<?>> extractors = new HashMap<>();
        extractors.put("User", new UserExtractor());

        CedarSchemaGenerator generator = new CedarSchemaGenerator("TestApp");
        String schema = generator.generateSchema(extractors, Set.of("ViewUser"), "User");

        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        JsonNode entityTypes = root.get("TestApp").get("entityTypes");

        assertNotNull(entityTypes);
        assertTrue(entityTypes.has("User"));
    }

    @Test
    void generateSchema_shouldIncludeAttributeSchemas() {
        Map<String, ResourceExtractor<?>> extractors = new HashMap<>();
        extractors.put("User", new UserExtractor());

        CedarSchemaGenerator generator = new CedarSchemaGenerator("TestApp");
        String schema = generator.generateSchema(extractors, Set.of("ViewUser"), "User");

        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        JsonNode userShape = root.get("TestApp").get("entityTypes").get("User").get("shape");
        JsonNode attributes = userShape.get("attributes");

        assertNotNull(attributes);
        assertTrue(attributes.has("email"));
        assertTrue(attributes.has("role"));
        assertTrue(attributes.has("active"));

        assertEquals("String", attributes.get("email").get("type").asText());
        assertEquals("String", attributes.get("role").get("type").asText());
        assertEquals("Boolean", attributes.get("active").get("type").asText());
    }

    @Test
    void generateSchema_shouldHandleSetTypes() {
        Map<String, ResourceExtractor<?>> extractors = new HashMap<>();
        extractors.put("Document", new DocumentExtractor());

        CedarSchemaGenerator generator = new CedarSchemaGenerator("TestApp");
        String schema = generator.generateSchema(extractors, Set.of("ViewDocument"), "User");

        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        JsonNode docShape = root.get("TestApp").get("entityTypes").get("Document").get("shape");
        JsonNode attributes = docShape.get("attributes");

        assertNotNull(attributes);
        assertTrue(attributes.has("tags"));

        JsonNode tagsType = attributes.get("tags");
        assertEquals("Set", tagsType.get("type").asText());
        assertEquals("String", tagsType.get("element").get("type").asText());
    }

    @Test
    void generateSchema_shouldIncludeActions() {
        Map<String, ResourceExtractor<?>> extractors = new HashMap<>();
        extractors.put("User", new UserExtractor());

        CedarSchemaGenerator generator = new CedarSchemaGenerator("TestApp");
        String schema = generator.generateSchema(
            extractors,
            Set.of("ViewUser", "CreateUser", "DeleteUser"),
            "User"
        );

        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        JsonNode actions = root.get("TestApp").get("actions");

        assertNotNull(actions);
        assertTrue(actions.has("ViewUser"));
        assertTrue(actions.has("CreateUser"));
        assertTrue(actions.has("DeleteUser"));

        // Verify action structure
        JsonNode viewUserAction = actions.get("ViewUser");
        assertTrue(viewUserAction.has("appliesTo"));

        JsonNode appliesTo = viewUserAction.get("appliesTo");
        assertTrue(appliesTo.has("principalTypes"));
        assertTrue(appliesTo.has("resourceTypes"));
    }

    @Test
    void parseSchema_shouldParseValidJson() {
        String validJson = "{\"namespace\": {\"entityTypes\": {}}}";
        JsonNode result = CedarSchemaGenerator.parseSchema(validJson);

        assertNotNull(result);
        assertTrue(result.has("namespace"));
    }

    @Test
    void parseSchema_shouldThrowOnInvalidJson() {
        assertThrows(RuntimeException.class, () -> {
            CedarSchemaGenerator.parseSchema("invalid json {");
        });
    }

    // Mock extractors for testing
    static class UserExtractor implements ResourceExtractor<String> {
        @Override
        public String getResourceType() {
            return "User";
        }

        @Override
        public ResourceEntity extract(String input, ResourceExtractionContext context) {
            return ResourceEntity.builder()
                .entityType("User")
                .entityId(input)
                .build();
        }

        @Override
        public Map<String, AttributeType> getAttributeSchema() {
            Map<String, AttributeType> schema = new HashMap<>();
            schema.put("email", AttributeType.STRING);
            schema.put("role", AttributeType.STRING);
            schema.put("active", AttributeType.BOOLEAN);
            return schema;
        }
    }

    static class DocumentExtractor implements ResourceExtractor<String> {
        @Override
        public String getResourceType() {
            return "Document";
        }

        @Override
        public ResourceEntity extract(String input, ResourceExtractionContext context) {
            return ResourceEntity.builder()
                .entityType("Document")
                .entityId(input)
                .build();
        }

        @Override
        public Map<String, AttributeType> getAttributeSchema() {
            Map<String, AttributeType> schema = new HashMap<>();
            schema.put("title", AttributeType.STRING);
            schema.put("tags", AttributeType.SET_STRING);
            schema.put("pageCount", AttributeType.LONG);
            return schema;
        }
    }
}
