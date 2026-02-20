package com.ardley.cedar.jaxrs;

import com.ardley.cedar.core.ResourceExtractor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code @PathParam} or {@code @QueryParam} as a Cedar resource requiring authorization.
 * The filter extracts the parameter value and checks all specified actions against the resource.
 *
 * <p><strong>Important:</strong> This annotation can ONLY be used on parameters annotated
 * with {@code @PathParam} or {@code @QueryParam}.
 *
 * <p>Single resource:
 * <pre>{@code
 * @GET
 * @Path("/users/{userId}")
 * @CedarSecured
 * public Response getUser(
 *     @PathParam("userId")
 *     @CedarResource(type = "User", actions = {"ViewUser"})
 *     String userId
 * ) { ... }
 * }</pre>
 *
 * <p>Multiple resources:
 * <pre>{@code
 * @POST
 * @Path("/documents/{docId}/transfer")
 * @CedarSecured
 * public Response transfer(
 *     @PathParam("docId")
 *     @CedarResource(type = "Document", actions = {"ViewDocument", "DeleteDocument"})
 *     String docId,
 *
 *     @QueryParam("toLoanAppId")
 *     @CedarResource(type = "LoanApplication", actions = {"ViewLoanApplication"})
 *     String toLoanAppId
 * ) { ... }
 * }</pre>
 *
 * <p>Bulk operations (List):
 * <pre>{@code
 * @DELETE
 * @Path("/documents/bulk")
 * @CedarSecured
 * public Response bulkDelete(
 *     @QueryParam("docIds")
 *     @CedarResource(type = "Document", actions = {"ViewDocument", "DeleteDocument"})
 *     List<String> docIds
 * ) { ... }
 * }</pre>
 *
 * <p>Request body with custom extractor:
 * <pre>{@code
 * @POST
 * @Path("/reports")
 * @CedarSecured
 * @RequiresActions({"CreateReport"})
 * public Response createReport(
 *     @CedarResource(
 *         type = "LoanApplication",
 *         actions = {"ViewLoanApplication"},
 *         extractor = CreateReportLoanAppExtractor.class
 *     )
 *     @Valid CreateReportRequest request
 * ) { ... }
 * }</pre>
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CedarResource {

    /**
     * Cedar resource type (e.g., "User", "LoanApplication", "Document").
     * Must match a resource type in your Cedar schema.
     *
     * @return Resource type name
     */
    String type();

    /**
     * Cedar actions required for this resource (ALL must be permitted).
     *
     * @return Array of action names
     */
    String[] actions();

    /**
     * Custom extractor class to use instead of registry lookup.
     * If not specified (default), uses the extractor registered for this resource type.
     *
     * <p>The extractor's generic type T must match the parameter type:
     * <ul>
     *   <li>For {@code @PathParam}/{@code @QueryParam}: T = String</li>
     *   <li>For request body: T = request body class</li>
     * </ul>
     *
     * @return Extractor class, or DefaultResourceExtractor.class to use registry
     */
    Class<? extends ResourceExtractor<?>> extractor() default DefaultResourceExtractor.class;

    /**
     * Whether this parameter is required.
     * If true and parameter is null/empty, returns 400 Bad Request.
     * If false and parameter is null/empty, skips authorization check.
     *
     * @return true if required (default), false if optional
     */
    boolean required() default true;

    /**
     * Marker interface indicating "use registry extractor".
     * This is the default value for {@link #extractor()}.
     */
    interface DefaultResourceExtractor extends ResourceExtractor<Void> {
    }
}
