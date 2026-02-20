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

    public static Builder builder() {
        return new Builder();
    }

    public String getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public boolean hasAttribute(String attributeName) {
        return attributes.containsKey(attributeName);
    }

    public static class Builder {
        private String entityType;
        private String entityId;
        private String customerId;
        private Map<String, Object> attributes = Map.of();

        public Builder entityType(String entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder entityId(String entityId) {
            this.entityId = entityId;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public ResourceEntity build() {
            return new ResourceEntity(this);
        }
    }
}
