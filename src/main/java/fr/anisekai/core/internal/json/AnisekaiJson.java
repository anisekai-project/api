package fr.anisekai.core.internal.json;

import fr.anisekai.core.internal.json.validation.JsonRule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static fr.anisekai.utils.StringUtils.parseIntOrThrow;

/**
 * Utility class adding few features on top of {@link JSONObject}.
 */

public class AnisekaiJson extends JSONObject {

    private static final BiFunction<JSONObject, String, AnisekaiJson> ANISEKAI_JSON_JSON =
            (json, key) -> {
                JSONObject item = json.getJSONObject(key);
                if (item instanceof AnisekaiJson anisekaiJson) return anisekaiJson;
                AnisekaiJson anisekaiJson = new AnisekaiJson(item);
                json.put(key, anisekaiJson);
                return anisekaiJson;
            };

    private static final BiFunction<JSONArray, Integer, AnisekaiArray> ANISEKAI_ARRAY_ARRAY =
            (array, index) -> {
                JSONArray item = array.getJSONArray(index);
                if (item instanceof AnisekaiArray anisekaiArray) return anisekaiArray;
                AnisekaiArray anisekaiArray = new AnisekaiArray(item);
                array.put(index, anisekaiArray);
                return anisekaiArray;
            };

    private static final BiFunction<JSONObject, String, AnisekaiArray> ANISEKAI_JSON_ARRAY =
            (json, key) -> {
                JSONArray item = json.getJSONArray(key);
                if (item instanceof AnisekaiArray anisekaiArray) return anisekaiArray;
                AnisekaiArray anisekaiArray = new AnisekaiArray(item);
                json.put(key, anisekaiArray);
                return anisekaiArray;
            };
    private static final BiFunction<JSONArray, Integer, AnisekaiJson>  ANISEKAI_ARRAY_JSON =
            (array, index) -> {
                JSONObject item = array.getJSONObject(index);
                if (item instanceof AnisekaiJson anisekaiJson) return anisekaiJson;
                AnisekaiJson anisekaiJson = new AnisekaiJson(item);
                array.put(index, anisekaiJson);
                return anisekaiJson;
            };

    /**
     * Create an empty {@link AnisekaiJson} instance.
     */
    public AnisekaiJson() {

        super();
    }

    /**
     * Create an {@link AnisekaiJson} instance using the provided {@link String}.
     *
     * @param source
     *         A string representation of a {@link JSONObject}.
     *
     * @throws JSONException
     *         If there is a syntax error in the source string or a duplicated key.
     */
    public AnisekaiJson(String source) throws JSONException {

        super(source);
    }

    /**
     * Create an {@link AnisekaiJson} instance from the provided {@link Map}.
     *
     * @param map
     *         A map object that can be used to initialize the contents of the {@link JSONObject}.
     *
     * @throws JSONException
     *         If a value in the map is non-finite number.
     * @throws NullPointerException
     *         If a key in the map is <code>null</code>
     */
    public AnisekaiJson(Map<?, ?> map) {

        super(map);
    }

    /**
     * Create an {@link AnisekaiJson} instance from an existing {@link JSONObject}.
     *
     * @param source
     *         A {@link JSONObject} from which this {@link AnisekaiJson} will be initialized.
     */
    public AnisekaiJson(JSONObject source) {

        this(source.toString());
    }

    /**
     * Traverses a JSON tree structure following a dot-separated path and applies a terminal function on the final node,
     * which can be a {@link JSONObject} or {@link JSONArray}.
     *
     * <p>Each segment in the path represents either a key in a {@link JSONObject} or an index in a {@link JSONArray}.
     * The method descends into the structure step by step and invokes the appropriate handler function when the final
     * node is reached.</p>
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     * @param ifJsonObj
     *         Function to handle the final node if it is a {@link JSONObject}.
     * @param ifJsonArray
     *         Function to handle the final node if it is a {@link JSONArray}.
     * @param <T>
     *         The type of result returned by the handler functions.
     *
     * @return The result of the applied handler function.
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a missing key, type mismatch, or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    public <T> T getInTree(@NotNull String path, @NotNull BiFunction<JSONObject, String, T> ifJsonObj, @NotNull BiFunction<JSONArray, Integer, T> ifJsonArray) {

        List<String> parts       = List.of(path.split("\\."));
        Object       currentNode = this;

        for (int i = 0; i < parts.size(); i++) {
            boolean isLast      = i == parts.size() - 1;
            String  key         = parts.get(i);
            String  currentPath = String.join(".", parts.subList(0, i + 1));

            //noinspection ChainOfInstanceofChecks
            if (currentNode instanceof JSONObject object) {
                if (isLast) return ifJsonObj.apply(object, key);
                if (!object.has(key)) throw new JSONException(String.format("[%s] not found", currentPath));

                currentNode = object.get(key);
                continue;

            } else if (currentNode instanceof JSONArray array) {
                int idx = parseIntOrThrow(
                        key, nfe -> new JSONException(
                                String.format("[%s] Key was encountered when index was expected.", currentPath),
                                nfe
                        )
                );

                if (isLast) return ifJsonArray.apply(array, idx);
                if (idx >= array.length()) {
                    throw new JSONException(String.format("[%s] Index was out of bounds", currentPath));
                }

                currentNode = array.get(idx);
                continue;
            }

            throw new UnsupportedOperationException(String.format(
                    "Encountered unsupported type: %s",
                    currentNode.getClass().getSimpleName()
            ));
        }

        // We should never get here, but this is to suppress error/warning messages.
        throw new IllegalStateException(String.format("[%s] Unresolved path.", path));
    }

    /**
     * Inserts a value into a nested {@link AnisekaiJson} structure following a dot-separated path.
     *
     * <p>If intermediate nodes along the path do not exist, they are created as empty {@link AnisekaiJson}s.
     * The final segment in the path is used as the key where the value is inserted.</p>
     *
     * <p>Note: This method does not currently support {@link JSONArray} traversal or insertion.</p>
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     * @param value
     *         The value to insert at the specified location.
     *
     * @return This {@link AnisekaiJson}, for chaining.
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a type mismatch or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    public AnisekaiJson putInTree(@NotNull String path, @Nullable Object value) throws JSONException {

        Object effectiveValue = Optional.ofNullable(value).orElse(NULL);

        if (!path.contains(".")) {
            super.put(path, effectiveValue);
            return this;
        }

        List<String> parts = List.of(path.split("\\."));

        AnisekaiJson currentNode = this;
        for (int i = 0; i < parts.size() - 1; i++) {
            String key = parts.get(i);

            if (!currentNode.has(key)) currentNode.put(key, new AnisekaiJson());
            currentNode = currentNode.readJson(key);
        }

        currentNode.put(parts.getLast(), effectiveValue);
        return this;
    }

    /**
     * Check if the path provided lead to an existing value within that {@link AnisekaiJson} and its subitems. This does
     * not check for nullability of the value; as long as the key exists, whether the underlying value is null or not,
     * the key will still be considered as "existing".
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return True if the key exists, false otherwise.
     *
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    public boolean hasInTree(@NotNull String path) {

        try {
            return this.getInTree(path, JSONObject::has, (array, index) -> index < array.length());
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Remove the key/value pair under the provided path.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a missing key, type mismatch, or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    public void removeInTree(@NotNull String path) {

        this.getInTree(path, JSONObject::remove, JSONArray::remove);
    }

    // <editor-fold desc=":: getOptional{X}()">

    /**
     * Retrieve an {@link Optional} {@link Integer} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link Optional} {@link Integer}.
     */
    public Optional<Integer> getOptionalInteger(@NotNull String path) {

        return this.hasInTree(path) ? Optional.of(this.getInTree(
                path,
                JSONObject::getInt,
                JSONArray::getInt
        )) : Optional.empty();
    }

    /**
     * Retrieve a {@link Optional} {@link Long} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link Optional} {@link Long}.
     */
    public Optional<Long> getOptionalLong(@NotNull String path) {

        return this.hasInTree(path) ? Optional.of(this.getInTree(
                path,
                JSONObject::getLong,
                JSONArray::getLong
        )) : Optional.empty();
    }

    /**
     * Retrieve an {@link Optional} {@link Double} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link Optional} {@link Double}.
     */
    public Optional<Double> getOptionalDouble(@NotNull String path) {

        return this.hasInTree(path) ? Optional.of(this.getInTree(
                path,
                JSONObject::getDouble,
                JSONArray::getDouble
        )) : Optional.empty();
    }

    /**
     * Retrieve an {@link Optional} {@link Boolean} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link Optional} {@link Boolean}.
     */
    public Optional<Boolean> getOptionalBoolean(@NotNull String path) {

        return this.hasInTree(path) ? Optional.of(this.getInTree(
                path,
                JSONObject::getBoolean,
                JSONArray::getBoolean
        )) : Optional.empty();
    }

    /**
     * Retrieve an {@link Optional} {@link String} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link Optional} {@link String}.
     */
    public Optional<String> getOptionalString(@NotNull String path) {

        return this.hasInTree(path) ? Optional.of(this.getInTree(
                path,
                JSONObject::getString,
                JSONArray::getString
        )) : Optional.empty();
    }

    /**
     * Retrieve an {@link Optional} {@link ZonedDateTime} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link Optional} {@link ZonedDateTime}.
     */
    public Optional<ZonedDateTime> getOptionalZonedDateTime(@NotNull String path) {

        return this.getOptionalString(path)
                   .map(str -> ZonedDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    /**
     * Retrieve an {@link Optional} {@link AnisekaiArray} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link Optional} {@link AnisekaiArray}.
     */
    public Optional<AnisekaiArray> getOptionalArray(@NotNull String path) {

        return this.hasInTree(path) ? Optional.of(this.getInTree(
                path,
                ANISEKAI_JSON_ARRAY,
                ANISEKAI_ARRAY_ARRAY
        )) : Optional.empty();
    }

    /**
     * Retrieve an {@link Optional} {@link AnisekaiJson} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link Optional} {@link AnisekaiJson}.
     */
    public Optional<AnisekaiJson> getOptionalJson(@NotNull String path) {

        return this.hasInTree(path) ? Optional.ofNullable(this.getInTree(
                path,
                ANISEKAI_JSON_JSON,
                ANISEKAI_ARRAY_JSON
        )) : Optional.empty();
    }

    /**
     * Retrieve an {@link Optional} {@link List} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     * @param mapper
     *         A function that can be applied to convert an {@link AnisekaiJson} to any object of type {@link T}.
     * @param <T>
     *         Type of the items, as defined by the provided mapper.
     *
     * @return An {@link Optional} {@link List}. If the {@link Optional} is empty, it means that the {@link List} was
     *         not found. If the {@link List} is empty, however, it means that the underlying {@link JSONArray} exists
     *         but is really empty.
     */
    public <T> Optional<List<T>> getOptionalList(@NotNull String path, @NotNull Function<AnisekaiJson, T> mapper) {

        return this.getOptionalArray(path).map(array -> array.map(mapper));
    }

    /**
     * Retrieve an {@link Optional} {@link AnisekaiJson} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     * @param mapper
     *         A function that can be applied to convert an {@link AnisekaiJson} to any object of type {@link T}.
     * @param <T>
     *         Type of the items, as defined by the provided mapper.
     *
     * @return An {@link Optional} {@link AnisekaiJson}.
     */
    public <T> Optional<T> getOptional(@NotNull String path, @NotNull Function<AnisekaiJson, T> mapper) {

        return this.getOptionalJson(path).map(mapper);
    }

    /**
     * Retrieve an {@link Optional} {@link Object} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link Optional} {@link Object}.
     */
    public Optional<Object> getOptional(@NotNull String path) {

        return this.hasInTree(path) ? Optional.ofNullable(this.getInTree(
                path,
                JSONObject::get,
                JSONArray::get
        )) : Optional.empty();
    }

    // </editor-fold>

    // <editor-fold desc=":: read{X}()">

    /**
     * Retrieve an {@link Integer} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link Integer}.
     */
    public Integer readInteger(@NotNull String path) {

        return this.getOptionalInteger(path).orElseThrow(() -> new JSONException("[%s] not found."));
    }

    /**
     * Retrieve a {@link Long} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return A {@link Long}.
     */
    public Long readLong(@NotNull String path) {

        return this.getOptionalLong(path).orElseThrow(() -> new JSONException("[%s] not found."));
    }

    /**
     * Retrieve a {@link Double} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return A {@link Double}.
     */
    public Double readDouble(@NotNull String path) {

        return this.getOptionalDouble(path).orElseThrow(() -> new JSONException("[%s] not found."));
    }

    /**
     * Retrieve a {@link Boolean} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return A {@link Boolean}.
     */
    public Boolean readBoolean(@NotNull String path) {

        return this.getOptionalBoolean(path).orElseThrow(() -> new JSONException("[%s] not found."));
    }

    /**
     * Retrieve a {@link String} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return A {@link String}.
     */
    public String readString(@NotNull String path) {

        return this.getOptionalString(path).orElseThrow(() -> new JSONException("[%s] not found.".formatted(path)));
    }

    /**
     * Retrieve a {@link ZonedDateTime} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return A {@link ZonedDateTime}.
     */
    public ZonedDateTime readZonedDateTime(@NotNull String path) {

        return this.getOptionalZonedDateTime(path)
                   .orElseThrow(() -> new JSONException("[%s] not found.".formatted(path)));
    }

    /**
     * Retrieve an {@link AnisekaiArray} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link AnisekaiArray}.
     */
    public AnisekaiArray readArray(@NotNull String path) {

        return this.getOptionalArray(path).orElseThrow(() -> new JSONException("[%s] not found.".formatted(path)));
    }

    /**
     * Retrieve an {@link AnisekaiJson} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     *
     * @return An {@link AnisekaiJson}.
     */
    public AnisekaiJson readJson(@NotNull String path) {

        return this.getOptionalJson(path).orElseThrow(() -> new JSONException("[%s] not found.".formatted(path)));
    }

    /**
     * Retrieve an {@link List} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     * @param mapper
     *         A function that can be applied to convert an {@link AnisekaiJson} to any object of type {@link T}.
     * @param <T>
     *         Type of the items, as defined by the provided mapper.
     *
     * @return A {@link List}. If the {@link List} is empty, it means that the underlying {@link JSONArray} exists but
     *         is empty.
     */
    public <T> List<T> readList(@NotNull String path, @NotNull Function<AnisekaiJson, T> mapper) {

        return this.getOptionalList(path, mapper)
                   .orElseThrow(() -> new JSONException("[%s] not found.".formatted(path)));
    }

    /**
     * Retrieve an {@link AnisekaiJson} defined by the path provided.
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     * @param mapper
     *         A function that can be applied to convert an {@link AnisekaiJson} to any object of type {@link T}.
     * @param <T>
     *         Type of the items, as defined by the provided mapper.
     *
     * @return An {@link AnisekaiJson}.
     */
    public <T> T read(@NotNull String path, @NotNull Function<AnisekaiJson, T> mapper) {

        return this.getOptional(path, mapper).orElseThrow(() -> new JSONException("[%s] not found.".formatted(path)));
    }

    // </editor-fold>

    // <editor-fold desc=":: put{X}()">

    /**
     * Add a boolean value under the provided {@link String} path.
     *
     * @param key
     *         The dot-separated path (e.g., "config.display.theme").
     * @param value
     *         The value to store.
     *
     * @return The current instance, for chaining.
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a type mismatch or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    @Override
    public JSONObject put(String key, boolean value) throws JSONException {

        return this.putInTree(key, value);
    }

    /**
     * Add a {@link Collection} under the provided {@link String} path.
     *
     * @param key
     *         The dot-separated path (e.g., "config.display.theme").
     * @param value
     *         The value to store.
     *
     * @return The current instance, for chaining.
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a type mismatch or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    @Override
    public JSONObject put(String key, Collection<?> value) throws JSONException {

        return this.putInTree(key, value);
    }

    /**
     * Add a double value under the provided {@link String} path.
     *
     * @param key
     *         The dot-separated path (e.g., "config.display.theme").
     * @param value
     *         The value to store.
     *
     * @return The current instance, for chaining.
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a type mismatch or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    @Override
    public JSONObject put(String key, double value) throws JSONException {

        return this.putInTree(key, value);
    }

    /**
     * Add a float value under the provided {@link String} path.
     *
     * @param key
     *         The dot-separated path (e.g., "config.display.theme").
     * @param value
     *         The value to store.
     *
     * @return The current instance, for chaining.
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a type mismatch or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    @Override
    public JSONObject put(String key, float value) throws JSONException {

        return this.putInTree(key, value);
    }

    /**
     * Add an int value under the provided {@link String} path.
     *
     * @param key
     *         The dot-separated path (e.g., "config.display.theme").
     * @param value
     *         The value to store.
     *
     * @return The current instance, for chaining.
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a type mismatch or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    @Override
    public JSONObject put(String key, int value) throws JSONException {

        return this.putInTree(key, value);
    }

    /**
     * Add a long value under the provided {@link String} path.
     *
     * @param key
     *         The dot-separated path (e.g., "config.display.theme").
     * @param value
     *         The value to store.
     *
     * @return The current instance, for chaining.
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a type mismatch or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    @Override
    public JSONObject put(String key, long value) throws JSONException {

        return this.putInTree(key, value);
    }

    /**
     * Add a {@link Map} value under the provided {@link String} path.
     *
     * @param key
     *         The dot-separated path (e.g., "config.display.theme").
     * @param value
     *         The value to store.
     *
     * @return The current instance, for chaining.
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a type mismatch or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    @Override
    public JSONObject put(String key, Map<?, ?> value) throws JSONException {

        return this.putInTree(key, value);
    }

    /**
     * Add a {@link TemporalAccessor} value under the provided {@link String} path.
     *
     * @param key
     *         The dot-separated path (e.g., "config.display.theme").
     * @param value
     *         The value to store.
     *
     * @return The current instance, for chaining.
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a type mismatch or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    public JSONObject put(String key, TemporalAccessor value) {

        if (value == null) return this.putInTree(key, null);
        String format = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value);
        return this.putInTree(key, format);
    }

    /**
     * Add an {@link Object} value under the provided {@link String} path.
     *
     * @param key
     *         The dot-separated path (e.g., "config.display.theme").
     * @param value
     *         The value to store.
     *
     * @return The current instance, for chaining.
     *
     * @throws JSONException
     *         If the path cannot be resolved due to a type mismatch or invalid array index.
     * @throws UnsupportedOperationException
     *         If the traversal encounters an unsupported type.
     */
    @Override
    public JSONObject put(String key, Object value) throws JSONException {

        return this.putInTree(key, value);
    }

    // </editor-fold>

    // <editor-fold desc=":: Type-Conversion overrides">

    /**
     * Inserts a value into a nested {@link AnisekaiJson} structure following a dot-separated path.
     *
     * <p>If intermediate nodes along the path do not exist, they are created as empty {@link AnisekaiJson}s.
     * The final segment in the path is used as the key where the value is inserted.</p>
     *
     * <p>Note: This method does not currently support {@link JSONArray} traversal or insertion.</p>
     *
     * @param path
     *         The dot-separated path (e.g., "config.display.theme").
     * @param temporal
     *         Any {@link TemporalAccessor} that will be converted to {@link String} using
     *         {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}.
     *
     * @return This {@link AnisekaiJson}, for chaining.
     *
     * @throws JSONException
     *         If a non-JSONObject is encountered during traversal.
     */
    public AnisekaiJson putInTree(@NotNull String path, @Nullable TemporalAccessor temporal) {

        if (temporal == null) return this.putInTree(path, null);
        String format = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(temporal);
        return this.putInTree(path, format);
    }

    // </editor-fold>

    /**
     * Check if this {@link AnisekaiJson} follows the provided set of {@link JsonRule}.
     *
     * @param rules
     *         Rules to check.
     *
     * @throws JSONException
     *         Thrown if one of the rules didn't match.
     */
    public void validate(JsonRule... rules) {

        for (JsonRule rule : rules) {
            rule.validate(this);
        }
    }

    /**
     * Check if this {@link AnisekaiJson} follows the provided set of {@link JsonRule}.
     *
     * @param rules
     *         Rules to check.
     *
     * @throws JSONException
     *         Thrown if one of the rules didn't match.
     */
    public void validate(Iterable<JsonRule> rules) {

        for (JsonRule rule : rules) {
            rule.validate(this);
        }
    }

}
