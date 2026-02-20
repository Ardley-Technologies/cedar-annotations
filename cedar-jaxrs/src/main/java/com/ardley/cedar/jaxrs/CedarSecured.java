package com.ardley.cedar.jaxrs;

import jakarta.ws.rs.NameBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Name binding annotation that marks JAX-RS resources or methods for Cedar authorization.
 * When applied, the CedarAuthorizationFilter will intercept requests to perform authorization checks.
 *
 * <p>Usage:
 * <pre>{@code
 * @Path("/users")
 * @CedarSecured
 * public class UserResource {
 *     // All methods require Cedar authorization
 * }
 * }</pre>
 */
@NameBinding
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CedarSecured {
}
