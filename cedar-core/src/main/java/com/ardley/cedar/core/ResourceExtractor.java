package com.ardley.cedar.core;

import java.util.Map;

/**
 * Extracts a Cedar resource entity from input of type T.
 * Generic type T allows type-safe extraction from various sources:
 * - String (for path/query parameters)
 * - Complex types (for request bodies)
 *
 * @param <T> Input type to extract from
 */
public interface ResourceExtractor<T> {

    /**
     * The Cedar resource type this extractor handles.
     * Must match the entity type in your Cedar schema.
     *
     * @return Resource type (e.g., "User", "LoanApplication", "Document")
     */
    String getResourceType();

    /**
     * Extract a ResourceEntity from the input.
     * Typically involves looking up the resource from a database and mapping attributes.
     *
     * @param input The input value (e.g., resource ID string, request body object)
     * @param context Extraction context with additional request data
     * @return ResourceEntity with all Cedar attributes populated
     */
    ResourceEntity extract(T input, ResourceExtractionContext context);

    /**
     * Get the attribute schema for this resource type.
     * Used for runtime schema generation and validation.
     *
     * @return Map of attribute name to attribute type
     */
    default Map<String, AttributeType> getAttributeSchema() {
        return Map.of();
    }

    /**
     * Cedar attribute types for schema generation.
     */
    enum AttributeType {
        /** String attribute type. */
        STRING,
        /** Long integer attribute type. */
        LONG,
        /** Boolean attribute type. */
        BOOLEAN,
        /** Set of strings attribute type. */
        SET_STRING,
        /** Set of long integers attribute type. */
        SET_LONG
    }
}
