package org.openpnp.spi.base;

import org.openpnp.spi.Signaler;
import org.simpleframework.xml.Attribute;

public abstract class AbstractSignaler implements Signaler {

    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    @Override
    public void signalMachineState(AbstractMachine.State state) {}

    @Override
    public void signalJobProcessorState(AbstractJobProcessor.State state) {}

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
