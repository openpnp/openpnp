package org.openpnp.gui.support;

import org.jdesktop.beansbinding.Converter;

import java.util.HashMap;
import java.util.Map;

public class Converters {

    private static Map<Class<?>, Converter<?, String>> CONVERTERS = new HashMap<>();

    static {
        CONVERTERS.put(Boolean.class, new BooleanConverter());
        CONVERTERS.put(Integer.class, new IntegerConverter());
        CONVERTERS.put(Long.class, new LongConverter());
        CONVERTERS.put(Double.class, new DoubleConverter());
        CONVERTERS.put(String.class, new Converter<String, String>() {
            @Override
            public String convertForward(String s) {
                return s;
            }

            @Override
            public String convertReverse(String s) {
                return s;
            }
        });
    }

    public static <T> Converter<T, String> getConverter(Class<?> clazz) {
        Converter<?, String> converter = CONVERTERS.get(clazz);
        if (converter == null) {
            throw new IllegalArgumentException("Converter for class \"" + clazz + "\" not found");
        }
        return (Converter<T, String>) converter;
    }
}
