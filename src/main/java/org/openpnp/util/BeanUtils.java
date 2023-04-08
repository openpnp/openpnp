package org.openpnp.util;

import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Converter;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;

public class BeanUtils {
    public static boolean addPropertyChangeListener(Object obj, String property, PropertyChangeListener listener) {
        try {
            Method method = obj.getClass().getMethod("addPropertyChangeListener", String.class, PropertyChangeListener.class);
            method.invoke(obj, property, listener);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
    
    public static boolean addPropertyChangeListener(Object obj, PropertyChangeListener listener) {
        try {
            Method method = obj.getClass().getMethod("addPropertyChangeListener", PropertyChangeListener.class);
            method.invoke(obj, listener);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
    
    public static AutoBinding bind(UpdateStrategy updateStrategy, Object source, String sourceProperty,
            Object target, String targetProperty) {
        AutoBinding binding = Bindings.createAutoBinding(updateStrategy, source,
                BeanProperty.create(sourceProperty), target, BeanProperty.create(targetProperty));
        binding.bind();
        return binding;
    }

    public static AutoBinding bind(UpdateStrategy updateStrategy, Object source, String sourceProperty,
            Object target, String targetProperty, Converter converter) {
        AutoBinding binding = Bindings.createAutoBinding(updateStrategy, source,
                BeanProperty.create(sourceProperty), target, BeanProperty.create(targetProperty));
        if (converter != null) {
            binding.setConverter(converter);
        }
        binding.bind();
        return binding;
    }
}
