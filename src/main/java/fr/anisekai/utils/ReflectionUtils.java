package fr.anisekai.utils;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.util.Comparator;

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

}
