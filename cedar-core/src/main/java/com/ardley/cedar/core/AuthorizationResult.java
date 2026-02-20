package com.ardley.cedar.core;

import java.util.List;

/**
 * Result of a Cedar authorization request.
 */
public class AuthorizationResult {
    private final Decision decision;
    private final List<String> determiningPolicies;
    private final List<String> errors;

    public AuthorizationResult(Decision decision, List<String> determiningPolicies, List<String> errors) {
        this.decision = decision;
        this.determiningPolicies = determiningPolicies;
        this.errors = errors;
    }

    public Decision getDecision() {
        return decision;
    }

    public boolean isAllowed() {
        return decision == Decision.ALLOW;
    }

    public boolean isDenied() {
        return decision == Decision.DENY;
    }

    public List<String> getDeterminingPolicies() {
        return determiningPolicies;
    }

    public List<String> getErrors() {
        return errors;
    }

    public enum Decision {
        ALLOW,
        DENY
    }

    public static AuthorizationResult allow(List<String> determiningPolicies) {
        return new AuthorizationResult(Decision.ALLOW, determiningPolicies, List.of());
    }

    public static AuthorizationResult deny(List<String> determiningPolicies, List<String> errors) {
        return new AuthorizationResult(Decision.DENY, determiningPolicies, errors);
    }
}
