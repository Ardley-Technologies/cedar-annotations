package com.ardley.cedar.jaxrs;

import com.ardley.cedar.core.*;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CedarAuthorizationFilterTest {

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

        // Create mock extractor
        ResourceExtractor<String> mockExtractor = new ResourceExtractor<>() {
            @Override
            public String getResourceType() {
                return "User";
            }

            @Override
            public ResourceEntity extract(String input, ResourceExtractionContext context) {
                return ResourceEntity.builder()
                    .entityType("User")
                    .entityId(input)
                    .customerId("customer-123")
                    .build();
            }
        };

        extractorRegistry = ResourceExtractorRegistry.builder()
            .register(mockExtractor)
            .build();

        filter = CedarAuthorizationFilter.builder()
            .principalExtractor(principalExtractor)
            .policyStoreResolver(policyStoreResolver)
            .contextEntityType("Customer")
            .extractorRegistry(extractorRegistry)
            .authorizationService(authorizationService)
            .build();

        // Inject resourceInfo via reflection (since @Context isn't available in tests)
        try {
            Field resourceInfoField = CedarAuthorizationFilter.class.getDeclaredField("resourceInfo");
            resourceInfoField.setAccessible(true);
            resourceInfoField.set(filter, resourceInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void filter_shouldSkipIfNoAnnotationsPresent() throws Exception {
        Method method = TestResource.class.getMethod("noAnnotations");
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        filter.filter(requestContext);

        verify(principalExtractor, never()).extract(any());
    }

    @Test
    void filter_shouldCheckContextActions() throws Exception {
        Method method = TestResource.class.getMethod("contextAction");
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        PrincipalEntity principal = PrincipalEntity.builder()
            .userId("user-123")
            .customerId("customer-456")
            .build();

        when(principalExtractor.extract(requestContext)).thenReturn(principal);
        when(policyStoreResolver.getCacheKey(principal)).thenReturn("customer-456");
        when(policyStoreResolver.resolvePolicyStore("customer-456")).thenReturn("PS-123");

        AuthorizationResult allowResult = AuthorizationResult.allow(List.of("policy-1"));
        when(authorizationService.isAuthorized(
            eq("PS-123"),
            eq(principal),
            eq("CreateUser"),
            any(ResourceEntity.class)
        )).thenReturn(allowResult);

        filter.filter(requestContext);

        verify(authorizationService).isAuthorized(
            eq("PS-123"),
            eq(principal),
            eq("CreateUser"),
            argThat(resource ->
                resource.getEntityType().equals("Customer") &&
                resource.getEntityId().equals("customer-456")
            )
        );
    }

    @Test
    void filter_shouldDenyIfContextActionDenied() throws Exception {
        Method method = TestResource.class.getMethod("contextAction");
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        PrincipalEntity principal = PrincipalEntity.builder()
            .userId("user-123")
            .customerId("customer-456")
            .build();

        when(principalExtractor.extract(requestContext)).thenReturn(principal);
        when(policyStoreResolver.getCacheKey(principal)).thenReturn("customer-456");
        when(policyStoreResolver.resolvePolicyStore("customer-456")).thenReturn("PS-123");

        AuthorizationResult denyResult = AuthorizationResult.deny(
            List.of("policy-1"),
            List.of("Insufficient permissions")
        );
        when(authorizationService.isAuthorized(any(), any(), any(), any())).thenReturn(denyResult);

        filter.filter(requestContext);

        ArgumentCaptor<Response> responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(requestContext).abortWith(responseCaptor.capture());

        Response response = responseCaptor.getValue();
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    void filter_shouldCheckResourceFromPathParam() throws Exception {
        Method method = TestResource.class.getMethod("getUser", String.class);
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
        pathParams.putSingle("userId", "user-789");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(pathParams);
        when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());
        when(requestContext.getPropertyNames()).thenReturn(Set.of());

        AuthorizationResult allowResult = AuthorizationResult.allow(List.of("policy-1"));
        when(authorizationService.isAuthorized(any(), any(), any(), any())).thenReturn(allowResult);

        filter.filter(requestContext);

        verify(authorizationService).isAuthorized(
            eq("PS-123"),
            eq(principal),
            eq("ViewUser"),
            argThat(resource ->
                resource.getEntityType().equals("User") &&
                resource.getEntityId().equals("user-789")
            )
        );
    }

    @Test
    void filter_shouldCachePolicyStoreId() throws Exception {
        Method method = TestResource.class.getMethod("contextAction");
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        PrincipalEntity principal = PrincipalEntity.builder()
            .userId("user-123")
            .customerId("customer-456")
            .build();

        when(principalExtractor.extract(requestContext)).thenReturn(principal);
        when(policyStoreResolver.getCacheKey(principal)).thenReturn("customer-456");
        when(policyStoreResolver.resolvePolicyStore("customer-456")).thenReturn("PS-123");

        AuthorizationResult allowResult = AuthorizationResult.allow(List.of("policy-1"));
        when(authorizationService.isAuthorized(any(), any(), any(), any())).thenReturn(allowResult);

        // First call
        filter.filter(requestContext);

        // Second call with same principal
        filter.filter(requestContext);

        // Policy store should only be resolved once (cached)
        verify(policyStoreResolver, times(1)).resolvePolicyStore("customer-456");
    }

    // Test resource class
    public static class TestResource {
        public void noAnnotations() {
        }

        @RequiresActions({"CreateUser"})
        public void contextAction() {
        }

        public void getUser(
            @PathParam("userId")
            @CedarResource(type = "User", actions = {"ViewUser"})
            String userId
        ) {
        }
    }
}
