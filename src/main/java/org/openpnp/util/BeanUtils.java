package org.openpnp.util;

import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;

public class BeanUtils {
    public static boolean addPropertyChangeListener(Object obj, String property, PropertyChangeListener listener) {
        try {
            /**
             * If the object we're creating a node for supports property change then we add
             * a listener. When we get an indexed changed we refresh our children and
             * when we get a non-indexed change we refresh ourself.
             * 
             * TODO: Note: Since we don't know which child got refreshed, we refresh them
             * all and this causes the JTree to collapse all the other children. This sucks
             * but there isn't a clean way to know which child changed without including the
             * property name somewhere.
             */
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
            /**
             * If the object we're creating a node for supports property change then we add
             * a listener. When we get an indexed changed we refresh our children and
             * when we get a non-indexed change we refresh ourself.
             * 
             * TODO: Note: Since we don't know which child got refreshed, we refresh them
             * all and this causes the JTree to collapse all the other children. This sucks
             * but there isn't a clean way to know which child changed without including the
             * property name somewhere.
             */
            Method method = obj.getClass().getMethod("addPropertyChangeListener", PropertyChangeListener.class);
            method.invoke(obj, listener);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}
