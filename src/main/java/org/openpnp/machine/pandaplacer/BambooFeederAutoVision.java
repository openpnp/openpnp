/*
 * Copyright (C) 2024 <pandaplacer.ca@gmail.com>
 * based on the ReferencePushPullFeeder
 * Copyright (C) 2020 <mark@makr.zone>
 * based on the ReferenceLeverFeeder
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 *
 * This file is part of OpenPnP.
 *
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.pandaplacer;

import javax.swing.Action;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
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
import org.simpleframework.xml.core.Persist;

public class BambooFeederAutoVision extends AbstractPandaplacerVisionFeeder {

    @Attribute(required=false)
    private String feedActuatorName;
    protected Actuator feedActuator;

    @Attribute(required=false)
    protected double feedActuatorValue;

    @Attribute(required=false)
    private String postPickActuatorName;
    protected Actuator postPickActuator;

    @Attribute(required=false)
    protected double postPickActuatorValue;

    @Attribute(required=false)
    protected boolean moveBeforeFeed = false;

    public String getFeedActuatorName() {
        return feedActuatorName;
    }

    public Actuator getFeedActuator() {
        return this.feedActuator;
    }

    public void setFeedActuator(Actuator feedActuator) {
        Object oldValue = this.feedActuator;
        this.feedActuator = feedActuator;
        firePropertyChange("feedActuator", oldValue, feedActuator);

//        Object oldValue2 = this.feedActuatorName;
        feedActuatorName = (feedActuator == null ? null : feedActuator.getName());
//        firePropertyChange("feedActuatorName", oldValue2, feedActuatorName);
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

    public Actuator getPostPickActuator() {
        return this.postPickActuator;
    }

    public void setPostPickActuator(Actuator postPickActuator) {
        Object oldValue = this.postPickActuator;
        this.postPickActuator = postPickActuator;
        firePropertyChange("postPickActuator", oldValue, postPickActuator);

//        Object oldValue2 = this.postPickActuatorName;
        postPickActuatorName = (postPickActuator == null ? null : postPickActuator.getName());
//        firePropertyChange("postPickActuatorName", oldValue2, postPickActuatorName);
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


  public BambooFeederAutoVision() {

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

  @Persist
  private void persist() {
    // Make sure the newest names are persisted (legacy way).
    feedActuatorName = (feedActuator == null ? null : feedActuator.getName());
    postPickActuatorName = (postPickActuator == null ? null : postPickActuator.getName());
  }

  // inherited from ReferenceFeeder
  @Override
  public void feed(Nozzle nozzle) throws Exception {
      if (isMoveBeforeFeed()) {
          MovableUtils.moveToLocationAtSafeZ(nozzle, getPickLocation().derive(null, null, Double.NaN, null));
      }

      Logger.debug("feed({})", nozzle);

      Head head = nozzle.getHead();

      if (getFeedOptions() == FeedOptions.Normal) {
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
      } else {
        assertCalibrated(false);
        head.moveToSafeZ();
      }

      if (getFeedOptions() == FeedOptions.SkipNext) {
        setFeedOptions(FeedOptions.Normal);
      }

      if (getFeedOptions() == FeedOptions.Normal) {
        // increment feed count
        setFeedCount(getFeedCount()+1);
      }
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

    @Override
    public boolean canTakeBackPart() {
        return (getFeedOptions() != FeedOptions.SkipNext && getFeedCount() > 0);
    }

    @Override
    public void takeBackPart(Nozzle nozzle) throws Exception {
        // first check if we can and want to take back this part (should be always be checked before calling, but to be sure)
        if (nozzle.getPart() == null) {
            throw new UnsupportedOperationException("No part loaded that could be taken back.");
        }
        if (!nozzle.getPart().equals(getPart())) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Can not take back " + nozzle.getPart().getId() + " this feeder only supports " + getPart().getId());
        }
        if (!canTakeBackPart()) {
            throw new UnsupportedOperationException("Feeder: " + getName() + " - Currently no free slot. Can not take back the part.");
        }

        // ok, now put the part back on the location of the last pick
        nozzle.moveToPickLocation(this);
        nozzle.place();
        nozzle.moveToSafeZ();
        if (nozzle.isPartOffEnabled(Nozzle.PartOffStep.AfterPlace) && !nozzle.isPartOff()) {
            throw new Exception("Feeder: " + getName() + " - Putting part back failed, check nozzle tip");
        }
        feedOptions = FeedOptions.SkipNext;
    }

// standard wizard overrides
    @Override
    public Wizard getConfigurationWizard() {
        return new BambooFeederAutoVisionConfigurationWizard(this);
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
