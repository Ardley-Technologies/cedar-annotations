package com.ardley.cedar.jaxrs;

import com.ardley.cedar.core.PrincipalEntity;

/**
 * Resolves which AWS Verified Permissions policy store to use for authorization.
 * Supports caching to avoid repeated lookups for the same principal.
 */
public interface PolicyStoreResolver {
    /**
     * Extract a cache key from the principal.
     * The filter will cache policy store IDs by this key.
     *
     * Example: return principal.getCustomerId()
     *
     * @param principal The authenticated principal
     * @return Cache key for this principal (typically customerId/tenantId)
     */
    String getCacheKey(PrincipalEntity principal);

    /**
     * Resolve policy store ID for the given cache key.
     * Only called on cache miss.
     *
     * @param cacheKey The cache key from getCacheKey()
     * @return AWS Verified Permissions policy store ID
     */
    String resolvePolicyStore(String cacheKey);
}
