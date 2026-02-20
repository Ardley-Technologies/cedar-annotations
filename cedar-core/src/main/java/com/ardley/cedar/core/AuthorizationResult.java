package com.ardley.cedar.core;

import java.util.List;

/**
 * Result of a Cedar authorization request.
 */
public class AuthorizationResult {
    private final Decision decision;
    private final List<String> determiningPolicies;
    private final List<String> errors;

    /**
     * Constructs an authorization result.
     *
     * @param decision The authorization decision
     * @param determiningPolicies List of policy IDs that influenced this decision
     * @param errors List of error messages if the decision was DENY
     */
    public AuthorizationResult(Decision decision, List<String> determiningPolicies, List<String> errors) {
        this.decision = decision;
        this.determiningPolicies = determiningPolicies;
        this.errors = errors;
    }

    /**
     * Gets the authorization decision.
     *
     * @return The decision (ALLOW or DENY)
     */
    public Decision getDecision() {
        return decision;
    }

    /**
     * Checks if the authorization was allowed.
     *
     * @return true if allowed, false otherwise
     */
    public boolean isAllowed() {
        return decision == Decision.ALLOW;
    }

    /**
     * Checks if the authorization was denied.
     *
     * @return true if denied, false otherwise
     */
    public boolean isDenied() {
        return decision == Decision.DENY;
    }

    /**
     * Gets the list of policies that determined this result.
     *
     * @return List of policy IDs
     */
    public List<String> getDeterminingPolicies() {
        return determiningPolicies;
    }

    /**
     * Gets any error messages associated with this result.
     *
     * @return List of error messages (empty if allowed)
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Authorization decision enumeration.
     */
    public enum Decision {
        /** Authorization was allowed */
        ALLOW,
        /** Authorization was denied */
        DENY
    }

    /**
     * Creates an ALLOW result.
     *
     * @param determiningPolicies List of policy IDs that allowed this request
     * @return An ALLOW authorization result
     */
    public static AuthorizationResult allow(List<String> determiningPolicies) {
        return new AuthorizationResult(Decision.ALLOW, determiningPolicies, List.of());
    }

    /**
     * Creates a DENY result.
     *
     * @param determiningPolicies List of policy IDs that denied this request
     * @param errors List of error messages explaining why access was denied
     * @return A DENY authorization result
     */
    public static AuthorizationResult deny(List<String> determiningPolicies, List<String> errors) {
        return new AuthorizationResult(Decision.DENY, determiningPolicies, errors);
    }
}
