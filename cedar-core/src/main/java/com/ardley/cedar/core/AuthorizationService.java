package com.ardley.cedar.core;

import java.util.List;

/**
 * Generic interface for Cedar authorization services.
 * Implementations handle communication with Cedar policy engines (AWS Verified Permissions, Cedar standalone, etc.)
 */
public interface AuthorizationService {

    /**
     * Check if a principal is authorized to perform an action on a resource.
     *
     * @param policyStoreId Policy store identifier (e.g., AWS policy store ID)
     * @param principal The authenticated user
     * @param action The action being performed
     * @param resource The resource being accessed
     * @return Authorization result (ALLOW or DENY)
     */
    AuthorizationResult isAuthorized(
        String policyStoreId,
        PrincipalEntity principal,
        String action,
        ResourceEntity resource
    );

    /**
     * Batch authorization check for multiple requests.
     * More efficient than individual calls for large lists.
     *
     * @param policyStoreId Policy store identifier
     * @param requests List of authorization requests
     * @return List of results in same order as requests
     */
    List<AuthorizationResult> isAuthorizedBatch(
        String policyStoreId,
        List<AuthorizationRequest> requests
    );

    /**
     * Deploy a Cedar schema to the policy store.
     *
     * @param cacheKey Cache key for the policy store (e.g., customerId)
     * @param policyStoreId Policy store identifier
     * @param schemaJson Cedar schema in JSON format
     */
    void deploySchema(String cacheKey, String policyStoreId, String schemaJson);

    /**
     * Single authorization request for batch operations.
     */
    class AuthorizationRequest {
        private final PrincipalEntity principal;
        private final String action;
        private final ResourceEntity resource;

        public AuthorizationRequest(PrincipalEntity principal, String action, ResourceEntity resource) {
            this.principal = principal;
            this.action = action;
            this.resource = resource;
        }

        public static AuthorizationRequest of(PrincipalEntity principal, String action, ResourceEntity resource) {
            return new AuthorizationRequest(principal, action, resource);
        }

        public PrincipalEntity getPrincipal() {
            return principal;
        }

        public String getAction() {
            return action;
        }

        public ResourceEntity getResource() {
            return resource;
        }
    }
}
