package org.openpnp.machine.reference;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionConfigurationWizard;
import org.openpnp.machine.reference.vision.wizards.ReferenceBottomVisionPartConfigurationWizard;
import org.openpnp.machine.reference.wizards.ZevatechCenteringStageConfigurationWizard;
import org.openpnp.machine.reference.wizards.ZevatechCenteringStagePartConfigurationWizard;
import org.openpnp.model.*;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.SimplePropertySheetHolder;
import org.openpnp.util.MovableUtils;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ZevatechMachine extends ReferenceMachine {
    @Element(required = false)
    PartAlignment centeringStage = new ZevatechCenteringStage();

    public PartAlignment getCenteringStage() { return centeringStage; }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        ArrayList<PropertySheetHolder> children = new ArrayList<>();
        children.add(new SimplePropertySheetHolder("Signalers", getSignalers()));
        children.add(new SimplePropertySheetHolder("Feeders", getFeeders()));
        children.add(new SimplePropertySheetHolder("Heads", getHeads()));
        children.add(new SimplePropertySheetHolder("Cameras", getCameras()));
        children.add(new SimplePropertySheetHolder("Actuators", getActuators()));
        children.add(
                new SimplePropertySheetHolder("Driver", Collections.singletonList(getDriver())));
        children.add(new SimplePropertySheetHolder("Job Processors",
                Arrays.asList(getPnpJobProcessor()/* , getPasteDispenseJobProcessor() */)));

        List<PropertySheetHolder> vision = new ArrayList<>();
        vision.add(getPartAlignment());
        vision.add(getFiducialLocator());
        children.add(new SimplePropertySheetHolder("Vision", vision));

        List<PropertySheetHolder> zevatechSpecific = new ArrayList<>();
        zevatechSpecific.add(getCenteringStage());
        children.add(new SimplePropertySheetHolder("Zevatech", zevatechSpecific));

        return children.toArray(new PropertySheetHolder[] {});
    }
}
