package com.ardley.cedar.jaxrs;

import com.ardley.cedar.core.CedarSchemaGenerator;
import com.ardley.cedar.core.ResourceEntity;
import com.ardley.cedar.core.ResourceExtractionContext;
import com.ardley.cedar.core.ResourceExtractor;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.ws.rs.PathParam;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that generated Cedar schemas match AWS Verified Permissions requirements.
 */
class CedarSchemaFormatTest {

    @Test
    void generatedSchema_shouldMatchCedarFormat() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new UserExtractor())
            .register(new DocumentExtractor())
            .build();

        String schema = registry.generateSchemaFromClasses(
            "MyApp",
            "User",
            TestEndpoints.class
        );

        assertNotNull(schema);

        // Parse and validate structure
        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        assertTrue(root.has("MyApp"), "Schema must have namespace at root");

        JsonNode namespace = root.get("MyApp");
        assertTrue(namespace.has("entityTypes"), "Namespace must have entityTypes");
        assertTrue(namespace.has("actions"), "Namespace must have actions");
    }

    @Test
    void generatedSchema_shouldHaveRequiredEntityTypes() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new UserExtractor())
            .register(new DocumentExtractor())
            .build();

        String schema = registry.generateSchemaFromClasses(
            "TestApp",
            "User",
            TestEndpoints.class
        );

        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        JsonNode entityTypes = root.get("TestApp").get("entityTypes");

        assertTrue(entityTypes.has("User"), "Must have User entity type");
        assertTrue(entityTypes.has("Document"), "Must have Document entity type");
    }

    @Test
    void generatedSchema_contextBasedActions_shouldHaveEmptyResourceTypes() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new UserExtractor())
            .build();

        String schema = registry.generateSchemaFromClasses(
            "TestApp",
            "User",
            TestEndpoints.class
        );

        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        JsonNode actions = root.get("TestApp").get("actions");

        // CreateUser is context-based (@RequiresActions without resource)
        assertTrue(actions.has("CreateUser"), "Must have CreateUser action");

        JsonNode createUserAction = actions.get("CreateUser");
        assertTrue(createUserAction.has("appliesTo"), "Action must have appliesTo");

        JsonNode appliesTo = createUserAction.get("appliesTo");
        assertTrue(appliesTo.has("principalTypes"), "appliesTo must have principalTypes");
        assertTrue(appliesTo.has("resourceTypes"), "appliesTo must have resourceTypes");

        // CRITICAL: Context-based actions must have EMPTY resourceTypes array []
        JsonNode resourceTypes = appliesTo.get("resourceTypes");
        assertTrue(resourceTypes.isArray(), "resourceTypes must be an array");
        assertEquals(0, resourceTypes.size(),
            "Context-based actions must have empty resourceTypes array []");
    }

    @Test
    void generatedSchema_resourceBasedActions_shouldHaveSpecificResourceTypes() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new UserExtractor())
            .build();

        String schema = registry.generateSchemaFromClasses(
            "TestApp",
            "User",
            TestEndpoints.class
        );

        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        JsonNode actions = root.get("TestApp").get("actions");

        // ViewUser is resource-based (@CedarResource on User)
        assertTrue(actions.has("ViewUser"), "Must have ViewUser action");

        JsonNode viewUserAction = actions.get("ViewUser");
        JsonNode appliesTo = viewUserAction.get("appliesTo");
        JsonNode resourceTypes = appliesTo.get("resourceTypes");

        assertTrue(resourceTypes.isArray(), "resourceTypes must be an array");
        assertEquals(1, resourceTypes.size(), "ViewUser should apply to exactly 1 resource type");
        assertEquals("User", resourceTypes.get(0).asText(),
            "ViewUser should apply to User resource type");
    }

    @Test
    void generatedSchema_multiResourceAction_shouldListAllResourceTypes() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new UserExtractor())
            .register(new DocumentExtractor())
            .build();

        String schema = registry.generateSchemaFromClasses(
            "TestApp",
            "User",
            TestEndpoints.class
        );

        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        JsonNode actions = root.get("TestApp").get("actions");

        // View action is used on both User and Document
        assertTrue(actions.has("View"), "Must have View action");

        JsonNode viewAction = actions.get("View");
        JsonNode appliesTo = viewAction.get("appliesTo");
        JsonNode resourceTypes = appliesTo.get("resourceTypes");

        assertTrue(resourceTypes.isArray());
        assertEquals(2, resourceTypes.size(), "View should apply to 2 resource types");

        // Check both User and Document are present
        Set<String> types = Set.of(
            resourceTypes.get(0).asText(),
            resourceTypes.get(1).asText()
        );
        assertTrue(types.contains("User"));
        assertTrue(types.contains("Document"));
    }

    @Test
    void generatedSchema_principalTypes_shouldAlwaysBeSet() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new UserExtractor())
            .build();

        String schema = registry.generateSchemaFromClasses(
            "TestApp",
            "User",
            TestEndpoints.class
        );

        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        JsonNode actions = root.get("TestApp").get("actions");

        // All actions must have principalTypes
        actions.fieldNames().forEachRemaining(actionName -> {
            JsonNode action = actions.get(actionName);
            JsonNode appliesTo = action.get("appliesTo");
            JsonNode principalTypes = appliesTo.get("principalTypes");

            assertTrue(principalTypes.isArray(),
                "Action " + actionName + " must have principalTypes array");
            assertTrue(principalTypes.size() > 0,
                "Action " + actionName + " must have at least one principal type");
            assertEquals("User", principalTypes.get(0).asText(),
                "Principal type should be User");
        });
    }

    @Test
    void generatedSchema_entityAttributes_shouldHaveProperTypes() {
        ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
            .register(new DocumentExtractor())
            .build();

        String schema = registry.generateSchemaFromClasses(
            "TestApp",
            "User",
            TestEndpoints.class
        );

        JsonNode root = CedarSchemaGenerator.parseSchema(schema);
        JsonNode entityTypes = root.get("TestApp").get("entityTypes");
        JsonNode document = entityTypes.get("Document");

        assertTrue(document.has("shape"));
        JsonNode shape = document.get("shape");
        assertEquals("Record", shape.get("type").asText());

        assertTrue(shape.has("attributes"));
        JsonNode attributes = shape.get("attributes");

        // Check String attribute
        assertTrue(attributes.has("title"));
        assertEquals("String", attributes.get("title").get("type").asText());

        // Check Boolean attribute
        assertTrue(attributes.has("private"));
        assertEquals("Boolean", attributes.get("private").get("type").asText());

        // Check Set<String> attribute
        assertTrue(attributes.has("tags"));
        JsonNode tags = attributes.get("tags");
        assertEquals("Set", tags.get("type").asText());
        assertTrue(tags.has("element"));
        assertEquals("String", tags.get("element").get("type").asText());
    }

    // Test resource extractors
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
            schema.put("private", AttributeType.BOOLEAN);
            schema.put("tags", AttributeType.SET_STRING);
            return schema;
        }
    }

    // Test REST endpoints with Cedar annotations
    public static class TestEndpoints {

        @RequiresActions({"CreateUser"})
        public void createUser() {
            // Context-based action - no specific resource
        }

        public void getUser(
            @PathParam("userId")
            @CedarResource(type = "User", actions = {"ViewUser"})
            String userId
        ) {
            // Resource-based action on User
        }

        public void viewUserDetails(
            @PathParam("userId")
            @CedarResource(type = "User", actions = {"View"})
            String userId
        ) {
            // View on User
        }

        public void viewDocument(
            @PathParam("docId")
            @CedarResource(type = "Document", actions = {"View"})
            String docId
        ) {
            // View on Document - same action, different resource
        }
    }
}
