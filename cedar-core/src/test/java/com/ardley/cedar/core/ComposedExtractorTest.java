package com.ardley.cedar.core;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComposedExtractorTest {

    static class TestRequest {
        private final String loanAppId;

        TestRequest(String loanAppId) {
            this.loanAppId = loanAppId;
        }

        String getLoanAppId() {
            return loanAppId;
        }
    }

    @Test
    void extract_shouldComposeExtractors() {
        // Mock base extractor
        @SuppressWarnings("unchecked")
        ResourceExtractor<String> baseExtractor = mock(ResourceExtractor.class);
        when(baseExtractor.getResourceType()).thenReturn("LoanApplication");

        ResourceEntity expectedResource = ResourceEntity.builder()
            .entityType("LoanApplication")
            .entityId("loan-123")
            .customerId("customer-456")
            .build();

        ResourceExtractionContext context = ResourceExtractionContext.builder().build();
        when(baseExtractor.extract("loan-123", context)).thenReturn(expectedResource);

        // Create composed extractor
        ComposedExtractor<TestRequest> composedExtractor = new ComposedExtractor<>(
            TestRequest::getLoanAppId,
            baseExtractor
        );

        // Test
        TestRequest request = new TestRequest("loan-123");
        ResourceEntity result = composedExtractor.extract(request, context);

        assertEquals(expectedResource, result);
        assertEquals("LoanApplication", composedExtractor.getResourceType());
        verify(baseExtractor).extract("loan-123", context);
    }

    @Test
    void getAttributeSchema_shouldDelegateToBaseExtractor() {
        @SuppressWarnings("unchecked")
        ResourceExtractor<String> baseExtractor = mock(ResourceExtractor.class);
        when(baseExtractor.getResourceType()).thenReturn("LoanApplication");

        Map<String, ResourceExtractor.AttributeType> schema = Map.of(
            "loanApplicationId", ResourceExtractor.AttributeType.STRING
        );
        when(baseExtractor.getAttributeSchema()).thenReturn(schema);

        ComposedExtractor<TestRequest> composedExtractor = new ComposedExtractor<>(
            TestRequest::getLoanAppId,
            baseExtractor
        );

        assertEquals(schema, composedExtractor.getAttributeSchema());
    }
}
