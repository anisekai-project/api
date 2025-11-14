package fr.anisekai.core.internal.json.validation;

import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.json.exceptions.JSONValidationException;
import org.json.JSONException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Specific implementation of a {@link JsonRule} allowing to validate an {@link AnisekaiJson} key.
 */
public class JsonObjectRule extends Rule {

    private static final String RULE_FORMAT = "[[ object '%s' is %s with type(s) (%s) ]]";
    private static final String RULE_EX_MSG = "JSON rule %s failed: Incompatible type '%s'.";

    private final Class<?>[] allowedTypes;

    /**
     * Create a new {@link JsonObjectRule}
     *
     * @param key
     *         The key to check for in an {@link AnisekaiJson}
     * @param required
     *         Define if the key must be defined.
     * @param allowedTypes
     *         Array of all types allowed for this key.
     */
    public JsonObjectRule(String key, boolean required, Class<?>... allowedTypes) {

        super(key, required);
        this.allowedTypes = allowedTypes;
    }

    /**
     * Validate the provided object against this {@link JsonRule}.
     *
     * @param json
     *         The object to validate.
     *
     * @throws JSONException
     *         Thrown if the current {@link JsonRule} failed.
     */
    @Override
    public void validate(AnisekaiJson json) {

        this.retrieve(json).ifPresent(obj -> {
            if (Arrays.stream(this.allowedTypes).noneMatch(type -> type.isInstance(obj))) {
                throw new JSONValidationException(
                        this,
                        String.format(RULE_EX_MSG, this, obj.getClass().getSimpleName())
                );
            }
        });
    }

    @Override
    public String getName() {

        String requireState = this.isRequired() ? "required" : "not required";

        String names = Arrays.stream(this.allowedTypes)
                             .map(Class::getSimpleName)
                             .collect(Collectors.joining(", "));

        return String.format(RULE_FORMAT, this.getKey(), requireState, names);
    }


}
