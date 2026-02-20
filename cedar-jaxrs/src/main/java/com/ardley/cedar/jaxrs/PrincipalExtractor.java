package com.ardley.cedar.jaxrs;

import com.ardley.cedar.core.PrincipalEntity;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Extracts the authenticated principal from JAX-RS request context.
 * Implementations can extract from JWT claims, session, database, etc.
 */
@FunctionalInterface
public interface PrincipalExtractor {
    /**
     * Extract principal from request context.
     *
     * @param context JAX-RS request context
     * @return Principal entity with userId, customerId, role, etc.
     * @throws jakarta.ws.rs.NotAuthorizedException if principal cannot be extracted
     */
    PrincipalEntity extract(ContainerRequestContext context);
}
