package com.ardley.cedar.jaxrs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares context-based Cedar actions required for a method.
 * Checks permissions against the authenticated principal's tenant/organization resource.
 *
 * <p>The resource type checked is configured in the CedarAuthorizationFilter
 * (typically "Customer", "Organization", or "Tenant").
 *
 * <p>Usage:
 * <pre>{@code
 * @POST
 * @Path("/users")
 * @CedarSecured
 * @RequiresActions({"CreateUser"})
 * public Response createUser(@Valid UserRequest request) {
 *     // Filter checks: Can principal CreateUser in their tenant?
 * }
 * }</pre>
 *
 * <p>Multiple actions (ALL must pass):
 * <pre>{@code
 * @GET
 * @Path("/settings")
 * @CedarSecured
 * @RequiresActions({"ViewCustomerSettings", "ViewBilling"})
 * public Response getSettings() {
 *     // Both actions must be authorized
 * }
 * }</pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresActions {

    /**
     * Cedar actions required (ALL must be permitted).
     * Use constants from your Actions class.
     *
     * @return Array of action names
     */
    String[] value();
}
