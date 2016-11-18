package org.openpnp.machine.reference;

import org.apache.commons.io.IOUtils;
import org.opencv.core.RotatedRect;
import org.opencv.features2d.KeyPoint;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ReferenceNozzleTipConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.*;
import org.openpnp.spi.base.AbstractNozzleTip;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage.Result;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.stream.Collectors;

public class ZevatechNozzleTip extends ReferenceNozzleTip {
    @Element(required = false)
    private int toolchangerID = -1;

    public int getToolchangerID()
    {
        return toolchangerID;
    }
    public void setToolchangerID(int id)
    {
        toolchangerID = id;
    }
}
