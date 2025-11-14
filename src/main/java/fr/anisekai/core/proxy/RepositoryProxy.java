package fr.anisekai.core.proxy;

import fr.anisekai.core.annotations.NoProxy;
import fr.anisekai.core.persistence.EntityEventProcessor;
import fr.anisekai.core.persistence.domain.Entity;
import fr.anisekai.core.persistence.interfaces.AnisekaiRepository;
import fr.anisekai.proxy.ClassProxyFactory;
import fr.anisekai.proxy.interfaces.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

// TODO: Make sure that upon save, the same proxy points toward the saved entity.
//  This would allow saving multiple time in a row while keeping state tracking and ignoring the source of the proxy
//  completely.
@SuppressWarnings("unchecked")
public final class RepositoryProxy<T extends Entity<ID>, ID extends Serializable> implements InvocationHandler, AutoCloseable {

    record CollectionResult<T, K>(List<State<T>> states, K result) {

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryProxy.class);

    private final AnisekaiRepository<T, ?> target;
    private final EntityEventProcessor     eventProcessor;
    private final ClassProxyFactory        proxyFactory;

    private RepositoryProxy(AnisekaiRepository<T, ID> target, EntityEventProcessor eventProcessor) {

        this.target         = target;
        this.eventProcessor = eventProcessor;
        this.proxyFactory   = new ClassProxyFactory();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] inputArgs) throws Throwable {

        Object[] args = inputArgs == null ? new Object[0] : inputArgs;

        boolean hasNoProxyAnnotation = method.getAnnotation(NoProxy.class) != null;

        if (method.getName().equals("close") && args.length == 0) {
            this.close();
            return null;
        }

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

        String methodName = method.getName();
        LOGGER.trace("Handling '{}()' on {}...", methodName, this.target.getClass().getInterfaces()[0].getSimpleName());

        if (methodName.startsWith("findBy") || methodName.startsWith("getBy")) {
            return this.handleFind(method.invoke(this.target, args));
        }

        if (methodName.startsWith("findAllBy") || methodName.startsWith("getAllBy")) {
            return this.handleFindAll((Collection<T>) method.invoke(this.target, args));
        }

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


        LOGGER.debug("... or not.");
        return method.invoke(this.target, args);
    }

    private <K> CollectionResult<T, K> applyToList(Collection<T> entities, Function<Collection<T>, K> function) {


        List<State<T>> states           = new ArrayList<>(entities.size());
        Collection<T>  originalEntities = new ArrayList<>(entities.size());

        for (T entity : entities) {
            State<T> state = this.proxyFactory.getState(entity);
            states.add(state);

            if (state == null) {
                originalEntities.add(entity);
            } else {
                originalEntities.add(state.getInstance());
            }
        }

        K result = function.apply(originalEntities);
        return new CollectionResult<>(states, result);
    }

    private Object handleFind(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Optional<?> opt) {
            return ((Optional<T>) opt).map(entity -> this.proxyFactory.create(entity).getProxy());
        } else {
            return this.proxyFactory.create((T) value).getProxy();
        }
    }

    private Collection<T> handleFindAll(Collection<T> value) {

        return value.stream().map(entity -> this.proxyFactory.create(entity).getProxy()).toList();
    }

    private T save(T entity, Function<T, T> saver) throws ReflectiveOperationException {

        State<T> state = this.proxyFactory.getState(entity);

        if (state == null) {
            T saved = saver.apply(entity);
            this.eventProcessor.sendCreatedEvent(this.target, saved);
            return saved;
        }

        T original = state.getInstance();
        T saved    = saver.apply(original);
        this.eventProcessor.sendModifiedEvent(this.target, state, saved);
        state.close();
        return this.proxyFactory.create(saved).getProxy();
    }

    private List<T> saveAll(Collection<T> entities, Function<Collection<T>, List<T>> saver) {

        CollectionResult<T, List<T>> applyResult = this.applyToList(entities, saver);

        List<State<T>> states             = applyResult.states();
        List<T>        allSaved           = applyResult.result();
        List<T>        registeredEntities = new ArrayList<>(entities.size());

        for (int i = 0; i < allSaved.size(); i++) {
            T        saved = allSaved.get(i);
            State<T> state = states.get(i);

            if (state == null) {
                this.eventProcessor.sendCreatedEvent(this.target, saved);
            } else {
                this.eventProcessor.sendModifiedEvent(this.target, state, saved);
                state.close();
            }

            registeredEntities.add(this.proxyFactory.create(saved).getProxy());
        }

        return registeredEntities;
    }

    private void delete(T entity, Consumer<T> deleter) {

        State<T> state = this.proxyFactory.getState(entity);

        if (state == null) {
            deleter.accept(entity);
            return;
        }

        T original = state.getInstance();
        deleter.accept(original);
        this.eventProcessor.sendDeletedEvent(this.target, original);
    }

    private void deleteAll(Collection<T> entities, Consumer<Collection<T>> deleter) {

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

    @Override
    public void close() {

        this.proxyFactory.close();
    }

    @SuppressWarnings("unchecked")
    public static <R extends AnisekaiRepository<T, ID>, T extends Entity<ID>, ID extends Serializable> R createProxy(R target, EntityEventProcessor eventProcessor) {

        LOGGER.trace("Creating repository proxy for {}", target.getClass().getInterfaces()[0].getSimpleName());
        return (R) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new RepositoryProxy<>(target, eventProcessor)
        );
    }

}
