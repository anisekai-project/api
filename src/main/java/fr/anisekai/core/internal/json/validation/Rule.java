package fr.anisekai.core.internal.json.validation;

import fr.anisekai.core.internal.json.AnisekaiArray;
import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.json.exceptions.JSONValidationException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Optional;

/**
 * Abstract base implementation of a {@link JsonRule} representing a validation rule applied to a specific key within a
 * JSON structure.
 * <p>
 * Each instance targets a particular JSON key and defines whether its presence is mandatory. Subclasses are expected to
 * implement the rule's specific logic, leveraging helper methods like {@link #retrieve(AnisekaiJson)},
 * {@link #asArray(Object, boolean)}, and {@link #asObject(Object, int)} for extraction and type validation.
 * <p>
 * If a required key is missing or its value is of an incompatible type, meaningful exceptions are thrown to aid in
 * debugging.
 */
public abstract class Rule implements JsonRule {

    private final String  key;
    private final boolean required;

    /**
     * Create a new {@link Rule} targeting the specified JSON key with a required flag.
     *
     * @param key
     *         The key in the JSON object this rule applies to.
     * @param required
     *         Whether the key is mandatory. If {@code true}, a missing or {@code null} value will cause validation to
     *         fail.
     */
    public Rule(String key, boolean required) {

        this.key      = key;
        this.required = required;
    }

    /**
     * Retrieve an optional object from the provided {@link AnisekaiJson} using this {@link Rule#getKey()}.
     *
     * @param source
     *         Source from which the object should be retrieved.
     *
     * @return An optional object
     */
    protected Optional<Object> retrieve(AnisekaiJson source) {

        Optional<Object> optional = source.getOptional(this.getKey());

        if (this.isRequired() && optional.isEmpty()) {
            throw new JSONValidationException(
                    this,
                    "The key was required but not found or null."
            );
        }

        return optional;
    }

    /**
     * Retrieve the provided object as an {@link AnisekaiArray}
     *
     * @param o
     *         The object
     * @param allowEmpty
     *         True if the {@link AnisekaiArray} can be empty.
     *
     * @return The {@link AnisekaiArray}
     *
     * @throws JSONException
     *         Threw if the provided object could not be retrieved as an {@link AnisekaiArray} or was empty when
     *         {@code allowEmpty} was set to false.
     */
    protected AnisekaiArray asArray(Object o, boolean allowEmpty) {

        AnisekaiArray array = switch (o) {
            case AnisekaiArray arr -> arr;
            case JSONArray arr -> new AnisekaiArray(arr);
            default -> throw new JSONValidationException(
                    this, String.format("Incompatible type '%s'.", o.getClass().getSimpleName())
            );
        };

        if (array.isEmpty() && !allowEmpty) {
            throw new JSONValidationException(this, "Empty array.");
        }

        return array;
    }

    /**
     * Retrieve the provided object as {@link AnisekaiJson}.
     *
     * @param o
     *         The object
     * @param index
     *         Index at which the object was encountered. Used only for error message purpose.
     *
     * @return The {@link AnisekaiJson}.
     *
     * @throws JSONException
     *         Threw if the provided object could not be retrieved as an {@link AnisekaiJson}.
     */
    protected AnisekaiJson asObject(Object o, int index) {

        return switch (o) {
            case AnisekaiJson obj -> obj;
            case JSONObject obj -> new AnisekaiJson(obj);
            default -> throw new JSONValidationException(this, String.format("Not an object at index %s.", index));
        };
    }

    @Override
    public String getKey() {

        return this.key;
    }

    @Override
    public boolean isRequired() {

        return this.required;
    }

}
