package org.openpnp.gui.support;

import org.jdesktop.beansbinding.Converter;
import org.openpnp.spi.Actuator;

import java.util.function.Supplier;

public class ActuatorConverter extends Converter <Object, String> {
    private final Supplier<Actuator> actuatorFct;

    public ActuatorConverter(Supplier<Actuator> actuatorFct) {
        this.actuatorFct = actuatorFct;
    }

    @Override
    public String convertForward(Object o) {
        return Converters.getConverter(actuatorFct.get().getValueClass()).convertForward(o);
    }

    @Override
    public Object convertReverse(String s) {
        return Converters.getConverter(actuatorFct.get().getValueClass()).convertReverse(s);
    }
}
