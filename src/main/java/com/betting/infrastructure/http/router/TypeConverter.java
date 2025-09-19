package com.betting.infrastructure.http.router;

import java.util.*;
import java.util.function.Function;

/**
 * `TypeConverter` is a utility class for converting String values to various target types.
 * It supports primitive types, their wrapper classes, and String. It also allows for custom type
 */
public class TypeConverter {
    private static final Map<Class<?>, Function<String, ?>> converters = new HashMap<>();
    private static final Map<Class<?>, Object> defaults = new HashMap<>();

    private TypeConverter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    static {
        converters.put(int.class, Integer::parseInt);
        converters.put(Integer.class, Integer::valueOf);
        defaults.put(int.class, 0);

        converters.put(long.class, Long::parseLong);
        converters.put(Long.class, Long::valueOf);
        defaults.put(long.class, 0L);

        converters.put(double.class, Double::parseDouble);
        converters.put(Double.class, Double::valueOf);
        defaults.put(double.class, 0.0);

        converters.put(boolean.class, Boolean::parseBoolean);
        converters.put(Boolean.class, Boolean::valueOf);
        defaults.put(boolean.class, false);

        converters.put(String.class, s -> s);
        defaults.put(String.class, "");
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(String value, Class<T> targetType) {
        if (value == null || value.isEmpty()) {
            return (T) defaults.getOrDefault(targetType, null);
        }
        Function<String, ?> converter = converters.get(targetType);
        if (converter == null) {
            throw new IllegalArgumentException("No converter for type: " + targetType.getName());
        }
        try {
            return (T) converter.apply(value.trim());
        } catch (Exception e) {
            // return default value if conversion fails
            return (T) defaults.getOrDefault(targetType, null);
        }
    }

    // 自定义注册
    public static <T> void register(Class<T> type, Function<String, T> converter, T defaultValue) {
        converters.put(type, converter);
        defaults.put(type, defaultValue);
    }
}

