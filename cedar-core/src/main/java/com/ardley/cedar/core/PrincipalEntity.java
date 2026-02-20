package com.ardley.cedar.core;

import java.util.Map;
import java.util.Set;

/**
 * Represents the authenticated principal (user) making a request.
 * Contains identity information and attributes for Cedar policy evaluation.
 */
public class PrincipalEntity {
    private final String userId;
    private final String customerId;
    private final String role;
    private final String email;
    private final Set<String> groups;
    private final Map<String, Object> attributes;

    private PrincipalEntity(Builder builder) {
        this.userId = builder.userId;
        this.customerId = builder.customerId;
        this.role = builder.role;
        this.email = builder.email;
        this.groups = builder.groups;
        this.attributes = builder.attributes;
    }

    /**
     * Creates a new builder for constructing principal entities.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the user identifier.
     *
     * @return The user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the customer identifier.
     *
     * @return The customer ID
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Returns the user's role.
     *
     * @return The role
     */
    public String getRole() {
        return role;
    }

    /**
     * Returns the user's email address.
     *
     * @return The email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Returns the groups this user belongs to.
     *
     * @return Immutable set of group names
     */
    public Set<String> getGroups() {
        return groups;
    }

    /**
     * Returns additional attributes for Cedar policy evaluation.
     *
     * @return Map of attribute names to values
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Builder for constructing principal entities.
     */
    public static class Builder {
        private String userId;
        private String customerId;
        private String role;
        private String email;
        private Set<String> groups = Set.of();
        private Map<String, Object> attributes = Map.of();

        /**
         * Sets the user identifier.
         *
         * @param userId The user ID
         * @return This builder
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the customer identifier.
         *
         * @param customerId The customer ID
         * @return This builder
         */
        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        /**
         * Sets the user's role.
         *
         * @param role The role
         * @return This builder
         */
        public Builder role(String role) {
            this.role = role;
            return this;
        }

        /**
         * Sets the user's email address.
         *
         * @param email The email
         * @return This builder
         */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /**
         * Sets the groups this user belongs to.
         *
         * @param groups Set of group names
         * @return This builder
         */
        public Builder groups(Set<String> groups) {
            this.groups = groups;
            return this;
        }

        /**
         * Sets additional attributes for Cedar policy evaluation.
         *
         * @param attributes Map of attribute names to values
         * @return This builder
         */
        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        /**
         * Builds the principal entity.
         *
         * @return A new principal entity instance
         */
        public PrincipalEntity build() {
            return new PrincipalEntity(this);
        }
    }
}
