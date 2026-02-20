package com.ardley.cedar.jaxrs;

import com.ardley.cedar.core.CedarSchemaGenerator;
import com.ardley.cedar.core.ResourceExtractor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Immutable registry for ResourceExtractor implementations.
 * Maps resource types to their extractors for O(1) lookup.
 *
 * <p>Example usage:
 * <pre>{@code
 * ResourceExtractorRegistry registry = ResourceExtractorRegistry.builder()
 *     .register(new UserResourceExtractor(userRepository))
 *     .register(new LoanApplicationResourceExtractor(loanAppRepository))
 *     .build();
 * }</pre>
 */
public class ResourceExtractorRegistry {
    private final Map<String, ResourceExtractor<?>> extractors;

    private ResourceExtractorRegistry(Builder builder) {
        this.extractors = Map.copyOf(builder.extractors);
    }

    /**
     * Creates a new builder for ResourceExtractorRegistry.
     *
     * @return A new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get extractor for a resource type.
     *
     * @param resourceType The resource type (e.g., "LoanApplication")
     * @return The registered extractor
     * @throws IllegalArgumentException if no extractor is registered for the type
     */
    public ResourceExtractor<?> getExtractor(String resourceType) {
        ResourceExtractor<?> extractor = extractors.get(resourceType);
        if (extractor == null) {
            throw new IllegalArgumentException(
                String.format(
                    "No ResourceExtractor registered for resource type: '%s'. Available types: [%s]",
                    resourceType,
                    String.join(", ", extractors.keySet())
                )
            );
        }
        return extractor;
    }

    /**
     * Check if a resource type has an extractor registered.
     *
     * @param resourceType The resource type to check
     * @return true if an extractor is registered
     */
    public boolean hasExtractor(String resourceType) {
        return extractors.containsKey(resourceType);
    }

    /**
     * Get all registered resource types.
     *
     * @return Set of all registered resource type names
     */
    public Set<String> getRegisteredTypes() {
        return extractors.keySet();
    }

    /**
     * Get all registered extractors.
     *
     * @return Immutable map of resource type to extractor
     */
    public Map<String, ResourceExtractor<?>> getExtractors() {
        return extractors;
    }

    /**
     * Generate Cedar schema JSON for all registered extractors.
     *
     * @param namespace The Cedar namespace (e.g., "MyApp")
     * @param actions Set of all actions used in the application
     * @param principalType The entity type used for principals (e.g., "User")
     * @return Cedar schema as JSON string
     * @deprecated Use {@link #generateSchemaFromClasspath(String, String, String)} for automatic action discovery
     */
    @Deprecated
    public String generateSchema(String namespace, Set<String> actions, String principalType) {
        CedarSchemaGenerator generator = new CedarSchemaGenerator(namespace);
        return generator.generateSchema(extractors, actions, principalType);
    }

    /**
     * Generate Cedar schema JSON by automatically scanning classpath for Cedar annotations.
     *
     * <p>Scans the specified package for @RequiresActions and @CedarResource annotations
     * to automatically discover which actions apply to which resource types.
     *
     * <p>Actions found only in @RequiresActions are context-based (resourceTypes: []).
     * Actions found in @CedarResource are resource-based with explicit resource types.
     *
     * @param namespace The Cedar namespace (e.g., "MyApp")
     * @param packageToScan Base package to scan for annotations (e.g., "com.myapp.api")
     * @param principalType The entity type used for principals (e.g., "User")
     * @return Cedar schema as JSON string
     */
    public String generateSchemaFromClasspath(String namespace, String packageToScan, String principalType) {
        ClasspathAnnotationScanner scanner = new ClasspathAnnotationScanner();
        ClasspathAnnotationScanner.ScanResult scanResult = scanner.scan(packageToScan);

        CedarSchemaGenerator generator = new CedarSchemaGenerator(namespace);
        return generator.generateSchema(
            extractors,
            scanResult.getActionToResourceTypes(),
            principalType
        );
    }

    /**
     * Generate Cedar schema JSON by scanning specific classes for Cedar annotations.
     *
     * <p>Useful for testing or when you want explicit control over which classes to scan.
     *
     * @param namespace The Cedar namespace (e.g., "MyApp")
     * @param principalType The entity type used for principals (e.g., "User")
     * @param classes Classes to scan for annotations
     * @return Cedar schema as JSON string
     */
    public String generateSchemaFromClasses(String namespace, String principalType, Class<?>... classes) {
        ClasspathAnnotationScanner scanner = new ClasspathAnnotationScanner();
        ClasspathAnnotationScanner.ScanResult scanResult = scanner.scanClasses(classes);

        CedarSchemaGenerator generator = new CedarSchemaGenerator(namespace);
        return generator.generateSchema(
            extractors,
            scanResult.getActionToResourceTypes(),
            principalType
        );
    }

    /**
     * Builder for ResourceExtractorRegistry.
     */
    public static class Builder {
        private final Map<String, ResourceExtractor<?>> extractors = new HashMap<>();

        /**
         * Register a resource extractor.
         *
         * @param extractor The extractor to register
         * @return This builder
         * @throws IllegalStateException if an extractor is already registered for this type
         */
        public Builder register(ResourceExtractor<?> extractor) {
            String resourceType = extractor.getResourceType();
            if (extractors.containsKey(resourceType)) {
                throw new IllegalStateException(
                    "Extractor already registered for resource type: " + resourceType
                );
            }
            extractors.put(resourceType, extractor);
            return this;
        }

        /**
         * Build the immutable registry.
         *
         * @return Immutable ResourceExtractorRegistry
         */
        public ResourceExtractorRegistry build() {
            return new ResourceExtractorRegistry(this);
        }
    }
}
