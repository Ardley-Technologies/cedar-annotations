package com.ardley.cedar.jaxrs;

import com.ardley.cedar.core.*;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Priority;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JAX-RS filter that performs Cedar authorization for endpoints annotated with @CedarSecured.
 * Runs at AUTHORIZATION priority (2000) after authentication but before business logic.
 */
@Provider
@CedarSecured
@Priority(Priorities.AUTHORIZATION)
public class CedarAuthorizationFilter implements ContainerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(CedarAuthorizationFilter.class);

    @Context
    private ResourceInfo resourceInfo;

    private final PrincipalExtractor principalExtractor;
    private final PolicyStoreResolver policyStoreResolver;
    private final String contextEntityType;
    private final ResourceExtractorRegistry extractorRegistry;
    private final AuthorizationService authorizationService;
    private final Map<String, String> policyStoreCache;
    private final boolean validateSchema;
    private final boolean includeResourceIdInErrors;

    private CedarAuthorizationFilter(Builder builder) {
        this.principalExtractor = Objects.requireNonNull(builder.principalExtractor, "principalExtractor is required");
        this.policyStoreResolver = Objects.requireNonNull(builder.policyStoreResolver, "policyStoreResolver is required");
        this.contextEntityType = Objects.requireNonNull(builder.contextEntityType, "contextEntityType is required");
        this.extractorRegistry = Objects.requireNonNull(builder.extractorRegistry, "extractorRegistry is required");
        this.authorizationService = Objects.requireNonNull(builder.authorizationService, "authorizationService is required");
        this.policyStoreCache = new ConcurrentHashMap<>();
        this.validateSchema = builder.validateSchema;
        this.includeResourceIdInErrors = builder.includeResourceIdInErrors;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void filter(ContainerRequestContext ctx) {
        Method method = resourceInfo.getResourceMethod();
        if (method == null) return;

        // Only process if Cedar annotations present
        if (!requiresAuthorization(method)) return;

        try {
            // 1. Extract principal
            PrincipalEntity principal = principalExtractor.extract(ctx);

            // 2. Resolve policy store (with caching)
            String policyStoreId = resolvePolicyStoreWithCache(principal);

            // 3. Check context-based actions
            checkContextActions(method, principal, policyStoreId);

            // 4. Check parameter-based resources
            checkParameterResources(method, ctx, principal, policyStoreId);

        } catch (NotAuthorizedException e) {
            log.warn("Authorization failed: {}", e.getMessage());
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "Unauthorized", "message", e.getMessage()))
                .build());
        } catch (AuthorizationException e) {
            log.warn("Authorization denied: {}", e.getMessage());
            ctx.abortWith(Response.status(Response.Status.FORBIDDEN)
                .entity(Map.of("error", "Forbidden", "message", e.getMessage()))
                .build());
        } catch (Exception e) {
            log.error("Unexpected error during Cedar authorization", e);
            ctx.abortWith(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Internal Server Error", "message", "Authorization check failed"))
                .build());
        }
    }

    private boolean requiresAuthorization(Method method) {
        if (method.getAnnotation(RequiresActions.class) != null) return true;

        for (Parameter param : method.getParameters()) {
            if (param.getAnnotation(CedarResource.class) != null) return true;
        }

        return false;
    }

    private String resolvePolicyStoreWithCache(PrincipalEntity principal) {
        String cacheKey = policyStoreResolver.getCacheKey(principal);

        return policyStoreCache.computeIfAbsent(cacheKey, key -> {
            log.debug("Policy store cache miss for key: {}", key);
            String policyStoreId = policyStoreResolver.resolvePolicyStore(key);
            log.debug("Resolved policy store: {} for key: {}", policyStoreId, key);
            return policyStoreId;
        });
    }

    private void checkContextActions(Method method, PrincipalEntity principal, String policyStoreId) {
        RequiresActions annotation = method.getAnnotation(RequiresActions.class);
        if (annotation == null) return;

        // Create context resource using configured entity type
        ResourceEntity contextResource = ResourceEntity.builder()
            .entityType(contextEntityType)
            .entityId(principal.getCustomerId())
            .customerId(principal.getCustomerId())
            .attributes(Map.of())
            .build();

        // Check all actions
        for (String action : annotation.value()) {
            AuthorizationResult result = authorizationService.isAuthorized(
                policyStoreId, principal, action, contextResource
            );

            if (result.isDenied()) {
                throw new AuthorizationException(
                    "You are not authorized to perform \"" + action + "\"."
                );
            }
        }

        log.debug("Context actions authorized: {}", Arrays.toString(annotation.value()));
    }

    private void checkParameterResources(
        Method method,
        ContainerRequestContext ctx,
        PrincipalEntity principal,
        String policyStoreId
    ) {
        Parameter[] parameters = method.getParameters();

        for (Parameter param : parameters) {
            CedarResource annotation = param.getAnnotation(CedarResource.class);
            if (annotation == null) continue;

            // Validate parameter has @PathParam or @QueryParam
            validateParameterType(param);

            // Get extractor (custom or registry)
            ResourceExtractor<?> extractor = getExtractorForParameter(param, annotation);

            // Extract parameter value
            Object paramValue = extractParameterValue(ctx, param);

            // Handle null/empty
            if (paramValue == null || isEmpty(paramValue)) {
                if (annotation.required()) {
                    throw new IllegalArgumentException(
                        "Required parameter is missing"
                    );
                }
                continue;
            }

            // Handle lists
            List<?> values = toList(paramValue);

            // Check each value
            for (Object value : values) {
                checkResource(
                    extractor,
                    value,
                    ctx,
                    annotation.type(),
                    annotation.actions(),
                    principal,
                    policyStoreId
                );
            }
        }
    }

    private void validateParameterType(Parameter param) {
        boolean hasPathParam = param.getAnnotation(PathParam.class) != null;
        boolean hasQueryParam = param.getAnnotation(QueryParam.class) != null;

        if (!hasPathParam && !hasQueryParam) {
            throw new IllegalStateException(
                "@CedarResource must be used with @PathParam or @QueryParam. " +
                "Found on parameter: " + param.getName()
            );
        }
    }

    private ResourceExtractor<?> getExtractorForParameter(Parameter param, CedarResource annotation) {
        Class<? extends ResourceExtractor<?>> extractorClass = annotation.extractor();

        if (extractorClass != CedarResource.DefaultResourceExtractor.class) {
            // Custom extractor specified
            try {
                return extractorClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to instantiate custom extractor: " + extractorClass.getName() +
                    ". Extractors must have a no-arg constructor and be instantiated " +
                    "with dependencies via the registry during filter setup.", e
                );
            }
        }

        // Use registry extractor
        return extractorRegistry.getExtractor(annotation.type());
    }

    private Object extractParameterValue(ContainerRequestContext ctx, Parameter param) {
        PathParam pathParam = param.getAnnotation(PathParam.class);
        if (pathParam != null) {
            return ctx.getUriInfo().getPathParameters().getFirst(pathParam.value());
        }

        QueryParam queryParam = param.getAnnotation(QueryParam.class);
        if (queryParam != null) {
            Class<?> paramType = param.getType();
            if (List.class.isAssignableFrom(paramType)) {
                return ctx.getUriInfo().getQueryParameters().get(queryParam.value());
            } else {
                return ctx.getUriInfo().getQueryParameters().getFirst(queryParam.value());
            }
        }

        return null;
    }

    private boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String) return ((String) value).isEmpty();
        if (value instanceof List) return ((List<?>) value).isEmpty();
        return false;
    }

    private List<?> toList(Object value) {
        if (value instanceof List) {
            return (List<?>) value;
        } else {
            return List.of(value);
        }
    }

    @SuppressWarnings("unchecked")
    private void checkResource(
        ResourceExtractor<?> extractor,
        Object input,
        ContainerRequestContext ctx,
        String resourceType,
        String[] actions,
        PrincipalEntity principal,
        String policyStoreId
    ) {
        // Build extraction context
        ResourceExtractionContext extractionCtx = buildExtractionContext(ctx);

        // Extract resource (type-safe via generics)
        ResourceExtractor<Object> typedExtractor = (ResourceExtractor<Object>) extractor;
        ResourceEntity resource = typedExtractor.extract(input, extractionCtx);

        // Validate attributes match schema (optional)
        if (validateSchema) {
            validateResourceAttributes(resource, extractor);
        }

        // Check all actions
        for (String action : actions) {
            AuthorizationResult result = authorizationService.isAuthorized(
                policyStoreId, principal, action, resource
            );

            if (result.isDenied()) {
                String errorMessage = includeResourceIdInErrors
                    ? String.format("You are not authorized to perform \"%s\" on %s \"%s\".",
                        action, resourceType, resource.getEntityId())
                    : String.format("You are not authorized to perform \"%s\" on this resource.", action);

                throw new AuthorizationException(errorMessage);
            }
        }

        log.debug("Resource actions authorized: resource={}, id={}, actions={}",
            resourceType, resource.getEntityId(), Arrays.toString(actions));
    }

    private ResourceExtractionContext buildExtractionContext(ContainerRequestContext ctx) {
        return ResourceExtractionContext.builder()
            .pathParameters(toSingleValueMap(ctx.getUriInfo().getPathParameters()))
            .queryParameters(toSingleValueMap(ctx.getUriInfo().getQueryParameters()))
            .properties(extractProperties(ctx))
            .build();
    }

    private Map<String, String> toSingleValueMap(jakarta.ws.rs.core.MultivaluedMap<String, String> multivaluedMap) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : multivaluedMap.entrySet()) {
            List<String> values = entry.getValue();
            if (values != null && !values.isEmpty()) {
                result.put(entry.getKey(), values.get(0));
            }
        }
        return result;
    }

    private Map<String, Object> extractProperties(ContainerRequestContext ctx) {
        Map<String, Object> properties = new HashMap<>();
        for (String propertyName : ctx.getPropertyNames()) {
            properties.put(propertyName, ctx.getProperty(propertyName));
        }
        return properties;
    }

    private void validateResourceAttributes(ResourceEntity resource, ResourceExtractor<?> extractor) {
        Map<String, ResourceExtractor.AttributeType> schema = extractor.getAttributeSchema();
        if (schema.isEmpty()) {
            return; // No schema defined, skip validation
        }

        Map<String, Object> attributes = resource.getAttributes();

        // Check for extra attributes not in schema
        for (String attrName : attributes.keySet()) {
            if (!schema.containsKey(attrName)) {
                throw new IllegalStateException(
                    String.format("Resource %s has attribute '%s' not defined in schema. Defined attributes: %s",
                        resource.getEntityType(), attrName, schema.keySet())
                );
            }
        }

        // Check for missing required attributes and validate types
        for (Map.Entry<String, ResourceExtractor.AttributeType> entry : schema.entrySet()) {
            String attrName = entry.getKey();
            ResourceExtractor.AttributeType expectedType = entry.getValue();

            if (!attributes.containsKey(attrName)) {
                throw new IllegalStateException(
                    String.format("Resource %s missing required attribute '%s' defined in schema",
                        resource.getEntityType(), attrName)
                );
            }

            Object attrValue = attributes.get(attrName);
            if (attrValue != null) {
                validateAttributeType(resource.getEntityType(), attrName, attrValue, expectedType);
            }
        }
    }

    private void validateAttributeType(String resourceType, String attrName, Object value,
                                        ResourceExtractor.AttributeType expectedType) {
        boolean valid = false;

        switch (expectedType) {
            case STRING:
                valid = value instanceof String;
                break;
            case LONG:
                valid = value instanceof Long || value instanceof Integer;
                break;
            case BOOLEAN:
                valid = value instanceof Boolean;
                break;
            case SET_STRING:
                if (value instanceof Set) {
                    Set<?> set = (Set<?>) value;
                    valid = set.isEmpty() || set.iterator().next() instanceof String;
                }
                break;
            case SET_LONG:
                if (value instanceof Set) {
                    Set<?> set = (Set<?>) value;
                    valid = set.isEmpty() ||
                            (set.iterator().next() instanceof Long || set.iterator().next() instanceof Integer);
                }
                break;
        }

        if (!valid) {
            throw new IllegalStateException(
                String.format("Resource %s attribute '%s' has invalid type. Expected: %s, Actual: %s",
                    resourceType, attrName, expectedType, value.getClass().getSimpleName())
            );
        }
    }

    /**
     * Builder for CedarAuthorizationFilter.
     */
    public static class Builder {
        private PrincipalExtractor principalExtractor;
        private PolicyStoreResolver policyStoreResolver;
        private String contextEntityType = "Customer";
        private ResourceExtractorRegistry extractorRegistry;
        private AuthorizationService authorizationService;
        private boolean validateSchema = false;
        private boolean includeResourceIdInErrors = false;

        public Builder principalExtractor(PrincipalExtractor extractor) {
            this.principalExtractor = extractor;
            return this;
        }

        public Builder policyStoreResolver(PolicyStoreResolver resolver) {
            this.policyStoreResolver = resolver;
            return this;
        }

        public Builder contextEntityType(String type) {
            this.contextEntityType = type;
            return this;
        }

        public Builder extractorRegistry(ResourceExtractorRegistry registry) {
            this.extractorRegistry = registry;
            return this;
        }

        public Builder authorizationService(AuthorizationService service) {
            this.authorizationService = service;
            return this;
        }

        public Builder validateSchema(boolean validate) {
            this.validateSchema = validate;
            return this;
        }

        public Builder includeResourceIdInErrors(boolean include) {
            this.includeResourceIdInErrors = include;
            return this;
        }

        public CedarAuthorizationFilter build() {
            return new CedarAuthorizationFilter(this);
        }
    }

    /**
     * Exception thrown when authorization is denied.
     */
    public static class AuthorizationException extends RuntimeException {
        public AuthorizationException(String message) {
            super(message);
        }
    }
}
