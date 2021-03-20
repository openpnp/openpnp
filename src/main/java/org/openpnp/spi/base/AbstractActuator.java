package org.openpnp.spi.base;

import javax.swing.Icon;

import org.openpnp.ConfigurationListener;
import org.openpnp.machine.reference.ActuatorInterlockMonitor;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractActuator extends AbstractHeadMountable implements Actuator {
    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    @Deprecated
    @Attribute(required = false)
    protected ActuatorValueType valueType = ActuatorValueType.Boolean;

    @Deprecated
    @Attribute(required = false)
    protected boolean valueTypeConfirmed = false;

    @Attribute(required = false)
    protected Class<?> valueClass = Boolean.class;

    @Attribute(required = false)
    protected boolean valueClassConfirmed = false;

    @Deprecated
    @Attribute(required = false)
    protected Double defaultOnDouble = 0.0;

    @Deprecated
    @Attribute(required = false)
    protected String defaultOnString = "";

    @Deprecated
    @Attribute(required = false)
    protected Double defaultOffDouble = 0.0;

    @Deprecated
    @Attribute(required = false)
    protected String defaultOffString = "";

    @Attribute(required = false)
    protected boolean interlockActuator;

    @Element(required = false)
    private InterlockMonitor interlockMonitor;

    protected Head head;

    private Driver driver;

    @Attribute(required = false)
    private String driverId;

    @Attribute(required = false)
    private boolean coordinatedBeforeActuate = true;

    @Attribute(required = false)
    private boolean coordinatedAfterActuate = false;

    @Attribute(required = false)
    private boolean coordinatedBeforeRead = true;

    public AbstractActuator() {
        this.id = Configuration.createId("ACT");
        this.name = getClass().getSimpleName();
        Configuration.get().addListener(new ConfigurationListener.Adapter() {

            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                driver = configuration.getMachine().getDriver(driverId);
            }
        });
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Head getHead() {
        return head;
    }

    @Override
    public void setHead(Head head) {
        this.head = head;
    }

    @Override
    public Driver getDriver() {
        // TODO: rework this fallback
        if (driver == null) {
            return Configuration.get().getMachine().getDrivers().get(0);
        }
        return driver;
    }

    @Override
    public void setDriver(Driver driver) {
        Object oldValue = this.driver;
        this.driver = driver;
        this.driverId = (driver == null) ? null : driver.getId();
        firePropertyChange("driver", oldValue, driver);
    }

    @Override
    public Location getCameraToolCalibratedOffset(Camera camera) {
        return new Location(camera.getUnitsPerPixel().getUnits());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        firePropertyChange("name", null, name);
    }

    @Override
    public Class<?> getValueClass() {
        return valueClass;
    }

    public void setValueClass(Class<?> valueClass) {
        Object oldValue = this.valueClass;
        this.valueClass = valueClass;
        if (oldValue != valueClass) {
            valueClassConfirmed = true;
            firePropertyChange("valueClass", oldValue, valueClass);
        }
    }

    /**
     * @return true if the valueClass was confirmed by functional usage or user interaction.
     */
    public boolean isValueClassConfirmed() {
        return valueClassConfirmed;
    }

    public void setValueClassConfirmed(boolean valueClassConfirmed) {
        this.valueClassConfirmed = valueClassConfirmed;
    }

    /**
     * Suggest a specific valueClass for the actuator, based on its functional use. This will only set the valueClass
     * once, if not yet otherwise confirmed by GUI user interaction etc. (mixed type usage must remain possible).
     *
     * Performs null and type checking on the actuator.
     *
     * @param actuator
     * @param valueClass
     */
    public static void suggestValueClass(Actuator actuator, Class<?> valueClass) {
        if (actuator instanceof AbstractActuator
                && !((AbstractActuator) actuator).isValueClassConfirmed()) {
            ((AbstractActuator) actuator).setValueClass(valueClass);
            ((AbstractActuator) actuator).setValueClassConfirmed(true);
        }
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    protected void coordinateWithMachine(boolean unconditional) throws Exception {
        Machine machine = Configuration.get().getMachine();
        if (!machine.isTask(Thread.currentThread())) {
            throw new Exception("Actuator "+getName()+" must not coordinate with machine when actuated outside machine task.");
        }
        machine.getMotionPlanner()
        .waitForCompletion(null, unconditional ?
                CompletionType.WaitForUnconditionalCoordination
                : CompletionType.WaitForStillstand);
    }

    @Override
    public Object read(Object value) throws Exception {
        if (isCoordinatedBeforeRead()) {
            coordinateWithMachine(false);
        }
        return null;
    }

    @Override
    public boolean isCoordinatedBeforeActuate() {
        return coordinatedBeforeActuate;
    }

    public void setCoordinatedBeforeActuate(boolean coordinateBeforeActuate) {
        this.coordinatedBeforeActuate = coordinateBeforeActuate;
    }

    @Override
    public boolean isCoordinatedAfterActuate() {
        return coordinatedAfterActuate;
    }

    public void setCoordinatedAfterActuate(boolean coordinateAfterActuate) {
        this.coordinatedAfterActuate = coordinateAfterActuate;
    }

    @Override
    public boolean isCoordinatedBeforeRead() {
        return coordinatedBeforeRead;
    }

    public void setCoordinatedBeforeRead(boolean coordinateBeforeRead) {
        this.coordinatedBeforeRead = coordinateBeforeRead;
    }

    public boolean isInterlockActuator() {
        return interlockActuator;
    }

    public void setInterlockActuator(boolean interlockActuator) {
        boolean oldValue = this.interlockActuator;
        this.interlockActuator = interlockActuator;
        firePropertyChange("interlockActuator", oldValue, interlockActuator);
        if (oldValue != interlockActuator) {
            if (interlockActuator) {
                if (interlockMonitor == null) {
                    setInterlockMonitor(new ActuatorInterlockMonitor());
                }
            }
            else {
                if (interlockMonitor != null) {
                    setInterlockMonitor(null);
                }
            }
        }
    }

    @Override
    public InterlockMonitor getInterlockMonitor() {
        return interlockMonitor;
    }

    public void setInterlockMonitor(InterlockMonitor interlockMonitor) {
        this.interlockMonitor = interlockMonitor;
    }
}
