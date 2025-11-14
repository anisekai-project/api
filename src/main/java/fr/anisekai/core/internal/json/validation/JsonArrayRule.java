package fr.anisekai.core.internal.json.validation;


import fr.anisekai.core.internal.json.AnisekaiArray;
import fr.anisekai.core.internal.json.AnisekaiJson;
import fr.anisekai.core.internal.json.exceptions.JSONValidationException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Specific implementation of a {@link JsonRule} allowing to validate an {@link AnisekaiArray} content.
 */
public class JsonArrayRule extends Rule {

    private static final String RULE_FORMAT = "[[ array '%s' is %s and %s containing type(s) (%s) ]]";
    private static final String RULE_EX_MSG = "JSON rule %s failed at index %s: Incompatible type %s.";

    private final boolean    allowEmpty;
    private final Class<?>[] allowedTypes;


    /**
     * Create a new {@link JsonArrayObjectRule} instance.
     *
     * @param key
     *         The key in the {@link AnisekaiJson} into which the {@link AnisekaiArray} is expected.
     * @param required
     *         Define if the {@link AnisekaiArray} must be defined.
     * @param allowEmpty
     *         Define if the {@link AnisekaiArray} can be empty.
     * @param allowedTypes
     *         Array of {@link Class} that will be used to validate the {@link AnisekaiArray} content.
     */
    public JsonArrayRule(String key, boolean required, boolean allowEmpty, Class<?>... allowedTypes) {

        super(key, required);
        this.allowEmpty   = allowEmpty;
        this.allowedTypes = allowedTypes;
    }

    @Override
    public void validate(AnisekaiJson json) {

        this.retrieve(json).ifPresent(obj -> {
            AnisekaiArray array = this.asArray(obj, this.allowEmpty);

            for (int i = 0; i < array.size(); i++) {
                Object item = array.get(i);
                if (Arrays.stream(this.allowedTypes).noneMatch(type -> type.isInstance(item))) {
                    throw new JSONValidationException(this, String.format(RULE_EX_MSG, this, i, item.getClass()));
                }
            }
        });
    }

    @Override
    public String getName() {

        String requireState = this.isRequired() ? "required" : "not required";
        String emptyState   = this.allowEmpty ? "allow empty" : "disallow empty";

        String names = Arrays.stream(this.allowedTypes)
                             .map(Class::getSimpleName)
                             .collect(Collectors.joining(", "));

        return String.format(RULE_FORMAT, this.getKey(), requireState, emptyState, names);
    }

}
