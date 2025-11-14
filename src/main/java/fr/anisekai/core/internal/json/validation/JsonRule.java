package fr.anisekai.core.internal.json.validation;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.json.exceptions.JSONValidationException;

/**
 * Interface representing a validation rule for an {@link AnisekaiJson}.
 */
public interface JsonRule {

    /**
     * Validate the provided object against this {@link JsonRule}.
     *
     * @param json
     *         The object to validate.
     *
     * @throws JSONValidationException
     *         Thrown if the current {@link JsonRule} failed.
     */
    void validate(AnisekaiJson json) throws JSONValidationException;

    /**
     * Get the key that should be used when checking an {@link AnisekaiJson}.
     *
     * @return A key
     */
    String getKey();

    /**
     * Check if the key of this rule must exist.
     *
     * @return True for enforce the check, false otherwise.
     */
    boolean isRequired();

    /**
     * Retrieve this {@link JsonRule} name. This should be a human-readable name so that any developer can quickly find
     * the rule in case it fails.
     *
     * @return This {@link JsonRule} name.
     */
    String getName();

}
