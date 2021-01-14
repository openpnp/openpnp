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

    @Attribute(required = false)
    protected ActuatorValueType valueType = ActuatorValueType.Boolean;

    @Attribute(required = false)
    protected boolean valueTypeConfirmed = false;

    @Attribute(required = false)
    protected Double defaultOnDouble = 0.0;
    @Attribute(required = false)
    protected String defaultOnString = "";

    @Attribute(required = false)
    protected Double defaultOffDouble = 0.0;
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
    public ActuatorValueType getValueType() {
        return valueType;
    }

    public void setValueType(ActuatorValueType valueType) {
        Object oldValue = this.valueType; 
        this.valueType = valueType;
        if (oldValue != valueType) {
            valueTypeConfirmed = true;
            firePropertyChange("valueType", oldValue, valueType);
        }
    }

    /**
     * @return true if the valueType was confirmed by functional usage or user interaction.
     */
    public boolean isValueTypeConfirmed() {
        return valueTypeConfirmed;
    }

    public void setValueTypeConfirmed(boolean valueTypeConfirmed) {
        this.valueTypeConfirmed = valueTypeConfirmed;
    }

    /**
     * Suggest a specific valueType for the actuator, based on its functional use. This will only set the valueType
     * once, if not yet otherwise confirmed by GUI user interaction etc. (mixed type usage must remain possible).  
     * 
     * Performs null and type checking on the actuator.   
     * 
     * @param actuator
     * @param valueType
     */
    public static void suggestValueType(Actuator actuator, ActuatorValueType valueType) {
        if (actuator instanceof AbstractActuator
                && !((AbstractActuator) actuator).isValueTypeConfirmed()) {
            ((AbstractActuator) actuator).setValueType(valueType);
            ((AbstractActuator) actuator).setValueTypeConfirmed(true);
        }
    }

    /**
     * As the user might change the valueType() in the life-time of the actuator, we try to re-interpret
     * persisted values in the most tolerant fashion. 
     * 
     * @param value
     * @return 
     */
    public Object typedValue(Object value) {
        if (value == null) {
            return getDefaultOffValue();
        }
        // Tolerant conversion in case the user changed the type.
        switch (getValueType()) {
            case Boolean:
                if (value instanceof Boolean) { 
                    return (Boolean)value;
                }
                else if (value instanceof Double) {
                    return (Double)value != 0.0;
                }
                else if (value instanceof String) {
                    try {
                        return !(((String)value).isEmpty() 
                                || ((String)value).equals(Boolean.FALSE.toString()) 
                                || Double.valueOf((String) value) == 0.0);
                    }
                    catch (NumberFormatException e) {
                        return false;
                    }
                }
                return false;
            case Double:
                    if (value instanceof Boolean) { 
                        return (Boolean)value ? getDefaultOnValue() : getDefaultOffValue();
                    }
                    else if (value instanceof Double) {
                        return (Double)value;
                    }
                    else if (value instanceof String) {
                        if (((String)value).equals(Boolean.TRUE.toString())) {
                            return getDefaultOnValue();
                        }
                        try {
                            return Double.valueOf((String) value);
                        }
                        catch (NumberFormatException e) {
                            return getDefaultOffValue();
                        }
                    }
                    return getDefaultOffValue();
            default:
                if (value instanceof Boolean) { 
                    return (Boolean)value ? getDefaultOnValue() : getDefaultOffValue();
                }
                return value.toString();
        }
    }

    /**
     * Convenience null- and type-checked call to typedValue().  
     * 
     * @param value
     * @param actuator
     * @return
     */
    public static Object typedValue(Object value, Actuator actuator) {
        if (actuator instanceof AbstractActuator) {
            return ((AbstractActuator) actuator).typedValue(value);
        }
        return null;
    }

    public Double getDefaultOnDouble() {
        return defaultOnDouble;
    }

    public void setDefaultOnDouble(Double defaultOnDouble) {
        this.defaultOnDouble = defaultOnDouble;
    }

    public String getDefaultOnString() {
        return defaultOnString;
    }

    public void setDefaultOnString(String defaultOnString) {
        this.defaultOnString = defaultOnString;
    }

    public Double getDefaultOffDouble() {
        return defaultOffDouble;
    }

    public void setDefaultOffDouble(Double defaultOffDouble) {
        this.defaultOffDouble = defaultOffDouble;
    }

    public String getDefaultOffString() {
        return defaultOffString;
    }

    public void setDefaultOffString(String defaultOffString) {
        this.defaultOffString = defaultOffString;
    }

    protected abstract String getDefaultOnProfile();

    protected abstract String getDefaultOffProfile();

    @Override
    public Object getDefaultOnValue() {
        switch (getValueType()) {
            case Boolean:
                return true;
            case Double:
                return getDefaultOnDouble();
            case String:
                return getDefaultOnString();
            case Profile:
                return getDefaultOnProfile();
        }
        return null;
   }

    @Override
    public Object getDefaultOffValue() {
        switch (getValueType()) {
            case Boolean:
                return false;
            case Double:
                return getDefaultOffDouble();
            case String:
                return getDefaultOffString();
            case Profile:
                return getDefaultOffProfile();
        }
        return null;
    }

    @Override 
    public boolean isActuated() { 
        Object defaultOff = getDefaultOffValue();
        Object last = getLastActuationValue();
        return defaultOff != null && last != null && !defaultOff.equals(last);
    }

    @Override
    public void actuate(Object value) throws Exception {
        if (value instanceof Boolean) {
            assertOnOffDefined();
        }

        switch (getValueType()) {
            case Boolean:
                actuate((boolean)typedValue(value));
                break;
            case Double:
                actuate((double)typedValue(value));
                break;
            case String:
                actuate((String)typedValue(value));
                break;
            case Profile:
                actuateProfile((String)typedValue(value));
                break;
            default:
                throw new Exception("Unsupported valueType "+getValueType());
        }
    }

    public void assertOnOffDefined() throws Exception {
        if (getDefaultOnValue() == null 
                || getDefaultOffValue() == null 
                || getDefaultOnValue().equals(getDefaultOffValue())) {
            throw new Exception("Actuator "+getName()+" has Default ON/OFF values not properly configured.");
        }
    }
    
    static public void assertOnOffDefined(Actuator actuator) throws Exception {
        if (actuator instanceof AbstractActuator) {
            ((AbstractActuator) actuator).assertOnOffDefined();
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
    public String read() throws Exception {
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
