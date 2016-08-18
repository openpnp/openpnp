/*
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

package org.openpnp.machine.reference.feeder;

import org.onvif.ver10.schema.Config;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.feeder.wizards.ReferenceAutoMountableFeederConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.machine.reference.ReferencePnpJobProcessor;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.List;

/**
 * Not yet finished feeder that will be used for automated feeding. Just getting the idea down
 * on paper, as it were. It will have an actuator attached and you will be able to choose
 * to either toggle the feeder with a delay or send a double.
 */

public class ReferenceAutoMountableFeeder extends ReferenceFeeder {
    private final static Logger logger = LoggerFactory.getLogger(ReferenceAutoMountableFeeder.class);

    @Attribute(required=false)
    protected String actuatorName;
    
    @Attribute(required=false)
    protected double actuatorValue;
    
    @Override
    public Location getPickLocation() throws Exception {
        if (parent == null) {
            logger.warn("No parent specified for feeder.");
            return null;
        }

        return parent.getPickLocation();
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        if (parent == null) {
            /*
                   If our parent is null then we aren't mounted, yet the jobprocessor wants us to feed,
                   so prompt the user and get them to mount the feeder...

                   Really this needs some logic to suggest which is the best bank, but for now, we just use
                   the first one in our configuration?
             */
            List<Feeder> feeders = Configuration.get().getMachine().getFeeders();

            Feeder suggestedSlot = null;

            // look for any empty slots we could use?
            Boolean foundEmptySlot = false;
            for(int i=0;i<feeders.size();i++)
            {
                if(feeders.get(i).supportsChildren())
                {
                    if(feeders.get(i).getChild() == null)
                    {
                         foundEmptySlot = true;
                         suggestedSlot = feeders.get(i);
                         break;
                    }
                }
            }

            Boolean foundUnusedSlot = false;
            if(!foundEmptySlot)
            {
                // all our slots are in use... loop round the job and see if any won't get used anymore
                ReferencePnpJobProcessor jobProcessor = (ReferencePnpJobProcessor) Configuration.get().getMachine().getPnpJobProcessor();
                List<ReferencePnpJobProcessor.JobPlacement> jobPlacements = jobProcessor.GetJobPlacements();

                for (Feeder feeder: feeders) {
                    if(feeder.supportsChildren()) {

                        Boolean feederUsed = false;
                        for (ReferencePnpJobProcessor.JobPlacement jobPlacement : jobPlacements) {
                            if (jobPlacement.status == ReferencePnpJobProcessor.JobPlacement.Status.Complete) {
                                continue;
                            }
                            if (jobPlacement.placement.getPart() == feeder.getChild().getPart())
                            {
                                feederUsed = true;
                                break;
                            }
                        }

                        if(feederUsed == false)
                        {
                            suggestedSlot = feeder;
                            foundUnusedSlot = true;
                            break;
                        }
                    }
                }
            }

            if(!foundEmptySlot && !foundUnusedSlot)
            {
                    // no free slots, and we have all the feeders currently mounted in the banks still needed by the rest of the program
                    // for now... we just tell the user to put the feeder in the first bank slot.

                    for (Feeder feeder: feeders)
                    {
                        if (feeder.supportsChildren())
                        {
                            suggestedSlot = feeder;
                            break;
                        }
                    }
            }

            String msg = "Feeder '"+this.getName() + "' is required but not mounted into a feeder slot, please mount it into feeder slot '"+suggestedSlot.getName()+"' and press OK.";

            MessageBoxes.infoBox("Feeder change required", msg);

            // For now we presume the user has done as they are told.

            Feeder feederSlotCopy = suggestedSlot;
            if(feederSlotCopy!=null)
            {
                if(suggestedSlot.getChild() != null)
                {
                    ReferenceAutoMountableFeeder existingChildFeeder = (ReferenceAutoMountableFeeder) suggestedSlot.getChild();
                    existingChildFeeder.setParent(null);
                    Configuration.get().getMachine().addFeeder(existingChildFeeder);
                    Configuration.get().getMachine().removeFeeder(suggestedSlot.getChild());
                }

                feederSlotCopy.setChild(this);
                Configuration.get().getMachine().addFeeder(feederSlotCopy);
                Configuration.get().getMachine().removeFeeder(suggestedSlot);
            }

            ReferenceAutoMountableFeeder feederCopy = this;
            if(feederCopy!=null) {
                feederCopy.setParent(suggestedSlot);
                Configuration.get().getMachine().addFeeder(feederCopy);
                Configuration.get().getMachine().removeFeeder(this);
            }


        }

        Actuator actuator = Configuration.get().getMachine().getActuatorByName(actuatorName);
        if(actuator!=null) {
            actuator.actuate(actuatorValue);
        }
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceAutoMountableFeederConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean isChildFeeder() { return true; }

    @Override
    public void setParent(Feeder parent) { if(parent==null) { this.parentID=null; this.parent=null; } else { this.parentID=parent.getId(); this.parent=parent; } }

}
