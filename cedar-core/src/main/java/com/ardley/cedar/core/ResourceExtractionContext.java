package com.ardley.cedar.core;

import java.util.Map;

/**
 * Context information available during resource extraction.
 * Framework-agnostic container for request data.
 */
public class ResourceExtractionContext {
    private final Map<String, String> pathParameters;
    private final Map<String, String> queryParameters;
    private final Map<String, Object> properties;

    private ResourceExtractionContext(Builder builder) {
        this.pathParameters = builder.pathParameters;
        this.queryParameters = builder.queryParameters;
        this.properties = builder.properties;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getPathParameter(String name) {
        return pathParameters.get(name);
    }

    public String getQueryParameter(String name) {
        return queryParameters.get(name);
    }

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public static class Builder {
        private Map<String, String> pathParameters = Map.of();
        private Map<String, String> queryParameters = Map.of();
        private Map<String, Object> properties = Map.of();

        public Builder pathParameters(Map<String, String> pathParameters) {
            this.pathParameters = pathParameters;
            return this;
        }

        public Builder queryParameters(Map<String, String> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }

        public Builder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public ResourceExtractionContext build() {
            return new ResourceExtractionContext(this);
        }
    }
}
