package fr.anisekai.core.proxy;

import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.core.persistence.annotations.NoProxy;
import fr.anisekai.core.persistence.annotations.WithProxy;
import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.core.persistence.events.EntityEvent;
import fr.anisekai.core.persistence.repository.AnisekaiRepository;
import fr.anisekai.proxy.ClassProxyFactory;
import fr.anisekai.proxy.interfaces.State;
import fr.anisekai.proxy.reflection.LinkedProperty;
import fr.anisekai.proxy.reflection.Properties;
import fr.anisekai.proxy.reflection.Property;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Dynamic proxy for {@link AnisekaiRepository} instances that intercepts persistence operations to provide automatic
 * event publishing and state tracking.
 * <p>
 * This handler wraps the underlying Spring Data repository. It ensures that all entities returned by {@code find*}
 * methods are wrapped in a state-tracking proxy. Conversely, when {@code save} or {@code delete} methods are called, it
 * unwraps the entities, performs the operation, calculates the differential state (dirty checking), and publishes the
 * appropriate {@link EntityEvent}.
 *
 * @param <T>
 *         The type of the entity.
 * @param <ID>
 *         The type of the entity's identifier.
 */
@SuppressWarnings("unchecked")
public final class RepositoryProxy<T extends Entity<ID>, ID extends Serializable> implements InvocationHandler, AutoCloseable {

    private static final Pattern FIND_PATTERN = Pattern.compile("find(?<collection>All)?(?:With(?<relations>.+))?By");
    private static final Logger  LOGGER       = LoggerFactory.getLogger(RepositoryProxy.class);

    private final AnisekaiRepository<T, ID> target;
    private final EntityEventProcessor      eventProcessor;
    private final ClassProxyFactory         factory;

    private RepositoryProxy(AnisekaiRepository<T, ID> target, EntityEventProcessor eventProcessor) {

        this.target         = target;
        this.eventProcessor = eventProcessor;
        this.factory        = new ClassProxyFactory();
    }

    /**
     * Creates a new proxied repository instance.
     *
     * @param target
     *         The actual repository bean to wrap.
     * @param eventProcessor
     *         The processor used to dispatch entity events.
     *
     * @return A dynamic proxy implementing the repository interface.
     */
    public static <R extends AnisekaiRepository<T, ID>, T extends Entity<ID>, ID extends Serializable> R createProxy(R target, EntityEventProcessor eventProcessor) {

        LOGGER.trace("Creating repository proxy for {}", target.getClass().getInterfaces()[0].getSimpleName());
        return (R) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new RepositoryProxy<>(target, eventProcessor)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] inputArgs) throws Throwable {

        Object[] args = inputArgs == null ? new Object[0] : inputArgs;

        // Handle AutoCloseable interface
        if (method.getName().equals("close") && args.length == 0) {
            this.close();
            return null;
        }

        // Check for explicit bypass or unsupported batch operations
        boolean hasNoProxyAnnotation = method.getAnnotation(NoProxy.class) != null;
        boolean isSpecialDelete = (method.getName().startsWith("deleteBy")) ||
                (method.getName().startsWith("deleteAllBy")) ||
                (method.getName().equals("deleteAll") && args.length == 0);

        if (hasNoProxyAnnotation || isSpecialDelete) {
            if (isSpecialDelete) {
                LOGGER.warn(
                        "({}) Cannot track 'deleteBy-(something)', 'deleteAll()' or 'deleteAllBy-(something)' calls.",
                        this.target.getClass().getInterfaces()[0].getName()
                );
                // The reason is too long to put it in a single log message.
                //
                // deleteAll(), deleteBy-(something) and deleteAllBy-(something) methods aren't supported because they are
                // directly going to the underlying entity manager. This cause the repository to not be aware of which
                // entities are being deleted.
                //
                // If you really need to get the deletion events for those entities, use a matching findBy-(something) or
                // findAllBy-(something) first, then send the result to delete(something) or deleteAll(something).
                //
                // It has been decided this should *not* be an error, but just a warning to the developer.
            }
            return method.invoke(this.target, args);
        }

        String methodName     = method.getName();
        String repositoryName = this.target.getClass().getInterfaces()[0].getSimpleName();
        LOGGER.trace("[{} Proxy]::{}(): Handling method...", repositoryName, methodName);

        // Intercept common write operations
        switch (methodName) {
            case "save" -> {
                return this.save((T) args[0], this.target::save);
            }
            case "saveAll" -> {
                return this.saveAll((Collection<T>) args[0], this.target::saveAll);
            }
            case "delete" -> {
                this.delete((T) args[0], this.target::delete);
                return null;
            }
            case "deleteAll" -> {
                this.deleteAll((Collection<T>) args[0], this.target::deleteAll);
                return null;
            }
        }

        // Call repository
        Matcher   matcher   = FIND_PATTERN.matcher(method.getName());
        WithProxy withProxy = method.getAnnotation(WithProxy.class);
        NoProxy   noProxy   = method.getAnnotation(NoProxy.class);
        boolean   hasMatch  = matcher.find();
        boolean   proxify   = (withProxy != null || hasMatch) && noProxy == null;

        if (withProxy != null && noProxy != null) {
            throw new IllegalStateException(String.format(
                    "Why annotating %s with both WithProxy and NoProxy at the same time ?",
                    methodName
            ));
        }

        for (int i = 0; i < args.length; i++) {
            Object value = args[i] instanceof State<?> state ? state.getInstance() : args[i];
            this.unwrap(value);
            args[i] = value;
        }

        Object result = method.invoke(this.target, args);
        return this.handleResult(result, proxify);
    }


    private Object handleItem(Object item, boolean proxify) {

        return proxify ? this.factory.create(item).getProxy() : item;
    }

    private Object handleResult(Object result, boolean proxify) {

        return switch (result) {
            case null -> null;
            case Optional<?> opt -> opt.map(entity -> this.handleItem(entity, proxify));
            case PageImpl<?> page -> page.map(entity -> this.handleItem(entity, proxify));
            case List<?> list -> list.stream()
                                     .map(entity -> this.handleItem(entity, proxify))
                                     .toList();
            case Set<?> set -> set.stream()
                                  .map(entity -> this.handleItem(entity, proxify))
                                  .collect(Collectors.toSet());
            case Collection<?> collection ->
                    throw new IllegalStateException("Unsupported collection type: " + collection.getClass().getName());
            default -> this.handleItem(result, proxify);
        };
    }

    private T save(T entity, Function<T, T> saver) throws ReflectiveOperationException {

        State<T> state = this.factory.getExistingState(entity);

        if (state == null) {
            this.unwrap(entity);
            T saved = saver.apply(entity);
            this.eventProcessor.sendCreatedEvent(this.target, saved);
            return this.factory.create(saved).getProxy();
        }

        T original = state.getInstance();
        T saved    = saver.apply(original);

        this.swapNotify(state, saved);
        return state.getProxy();
    }

    private List<T> saveAll(Collection<T> entities, Function<Collection<T>, List<T>> saver) throws ReflectiveOperationException {
        // Unwraps proxies to get raw instances for the repository
        CollectionResult<T, List<T>> applyResult = this.applyToList(entities, saver);

        List<State<T>> states   = applyResult.states();
        List<T>        allSaved = applyResult.result();

        List<T> registeredEntities = new ArrayList<>(entities.size());

        for (int i = 0; i < allSaved.size(); i++) {
            T        saved = allSaved.get(i);
            State<T> state = states.get(i);

            if (state == null) {
                state = this.factory.create(saved);
                this.eventProcessor.sendCreatedEvent(this.target, state.getProxy());
            } else {
                this.swapNotify(state, saved);
            }

            registeredEntities.add(state.getProxy());
        }

        return registeredEntities;
    }

    private void delete(T entity, Consumer<T> deleter) {

        State<T> state = this.factory.getExistingState(entity);

        if (state == null) {
            deleter.accept(entity);
            return;
        }

        T original = state.getInstance();
        deleter.accept(original);
        this.eventProcessor.sendDeletedEvent(this.target, original);
    }

    private void deleteAll(Collection<T> entities, Consumer<Collection<T>> deleter) throws ReflectiveOperationException {

        CollectionResult<T, Collection<T>> applyResult = this.applyToList(
                entities,
                items -> {
                    deleter.accept(items);
                    return items;
                }
        );

        for (T entity : applyResult.result()) {
            this.eventProcessor.sendDeletedEvent(this.target, entity);
        }
    }

    /**
     * Unwraps proxies into a list of raw entities to pass to the underlying repository. Maintains a parallel list of
     * State objects to correlate results later.
     */
    private <K> CollectionResult<T, K> applyToList(Collection<T> entities, Function<Collection<T>, K> function) throws ReflectiveOperationException {

        List<State<T>> states           = new ArrayList<>(entities.size());
        Collection<T>  originalEntities = new ArrayList<>(entities.size());

        for (T entity : entities) {
            State<T> state = this.factory.getExistingState(entity);
            states.add(state);

            if (state == null) {
                this.unwrap(entity);
                originalEntities.add(entity);
            } else {
                originalEntities.add(state.getInstance());
            }
        }

        K result = function.apply(originalEntities);
        return new CollectionResult<>(states, result);
    }

    @Override
    public void close() {

        this.factory.close();
    }

    /**
     * Helper record to hold the mapping between states and the operation result.
     */
    record CollectionResult<T, K>(List<State<T>> states, K result) {

    }

    /**
     * Allow to recursively unwrap proxy objects from the provided {@code object}.
     *
     * @param object
     *         The object to unwrap.
     *
     * @throws ReflectiveOperationException
     *         if an error occurs while setting / getting a property value.
     */
    private void unwrap(Object object) throws ReflectiveOperationException {

        this.unwrap(object, new HashSet<>());
    }

    private void unwrap(Object object, Set<Object> visited) throws ReflectiveOperationException {

        if (object == null) return;
        visited.add(object);
        Set<LinkedProperty> properties = Properties.getPropertiesOf(object);

        for (LinkedProperty property : properties) {
            Object value = property.getValue();

            if (value instanceof State<?> state) {
                property.setValue(state.getInstance());
            } else {
                if (!visited.contains(value)) {
                    this.unwrap(value, visited);
                }
            }
        }
    }

    private void swapNotify(State<T> state, T entity) {

        Map<Property, Object> copyOriginal     = new HashMap<>(state.getOriginalState());
        Map<Property, Object> copyDifferential = new HashMap<>(state.getDifferentialState());

        this.factory.refresh(state.getInstance(), entity);
        this.eventProcessor.sendModifiedEvent(this.target, state.getProxy(), copyOriginal, copyDifferential);
    }

}