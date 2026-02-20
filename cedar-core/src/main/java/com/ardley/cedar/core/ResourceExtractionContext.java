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

    /**
     * Creates a new builder for constructing extraction contexts.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns a path parameter value by name.
     *
     * @param name The parameter name
     * @return The parameter value, or null if not found
     */
    public String getPathParameter(String name) {
        return pathParameters.get(name);
    }

    /**
     * Returns a query parameter value by name.
     *
     * @param name The parameter name
     * @return The parameter value, or null if not found
     */
    public String getQueryParameter(String name) {
        return queryParameters.get(name);
    }

    /**
     * Returns a custom property value by name.
     *
     * @param name The property name
     * @return The property value, or null if not found
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }

    /**
     * Returns all path parameters.
     *
     * @return Map of parameter names to values
     */
    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    /**
     * Returns all query parameters.
     *
     * @return Map of parameter names to values
     */
    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    /**
     * Returns all custom properties.
     *
     * @return Map of property names to values
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Builder for constructing resource extraction contexts.
     */
    public static class Builder {
        private Map<String, String> pathParameters = Map.of();
        private Map<String, String> queryParameters = Map.of();
        private Map<String, Object> properties = Map.of();

        /**
         * Sets the path parameters from the request.
         *
         * @param pathParameters Map of parameter names to values
         * @return This builder
         */
        public Builder pathParameters(Map<String, String> pathParameters) {
            this.pathParameters = pathParameters;
            return this;
        }

        /**
         * Sets the query parameters from the request.
         *
         * @param queryParameters Map of parameter names to values
         * @return This builder
         */
        public Builder queryParameters(Map<String, String> queryParameters) {
            this.queryParameters = queryParameters;
            return this;
        }

        /**
         * Sets custom properties for the extraction context.
         *
         * @param properties Map of property names to values
         * @return This builder
         */
        public Builder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        /**
         * Builds the resource extraction context.
         *
         * @return A new context instance
         */
        public ResourceExtractionContext build() {
            return new ResourceExtractionContext(this);
        }
    }
}
