package org.openpnp.spi.base;

import javax.swing.Icon;

import org.openpnp.ConfigurationListener;
import org.openpnp.machine.reference.ActuatorInterlockMonitor;
import org.openpnp.machine.reference.ActuatorInterlockMonitor.InterlockType;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Driver;
import org.openpnp.spi.Head;
import org.openpnp.spi.Actuator.InterlockMonitor;
import org.openpnp.spi.MotionPlanner.CompletionType;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

public abstract class AbstractActuator extends AbstractHeadMountable implements Actuator {
    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

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
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    protected void coordinateWithMachine(boolean unconditional) throws Exception {
        Configuration.get().getMachine().getMotionPlanner()
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
