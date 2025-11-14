package fr.anisekai.core.internal.json.exceptions;


import fr.anisekai.core.internal.json.validation.JsonRule;

/**
 * Exception representing a failure in validating a {@link JsonRule}.
 */
public class JSONValidationException extends RuntimeException {

    /**
     * Create a new {@link JSONValidationException}.
     *
     * @param failingRule
     *         The {@link JsonRule} that failed its validation.
     * @param reason
     *         The reason for which the validation failed.
     */
    public JSONValidationException(JsonRule failingRule, String reason) {

        super(String.format("Rule %s validation failed: %s", failingRule.getName(), reason));
    }

    /**
     * Create a new {@link JSONValidationException}.
     *
     * @param failingRule
     *         The {@link JsonRule} that failed its validation.
     * @param reason
     *         The reason for which the validation failed.
     * @param cause
     *         The underlying {@link JSONValidationException} that cascade the validation failure.
     */
    public JSONValidationException(JsonRule failingRule, String reason, JSONValidationException cause) {

        super(String.format("Rule %s validation failed: %s", failingRule.getName(), reason), cause);
    }

}
