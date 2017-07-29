package org.openpnp.vision.pipeline.ui.converters;

import com.l2fprod.common.util.converter.Converter;
import com.l2fprod.common.util.converter.Registry;

import org.openpnp.gui.support.LengthConverter;
import org.openpnp.model.Length;

public class LengthPropertySheetConverter implements Converter {
    private LengthConverter lengthConverter = new LengthConverter();

    @Override
    public Object convert(Class aClass, Object o) {
        if (aClass == Length.class) {
            String string = (String)o;
            return toObject(string);
        } else if (aClass == String.class) {
            Length length = (Length)o;
            return toString(length);
        }
        return null;
    }

    @Override
    public void register(Registry registry) {
        registry.addConverter(Length.class, String.class, this);
        registry.addConverter(String.class, Length.class, this);
    }

    public Length toObject(String text) {
        return lengthConverter.convertReverse(text);
    }

    public String toString(Length length) {
        // lengthConverter.convertForward(length) will convert the units to the systems units, and not display the
        // units used in the final string. We don't want this, as the user is entering the units they want to be using.
        return length.toString();
    }

}