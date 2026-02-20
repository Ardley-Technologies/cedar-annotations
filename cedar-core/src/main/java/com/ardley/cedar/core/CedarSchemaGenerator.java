package com.ardley.cedar.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Generates Cedar policy schema JSON from ResourceExtractor definitions.
 *
 * <p>Cedar schemas define the structure of entities and their attributes,
 * which enables policy validation and enhanced authorization checks.
 */
public class CedarSchemaGenerator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String namespace;

    /**
     * Creates a new schema generator for the specified namespace.
     *
     * @param namespace The Cedar namespace for the schema (e.g., "MyApp")
     */
    public CedarSchemaGenerator(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Generates a complete Cedar schema JSON string from a map of extractors.
     *
     * @param extractors Map of resource type to extractor
     * @param actions Set of all actions referenced in the application
     * @param principalType The entity type used for principals (e.g., "User")
     * @return Cedar schema as JSON string
     * @deprecated Use {@link #generateSchema(Map, Map, String)} with action-to-resource mappings
     */
    @Deprecated
    public String generateSchema(
            Map<String, ResourceExtractor<?>> extractors,
            Set<String> actions,
            String principalType) {
        // For backward compatibility, assume all actions apply to all resource types
        Map<String, Set<String>> actionToResourceTypes = new HashMap<>();
        for (String action : actions) {
            actionToResourceTypes.put(action, new HashSet<>(extractors.keySet()));
        }
        return generateSchema(extractors, actionToResourceTypes, principalType);
    }

    /**
     * Generates a complete Cedar schema JSON string with explicit action-to-resource-type mappings.
     *
     * @param extractors Map of resource type to extractor
     * @param actionToResourceTypes Map of action name to the set of resource types it applies to.
     *                              Empty set means context-based action (no specific resource).
     * @param principalType The entity type used for principals (e.g., "User")
     * @return Cedar schema as JSON string
     */
    public String generateSchema(
            Map<String, ResourceExtractor<?>> extractors,
            Map<String, Set<String>> actionToResourceTypes,
            String principalType) {

        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode namespaceNode = MAPPER.createObjectNode();

        // Generate entity types
        ObjectNode entityTypes = generateEntityTypes(extractors, principalType);
        namespaceNode.set("entityTypes", entityTypes);

        // Generate actions with proper resource type mappings
        ObjectNode actionsNode = generateActionsWithMappings(actionToResourceTypes, principalType);
        namespaceNode.set("actions", actionsNode);

        root.set(namespace, namespaceNode);

        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Cedar schema", e);
        }
    }

    /**
     * Generates entity type definitions from extractors.
     */
    private ObjectNode generateEntityTypes(Map<String, ResourceExtractor<?>> extractors, String principalType) {
        ObjectNode entityTypes = MAPPER.createObjectNode();

        // Add principal type (if not already in extractors)
        if (!extractors.containsKey(principalType)) {
            entityTypes.set(principalType, createEntityShape(Map.of()));
        }

        // Add resource types from extractors
        for (Map.Entry<String, ResourceExtractor<?>> entry : extractors.entrySet()) {
            String resourceType = entry.getKey();
            ResourceExtractor<?> extractor = entry.getValue();

            Map<String, ResourceExtractor.AttributeType> schema = extractor.getAttributeSchema();
            entityTypes.set(resourceType, createEntityShape(schema));
        }

        return entityTypes;
    }

    /**
     * Creates an entity shape definition with attributes.
     */
    private ObjectNode createEntityShape(Map<String, ResourceExtractor.AttributeType> attributes) {
        ObjectNode shape = MAPPER.createObjectNode();
        ObjectNode shapeType = MAPPER.createObjectNode();
        shapeType.put("type", "Record");

        if (!attributes.isEmpty()) {
            ObjectNode attributesNode = MAPPER.createObjectNode();

            for (Map.Entry<String, ResourceExtractor.AttributeType> entry : attributes.entrySet()) {
                String attrName = entry.getKey();
                ResourceExtractor.AttributeType attrType = entry.getValue();

                attributesNode.set(attrName, convertAttributeType(attrType));
            }

            shapeType.set("attributes", attributesNode);
        }

        shape.set("shape", shapeType);
        return shape;
    }

    /**
     * Converts framework AttributeType to Cedar type definition.
     */
    private ObjectNode convertAttributeType(ResourceExtractor.AttributeType type) {
        ObjectNode typeNode = MAPPER.createObjectNode();

        switch (type) {
            case STRING:
                typeNode.put("type", "String");
                break;
            case LONG:
                typeNode.put("type", "Long");
                break;
            case BOOLEAN:
                typeNode.put("type", "Boolean");
                break;
            case SET_STRING:
                typeNode.put("type", "Set");
                ObjectNode stringElement = MAPPER.createObjectNode();
                stringElement.put("type", "String");
                typeNode.set("element", stringElement);
                break;
            case SET_LONG:
                typeNode.put("type", "Set");
                ObjectNode longElement = MAPPER.createObjectNode();
                longElement.put("type", "Long");
                typeNode.set("element", longElement);
                break;
            default:
                throw new IllegalArgumentException("Unsupported attribute type: " + type);
        }

        return typeNode;
    }

    /**
     * Generates action definitions (legacy method).
     * @deprecated Use generateActionsWithMappings
     */
    @Deprecated
    private ObjectNode generateActions(Set<String> actions, Set<String> resourceTypes, String principalType) {
        ObjectNode actionsNode = MAPPER.createObjectNode();

        for (String action : actions) {
            ObjectNode actionDef = MAPPER.createObjectNode();
            ObjectNode appliesTo = MAPPER.createObjectNode();

            // Principal types
            ArrayNode principalTypes = MAPPER.createArrayNode();
            principalTypes.add(principalType);
            appliesTo.set("principalTypes", principalTypes);

            // Resource types - actions can apply to any resource type
            ArrayNode resourceTypesArray = MAPPER.createArrayNode();
            for (String resourceType : resourceTypes) {
                resourceTypesArray.add(resourceType);
            }
            appliesTo.set("resourceTypes", resourceTypesArray);

            actionDef.set("appliesTo", appliesTo);
            actionsNode.set(action, actionDef);
        }

        return actionsNode;
    }

    /**
     * Generates action definitions with explicit resource type mappings.
     *
     * <p>Each action specifies exactly which resource types it applies to:
     * <ul>
     *   <li>Empty set = context-based action, applies to no specific resource (resourceTypes: [])</li>
     *   <li>Non-empty set = resource-based action, applies to specified types (resourceTypes: ["Type1", ...])</li>
     * </ul>
     *
     * @param actionToResourceTypes Map of action name to resource types it applies to
     * @param principalType The principal entity type
     * @return JSON object node with action definitions
     */
    private ObjectNode generateActionsWithMappings(
            Map<String, Set<String>> actionToResourceTypes,
            String principalType) {

        ObjectNode actionsNode = MAPPER.createObjectNode();

        for (Map.Entry<String, Set<String>> entry : actionToResourceTypes.entrySet()) {
            String actionName = entry.getKey();
            Set<String> resourceTypes = entry.getValue();

            ObjectNode actionDef = MAPPER.createObjectNode();
            ObjectNode appliesTo = MAPPER.createObjectNode();

            // Principal types - always includes the principal type
            ArrayNode principalTypes = MAPPER.createArrayNode();
            principalTypes.add(principalType);
            appliesTo.set("principalTypes", principalTypes);

            // Resource types - explicitly set based on discovered mappings
            // Empty array [] means context-based (no specific resource)
            // Populated array means resource-based (specific resource types)
            ArrayNode resourceTypesArray = MAPPER.createArrayNode();
            for (String resourceType : resourceTypes) {
                resourceTypesArray.add(resourceType);
            }
            appliesTo.set("resourceTypes", resourceTypesArray);

            actionDef.set("appliesTo", appliesTo);
            actionsNode.set(actionName, actionDef);
        }

        return actionsNode;
    }

    /**
     * Parses a Cedar schema JSON string into a structured object.
     *
     * @param schemaJson The schema JSON string
     * @return Parsed JsonNode
     */
    public static JsonNode parseSchema(String schemaJson) {
        try {
            return MAPPER.readTree(schemaJson);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Cedar schema", e);
        }
    }
}
