package org.openpnp.machine.reference;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.driver.GcodeDriver;
import org.openpnp.machine.reference.wizards.ReferenceNozzleConfigurationWizard;
import org.openpnp.model.*;
import org.openpnp.spi.NozzleTip;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractNozzle;
import org.openpnp.spi.base.SimplePropertySheetHolder;
import org.openpnp.spi.Actuator;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;

public class ZevatechNozzle extends ReferenceNozzle {
    @Attribute(required = false)
    protected PartAlignment partAlignment = null;

    @Element(required = false)
    private Location nozzleTestPad = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location changerLocation1 = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location changerLocation2 = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location changerLocation3 = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location changerLocation4 = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location changerLocation5 = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private Location changerLocation6 = new Location(LengthUnit.Millimeters);

    @Element(required = false)
    private int nozzleVacuumThreshold = 0;

    public void checkNozzleTipInstalled(Boolean nozzleExpected) throws Exception {
        Boolean bFlag = false;

        Actuator headActuator=Configuration.get().getMachine().getActuatorByName("headUpDown");
        Actuator vacuumTestActuator=Configuration.get().getMachine().getActuatorByName("vacuumTest");
        Actuator vacuumInverseTestActuator=Configuration.get().getMachine().getActuatorByName("vacuumInverseTest");

        moveTo(nozzleTestPad);

        // lower the head
        headActuator.actuate(true);

        // query the vacuum sensor
        if(nozzleExpected) {
            vacuumTestActuator.actuate(nozzleVacuumThreshold);
        }
        else
        {
            vacuumInverseTestActuator.actuate(nozzleVacuumThreshold);
        }

        // raise head
        headActuator.actuate(false);
    }

    @Override
    public void loadNozzleTip(NozzleTip nozzleTip) throws Exception {
        if (this.nozzleTip == nozzleTip) {
            return;
        }

        unloadNozzleTip();

        // check that no nozzle is now installed
        checkNozzleTipInstalled(false);

        // move above the tool changer
        ZevatechNozzleTip nt = (ZevatechNozzleTip) nozzleTip;

        if(nt.getToolchangerID()==-1)
        {
            throw new Exception("Trying to load nozzle, but no toolchanger slot id assigned");
        }

        Actuator actuator=Configuration.get().getMachine().getActuatorByName("toolChanger");

        //  move above the cylinder and then actuate the tool changer
        switch(nt.getToolchangerID())
        {
            case 1:
                moveTo(changerLocation1, getHead().getMachine().getSpeed());
                actuator.actuate(1);
                break;
            case 2:
                moveTo(changerLocation2, getHead().getMachine().getSpeed());
                actuator.actuate(2);
                break;
            case 3:
                moveTo(changerLocation3, getHead().getMachine().getSpeed());
                actuator.actuate(3);
                break;
            case 4:
                moveTo(changerLocation4, getHead().getMachine().getSpeed());
                actuator.actuate(4);
                break;
            case 5:
                moveTo(changerLocation5, getHead().getMachine().getSpeed());
                actuator.actuate(5);
                break;
            case 6:
                moveTo(changerLocation6, getHead().getMachine().getSpeed());
                actuator.actuate(6);
                break;
            default:
                throw new Exception("Invalid toolchanger slot id specified");
        }


        // de-actuate the tool changer
        actuator.actuate(0);

        // check that a nozzle is now installed
        checkNozzleTipInstalled(true);

        this.nozzleTip = (ReferenceNozzleTip) nozzleTip;
        this.nozzleTip.getCalibration().reset();
    }

    @Override
    public void unloadNozzleTip() throws Exception {
        if (nozzleTip == null) {
            return;
        }

        // check that a nozzle is now installed
        checkNozzleTipInstalled(true);

        // move above the tool changer
        ZevatechNozzleTip nt = (ZevatechNozzleTip) nozzleTip;
        Actuator actuator=Configuration.get().getMachine().getActuatorByName("toolChanger");

        if(nt.getToolchangerID()==-1)
        {
            throw new Exception("Trying to unload nozzle, but no toolchanger slot id assigned");
        }

        //  move above the cylinder and then actuate the tool changer
        switch(nt.getToolchangerID())
        {
            case 1:
                moveTo(changerLocation1, getHead().getMachine().getSpeed());
                actuator.actuate(1);
                break;
            case 2:
                moveTo(changerLocation2, getHead().getMachine().getSpeed());
                actuator.actuate(2);
                break;
            case 3:
                moveTo(changerLocation3, getHead().getMachine().getSpeed());
                actuator.actuate(3);
                break;
            case 4:
                moveTo(changerLocation4, getHead().getMachine().getSpeed());
                actuator.actuate(4);
                break;
            case 5:
                moveTo(changerLocation5, getHead().getMachine().getSpeed());
                actuator.actuate(5);
                break;
            case 6:
                moveTo(changerLocation6, getHead().getMachine().getSpeed());
                actuator.actuate(6);
                break;
            default:
                throw new Exception("Invalid toolchanger slot id specified");
        }


        // de-actuate the tool changer
        actuator.actuate(0);

        // check that no nozzle is now installed
        checkNozzleTipInstalled(false);

        nozzleTip = null;
    }

}
