package com.ardley.cedar.jaxrs;

import com.ardley.cedar.core.*;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class SchemaValidationTest {

    private ContainerRequestContext requestContext;
    private ResourceInfo resourceInfo;
    private PrincipalExtractor principalExtractor;
    private PolicyStoreResolver policyStoreResolver;
    private ResourceExtractorRegistry extractorRegistry;
    private AuthorizationService authorizationService;
    private CedarAuthorizationFilter filter;

    @BeforeEach
    void setUp() {
        requestContext = mock(ContainerRequestContext.class);
        resourceInfo = mock(ResourceInfo.class);
        principalExtractor = mock(PrincipalExtractor.class);
        policyStoreResolver = mock(PolicyStoreResolver.class);
        authorizationService = mock(AuthorizationService.class);
    }

    @Test
    void filter_shouldPassValidationWhenAttributesMatchSchema() throws Exception {
        // Create extractor with schema
        ResourceExtractor<String> schemaExtractor = new ResourceExtractor<>() {
            @Override
            public String getResourceType() {
                return "Document";
            }

            @Override
            public ResourceEntity extract(String input, ResourceExtractionContext context) {
                return ResourceEntity.builder()
                    .entityType("Document")
                    .entityId(input)
                    .customerId("customer-123")
                    .attributes(Map.of(
                        "title", "Test Document",
                        "status", "active"
                    ))
                    .build();
            }

            @Override
            public Map<String, AttributeType> getAttributeSchema() {
                Map<String, AttributeType> schema = new HashMap<>();
                schema.put("title", AttributeType.STRING);
                schema.put("status", AttributeType.STRING);
                return schema;
            }
        };

        extractorRegistry = ResourceExtractorRegistry.builder()
            .register(schemaExtractor)
            .build();

        filter = CedarAuthorizationFilter.builder()
            .principalExtractor(principalExtractor)
            .policyStoreResolver(policyStoreResolver)
            .contextEntityType("Customer")
            .extractorRegistry(extractorRegistry)
            .authorizationService(authorizationService)
            .validateSchema(true)  // Enable validation
            .build();

        injectResourceInfo(filter, resourceInfo);

        Method method = TestResource.class.getMethod("getDocument", String.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        PrincipalEntity principal = PrincipalEntity.builder()
            .userId("user-123")
            .customerId("customer-456")
            .build();

        when(principalExtractor.extract(requestContext)).thenReturn(principal);
        when(policyStoreResolver.getCacheKey(principal)).thenReturn("customer-456");
        when(policyStoreResolver.resolvePolicyStore("customer-456")).thenReturn("PS-123");

        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.putSingle("docId", "doc-789");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(pathParams);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(requestContext.getPropertyNames()).thenReturn(Set.of());

        AuthorizationResult allowResult = AuthorizationResult.allow(List.of("policy-1"));
        when(authorizationService.isAuthorized(any(), any(), any(), any())).thenReturn(allowResult);

        // Should not throw
        filter.filter(requestContext);

        verify(authorizationService).isAuthorized(any(), any(), any(), any());
    }

    @Test
    void filter_shouldFailValidationWhenAttributeMissing() throws Exception {
        // Create extractor that doesn't provide required attribute
        ResourceExtractor<String> schemaExtractor = new ResourceExtractor<>() {
            @Override
            public String getResourceType() {
                return "Document";
            }

            @Override
            public ResourceEntity extract(String input, ResourceExtractionContext context) {
                return ResourceEntity.builder()
                    .entityType("Document")
                    .entityId(input)
                    .customerId("customer-123")
                    .attributes(Map.of(
                        "title", "Test Document"
                        // Missing "status" attribute
                    ))
                    .build();
            }

            @Override
            public Map<String, AttributeType> getAttributeSchema() {
                Map<String, AttributeType> schema = new HashMap<>();
                schema.put("title", AttributeType.STRING);
                schema.put("status", AttributeType.STRING);
                return schema;
            }
        };

        extractorRegistry = ResourceExtractorRegistry.builder()
            .register(schemaExtractor)
            .build();

        filter = CedarAuthorizationFilter.builder()
            .principalExtractor(principalExtractor)
            .policyStoreResolver(policyStoreResolver)
            .contextEntityType("Customer")
            .extractorRegistry(extractorRegistry)
            .authorizationService(authorizationService)
            .validateSchema(true)  // Enable validation
            .build();

        injectResourceInfo(filter, resourceInfo);

        Method method = TestResource.class.getMethod("getDocument", String.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        PrincipalEntity principal = PrincipalEntity.builder()
            .userId("user-123")
            .customerId("customer-456")
            .build();

        when(principalExtractor.extract(requestContext)).thenReturn(principal);
        when(policyStoreResolver.getCacheKey(principal)).thenReturn("customer-456");
        when(policyStoreResolver.resolvePolicyStore("customer-456")).thenReturn("PS-123");

        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.putSingle("docId", "doc-789");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(pathParams);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(requestContext.getPropertyNames()).thenReturn(Set.of());

        // Should abort with validation error
        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(response ->
            response.getStatus() == 500  // Internal Server Error for unexpected exceptions
        ));
    }

    @Test
    void filter_shouldFailValidationWhenAttributeTypeWrong() throws Exception {
        // Create extractor with wrong type
        ResourceExtractor<String> schemaExtractor = new ResourceExtractor<>() {
            @Override
            public String getResourceType() {
                return "Document";
            }

            @Override
            public ResourceEntity extract(String input, ResourceExtractionContext context) {
                return ResourceEntity.builder()
                    .entityType("Document")
                    .entityId(input)
                    .customerId("customer-123")
                    .attributes(Map.of(
                        "title", "Test Document",
                        "pageCount", "not-a-number"  // Should be Long, but it's String
                    ))
                    .build();
            }

            @Override
            public Map<String, AttributeType> getAttributeSchema() {
                Map<String, AttributeType> schema = new HashMap<>();
                schema.put("title", AttributeType.STRING);
                schema.put("pageCount", AttributeType.LONG);
                return schema;
            }
        };

        extractorRegistry = ResourceExtractorRegistry.builder()
            .register(schemaExtractor)
            .build();

        filter = CedarAuthorizationFilter.builder()
            .principalExtractor(principalExtractor)
            .policyStoreResolver(policyStoreResolver)
            .contextEntityType("Customer")
            .extractorRegistry(extractorRegistry)
            .authorizationService(authorizationService)
            .validateSchema(true)  // Enable validation
            .build();

        injectResourceInfo(filter, resourceInfo);

        Method method = TestResource.class.getMethod("getDocument", String.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        PrincipalEntity principal = PrincipalEntity.builder()
            .userId("user-123")
            .customerId("customer-456")
            .build();

        when(principalExtractor.extract(requestContext)).thenReturn(principal);
        when(policyStoreResolver.getCacheKey(principal)).thenReturn("customer-456");
        when(policyStoreResolver.resolvePolicyStore("customer-456")).thenReturn("PS-123");

        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.putSingle("docId", "doc-789");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(pathParams);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(requestContext.getPropertyNames()).thenReturn(Set.of());

        // Should abort with validation error
        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(response ->
            response.getStatus() == 500  // Internal Server Error for unexpected exceptions
        ));
    }

    @Test
    void filter_shouldFailValidationWhenExtraAttributePresent() throws Exception {
        // Create extractor with extra attribute not in schema
        ResourceExtractor<String> schemaExtractor = new ResourceExtractor<>() {
            @Override
            public String getResourceType() {
                return "Document";
            }

            @Override
            public ResourceEntity extract(String input, ResourceExtractionContext context) {
                return ResourceEntity.builder()
                    .entityType("Document")
                    .entityId(input)
                    .customerId("customer-123")
                    .attributes(Map.of(
                        "title", "Test Document",
                        "extraField", "not-in-schema"  // Not defined in schema
                    ))
                    .build();
            }

            @Override
            public Map<String, AttributeType> getAttributeSchema() {
                Map<String, AttributeType> schema = new HashMap<>();
                schema.put("title", AttributeType.STRING);
                return schema;
            }
        };

        extractorRegistry = ResourceExtractorRegistry.builder()
            .register(schemaExtractor)
            .build();

        filter = CedarAuthorizationFilter.builder()
            .principalExtractor(principalExtractor)
            .policyStoreResolver(policyStoreResolver)
            .contextEntityType("Customer")
            .extractorRegistry(extractorRegistry)
            .authorizationService(authorizationService)
            .validateSchema(true)  // Enable validation
            .build();

        injectResourceInfo(filter, resourceInfo);

        Method method = TestResource.class.getMethod("getDocument", String.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        PrincipalEntity principal = PrincipalEntity.builder()
            .userId("user-123")
            .customerId("customer-456")
            .build();

        when(principalExtractor.extract(requestContext)).thenReturn(principal);
        when(policyStoreResolver.getCacheKey(principal)).thenReturn("customer-456");
        when(policyStoreResolver.resolvePolicyStore("customer-456")).thenReturn("PS-123");

        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.putSingle("docId", "doc-789");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(pathParams);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(requestContext.getPropertyNames()).thenReturn(Set.of());

        // Should abort with validation error
        filter.filter(requestContext);

        verify(requestContext).abortWith(argThat(response ->
            response.getStatus() == 500  // Internal Server Error for unexpected exceptions
        ));
    }

    @Test
    void filter_shouldSkipValidationWhenDisabled() throws Exception {
        // Create extractor with invalid data
        ResourceExtractor<String> schemaExtractor = new ResourceExtractor<>() {
            @Override
            public String getResourceType() {
                return "Document";
            }

            @Override
            public ResourceEntity extract(String input, ResourceExtractionContext context) {
                return ResourceEntity.builder()
                    .entityType("Document")
                    .entityId(input)
                    .customerId("customer-123")
                    .attributes(Map.of(
                        "extraField", "not-in-schema"
                    ))
                    .build();
            }

            @Override
            public Map<String, AttributeType> getAttributeSchema() {
                Map<String, AttributeType> schema = new HashMap<>();
                schema.put("title", AttributeType.STRING);
                return schema;
            }
        };

        extractorRegistry = ResourceExtractorRegistry.builder()
            .register(schemaExtractor)
            .build();

        filter = CedarAuthorizationFilter.builder()
            .principalExtractor(principalExtractor)
            .policyStoreResolver(policyStoreResolver)
            .contextEntityType("Customer")
            .extractorRegistry(extractorRegistry)
            .authorizationService(authorizationService)
            .validateSchema(false)  // Disable validation
            .build();

        injectResourceInfo(filter, resourceInfo);

        Method method = TestResource.class.getMethod("getDocument", String.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        PrincipalEntity principal = PrincipalEntity.builder()
            .userId("user-123")
            .customerId("customer-456")
            .build();

        when(principalExtractor.extract(requestContext)).thenReturn(principal);
        when(policyStoreResolver.getCacheKey(principal)).thenReturn("customer-456");
        when(policyStoreResolver.resolvePolicyStore("customer-456")).thenReturn("PS-123");

        UriInfo uriInfo = mock(UriInfo.class);
        MultivaluedMap<String, String> pathParams = new MultivaluedHashMap<>();
        pathParams.putSingle("docId", "doc-789");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(pathParams);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(requestContext.getPropertyNames()).thenReturn(Set.of());

        AuthorizationResult allowResult = AuthorizationResult.allow(List.of("policy-1"));
        when(authorizationService.isAuthorized(any(), any(), any(), any())).thenReturn(allowResult);

        // Should not throw even though data is invalid
        filter.filter(requestContext);

        verify(authorizationService).isAuthorized(any(), any(), any(), any());
    }

    private void injectResourceInfo(CedarAuthorizationFilter filter, ResourceInfo resourceInfo) {
        try {
            Field resourceInfoField = CedarAuthorizationFilter.class.getDeclaredField("resourceInfo");
            resourceInfoField.setAccessible(true);
            resourceInfoField.set(filter, resourceInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Test resource class
    public static class TestResource {
        public void getDocument(
            @PathParam("docId")
            @CedarResource(type = "Document", actions = {"ViewDocument"})
            String docId
        ) {
        }
    }
}
