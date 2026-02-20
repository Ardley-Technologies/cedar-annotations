package com.ardley.cedar.core;

import java.util.Map;

/**
 * Represents a resource being accessed in a Cedar authorization request.
 * Contains the resource type, ID, and attributes for policy evaluation.
 */
public class ResourceEntity {
    private final String entityType;
    private final String entityId;
    private final String customerId;
    private final Map<String, Object> attributes;

    private ResourceEntity(Builder builder) {
        this.entityType = builder.entityType;
        this.entityId = builder.entityId;
        this.customerId = builder.customerId;
        this.attributes = builder.attributes;
    }

    /**
     * Creates a new builder for constructing resource entities.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the Cedar entity type of this resource.
     *
     * @return The entity type
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Returns the unique identifier for this resource.
     *
     * @return The entity ID
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * Returns the customer identifier this resource belongs to.
     *
     * @return The customer ID
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Returns the Cedar attributes for policy evaluation.
     *
     * @return Map of attribute names to values
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Checks if this resource has a specific attribute.
     *
     * @param attributeName The attribute name to check
     * @return true if the attribute exists
     */
    public boolean hasAttribute(String attributeName) {
        return attributes.containsKey(attributeName);
    }

    /**
     * Builder for constructing resource entities.
     */
    public static class Builder {
        private String entityType;
        private String entityId;
        private String customerId;
        private Map<String, Object> attributes = Map.of();

        /**
         * Sets the Cedar entity type.
         *
         * @param entityType The entity type
         * @return This builder
         */
        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        /**
         * Sets the unique resource identifier.
         *
         * @param entityId The entity ID
         * @return This builder
         */
        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        /**
         * Sets the customer identifier this resource belongs to.
         *
         * @param customerId The customer ID
         * @return This builder
         */
        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        /**
         * Sets the Cedar attributes for policy evaluation.
         *
         * @param attributes Map of attribute names to values
         * @return This builder
         */
        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Builds the resource entity.
         *
         * @return A new resource entity instance
         */
        public ResourceEntity build() {
            return new ResourceEntity(this);
        }
    }
}
