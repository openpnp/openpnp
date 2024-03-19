package org.openpnp.machine.reference.feeder;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.feeder.wizards.ReferenceAutoVisionFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

public class ReferenceAutoVisionFeeder extends ReferenceVisionFeeder {

	@Attribute(required=false)
    protected String feedActuatorName;
	protected Actuator feedActuator;
    
    @Attribute(required=false)
    protected double feedActuatorValue;

    @Attribute(required=false)
    protected String postPickActuatorName;
	protected Actuator postPickActuator;

    @Attribute(required=false)
    protected double postPickActuatorValue;
    
    @Attribute(required=false)
    protected boolean moveBeforeFeed = false;
	
    public String getFeedActuatorName() {
        return feedActuatorName;
    }

    public void setFeedActuatorName(String feedActuatorName) {
        this.feedActuatorName = feedActuatorName;
    }

    public double getFeedActuatorValue() {
        return feedActuatorValue;
    }

    public void setFeedActuatorValue(double feedActuatorValue) {
        this.feedActuatorValue = feedActuatorValue;
    }

    public String getPostPickActuatorName() {
        return postPickActuatorName;
    }

    public void setPostPickActuatorName(String postPickActuatorName) {
        this.postPickActuatorName = postPickActuatorName;
    }

    public double getPostPickActuatorValue() {
        return postPickActuatorValue;
    }

    public void setPostPickActuatorValue(double postPickActuatorValue) {
        this.postPickActuatorValue = postPickActuatorValue;
    }

    public boolean isMoveBeforeFeed() {
		return moveBeforeFeed;
	}

	public void setMoveBeforeFeed(boolean moveBeforeFeed) {
		this.moveBeforeFeed = moveBeforeFeed;
	}

	
	public ReferenceAutoVisionFeeder() {
    	
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                // Resolve the actuators by name (legacy way).
                Machine machine = Configuration.get().getMachine();
                try {
                    feedActuator = machine.getActuatorByName(feedActuatorName);
                }
                catch (Exception e) {
                }
                try {
                	postPickActuator = machine.getActuatorByName(postPickActuatorName);
                }
                catch (Exception e) {
                }
                // Listen to the machine become unhomed to invalidate feeder calibration.
                // Note that home()  first switches the machine isHomed() state off, then on again, 
                // so we also catch re-homing.
                Configuration.get().getMachine().addListener(new MachineListener.Adapter() {

                    @Override
                    public void machineHeadActivity(Machine machine, Head head) {
                        checkHomedState(machine);
                    }

                    @Override
                    public void machineEnabled(Machine machine) {
                        checkHomedState(machine);
                    }
                });
            }
        });
    	
	}

	// inherited from ReferenceFeeder
	@Override
    public void feed(Nozzle nozzle) throws Exception {
        if (isMoveBeforeFeed()) {
            MovableUtils.moveToLocationAtSafeZ(nozzle, getPickLocation().derive(null, null, Double.NaN, null));
        }

        Logger.debug("feed({})", nozzle);

        Head head = nozzle.getHead();

        if (getFeedCount() % getPartsPerFeedCycle() == 0) {
            // Modulo of feed count is zero - no more parts there to pick, must feed

            // Make sure we're calibrated
            assertCalibrated(false);

            long feedsPerPart = (long)Math.ceil(getPartPitch().divide(getFeedPitch()));
            long n = feedsPerPart;
            for (long i = 0; i < n; i++) {  // perform multiple feed actuations if required

                // Actuate the tape once for the length equal to Tape Pitch
            	feedTape(nozzle, getFeedPitch().getValue());
            }

            head.moveToSafeZ();
            // Make sure we're calibrated after type feed
            assertCalibrated(true);
        }
        else {
            Logger.debug("Multi parts feed: skipping tape feed at feed count " + getFeedCount());
        }

        // increment feed count
        setFeedCount(getFeedCount()+1);

    }
	
	private void feedTape(Nozzle nozzle, Double feedPitch) throws Exception {
		if (feedActuatorName == null || feedActuatorName.equals("")) {
			throw new Exception("No actuatorName specified for feeder " + getName());
        }
        Actuator actuator = nozzle.getHead().getActuatorByName(feedActuatorName);
        if (actuator == null) {
            actuator = Configuration.get().getMachine().getActuatorByName(feedActuatorName);
        }
        if (actuator == null) {
            throw new Exception("Feed failed. Unable to find an actuator named " + feedActuatorName);
        }
     // Note by using the Object generic method, the value will be properly interpreted according to actuator.valueType.
        actuator.actuate((Object)feedActuatorValue);
	}
	
	@Override
    public void postPick(Nozzle nozzle) throws Exception {
        if (postPickActuatorName == null || postPickActuatorName.equals("")) {
        	Logger.debug("Post pick cancelled. Actuator not set for feeder " + getName());
            return;
        }
        Actuator actuator = nozzle.getHead().getActuatorByName(postPickActuatorName);
        if (actuator == null) {
            actuator = Configuration.get().getMachine().getActuatorByName(postPickActuatorName);
        }
        if (actuator == null) {
            throw new Exception("Post pick failed. Unable to find an actuator named " + postPickActuatorName);
        }
        // Note by using the Object generic method, the value will be properly interpreted according to actuator.valueType.
        actuator.actuate((Object)postPickActuatorValue);
    }
    
// standard wizard overrides
    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceAutoVisionFeederConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard(), "Configuration"),
        };
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

}
