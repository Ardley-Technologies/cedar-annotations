package com.ardley.cedar.core;

import java.util.Map;
import java.util.function.Function;

/**
 * Composes two extractors: InputType → IntermediateType → ResourceEntity.
 * Useful for extracting resources from complex request bodies.
 *
 * Example: CreateReportRequest → String (loanAppId) → ResourceEntity
 *
 * @param <T> Input type (e.g., CreateReportRequest)
 */
public class ComposedExtractor<T> implements ResourceExtractor<T> {

    private final Function<T, String> idExtractor;
    private final ResourceExtractor<String> resourceExtractor;

    /**
     * Create a composed extractor.
     *
     * @param idExtractor Function to extract resource ID from input
     * @param resourceExtractor Extractor to get ResourceEntity from ID
     */
    public ComposedExtractor(
        Function<T, String> idExtractor,
        ResourceExtractor<String> resourceExtractor
    ) {
        this.idExtractor = idExtractor;
        this.resourceExtractor = resourceExtractor;
    }

    @Override
    public String getResourceType() {
        return resourceExtractor.getResourceType();
    }

    @Override
    public ResourceEntity extract(T input, ResourceExtractionContext context) {
        // Step 1: Extract ID from input
        String resourceId = idExtractor.apply(input);

        // Step 2: Extract resource from ID
        return resourceExtractor.extract(resourceId, context);
    }

    @Override
    public Map<String, AttributeType> getAttributeSchema() {
        return resourceExtractor.getAttributeSchema();
    }
}
