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

    public static Builder builder() {
        return new Builder();
    }

    public String getUserId() {
        return userId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static class Builder {
        private String userId;
        private String customerId;
        private String role;
        private String email;
        private Set<String> groups = Set.of();
        private Map<String, Object> attributes = Map.of();

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder customerId(String customerId) {
            this.customerId = customerId;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder groups(Set<String> groups) {
            this.groups = groups;
            return this;
        }

        public Builder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        public PrincipalEntity build() {
            return new PrincipalEntity(this);
        }
    }
}
