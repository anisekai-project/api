package fr.anisekai.utils;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Comparator;
import java.util.Optional;

public final class ReflectionUtils {

    private ReflectionUtils() {}

    public static int extractOrder(Object object) {

        return extractOrder(object.getClass());
    }

    public static int extractOrder(Class<?> clazz) {

        Order order = clazz.getAnnotation(Order.class);
        return order == null ? Ordered.LOWEST_PRECEDENCE : order.value();
    }

    public static Comparator<Object> compareOrder() {

        return Comparator.comparingInt(ReflectionUtils::extractOrder);
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<Constructor<? extends T>> findAdequateConstructor(Class<? extends T> clazz, Class<?>... args) {

        lookup:
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.getParameterCount() != args.length) continue;

            Parameter[] parameters = constructor.getParameters();

            for (int i = 0; i < parameters.length; i++) {
                if (!parameters[i].getType().isAssignableFrom(args[i])) {
                    continue lookup;
                }
            }

            return Optional.of((Constructor<? extends T>) constructor);
        }

        return Optional.empty();
    }

}
