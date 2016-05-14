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

package org.openpnp.machine.reference.feeder.wizards;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.CameraViewActionEvent;
import org.openpnp.gui.components.CameraViewActionListener;
import org.openpnp.gui.components.CameraViewFilter;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.machine.reference.feeder.ReferenceStripFeeder;
import org.openpnp.machine.reference.feeder.ReferenceStripFeeder.TapeType;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Camera;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.FluentCv;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceStripFeederConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceStripFeeder feeder;

    private JPanel panelPart;

    private JComboBox comboBoxPart;

    private JTextField textFieldFeedStartX;
    private JTextField textFieldFeedStartY;
    private JTextField textFieldFeedStartZ;
    private JTextField textFieldFeedEndX;
    private JTextField textFieldFeedEndY;
    private JTextField textFieldFeedEndZ;
    private JTextField textFieldTapeWidth;
    private JLabel lblPartPitch;
    private JTextField textFieldPartPitch;
    private JPanel panelTapeSettings;
    private JPanel panelLocations;
    private LocationButtonsPanel locationButtonsPanelFeedStart;
    private LocationButtonsPanel locationButtonsPanelFeedEnd;
    private JLabel lblFeedCount;
    private JTextField textFieldFeedCount;
    private JButton btnResetFeedCount;
    private JLabel lblTapeType;
    private JComboBox comboBoxTapeType;
    private JLabel lblRotationInTape;
    private JTextField textFieldLocationRotation;
    private JButton btnAutoSetup;

    private Location firstPartLocation;
    private Location secondPartLocation;
    private List<Location> part1HoleLocations;
    private Camera autoSetupCamera;


    public ReferenceStripFeederConfigurationWizard(ReferenceStripFeeder feeder) {
        this.feeder = feeder;

        panelPart = new JPanel();
        panelPart.setBorder(
                new TitledBorder(null, "Part", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelPart);
        panelPart.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        comboBoxPart = new JComboBox();
        try {
            comboBoxPart.setModel(new PartsComboBoxModel());
        }
        catch (Throwable t) {
            // Swallow this error. This happens during parsing in
            // in WindowBuilder but doesn't happen during normal run.
        }
        comboBoxPart.setRenderer(new IdentifiableListCellRenderer<Part>());
        panelPart.add(comboBoxPart, "2, 2, 3, 1, left, default");

        lblRotationInTape = new JLabel("Rotation In Tape");
        panelPart.add(lblRotationInTape, "2, 4, left, default");

        textFieldLocationRotation = new JTextField();
        panelPart.add(textFieldLocationRotation, "4, 4, fill, default");
        textFieldLocationRotation.setColumns(4);

        panelTapeSettings = new JPanel();
        contentPanel.add(panelTapeSettings);
        panelTapeSettings.setBorder(new TitledBorder(
                new EtchedBorder(EtchedBorder.LOWERED, null, null), "Tape Settings",
                TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelTapeSettings.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        btnAutoSetup = new JButton(autoSetup);
        panelTapeSettings.add(btnAutoSetup, "2, 2, 11, 1");

        lblTapeType = new JLabel("Tape Type");
        panelTapeSettings.add(lblTapeType, "2, 4, right, default");

        comboBoxTapeType = new JComboBox(TapeType.values());
        panelTapeSettings.add(comboBoxTapeType, "4, 4, fill, default");

        JLabel lblTapeWidth = new JLabel("Tape Width");
        panelTapeSettings.add(lblTapeWidth, "8, 4, right, default");

        textFieldTapeWidth = new JTextField();
        panelTapeSettings.add(textFieldTapeWidth, "10, 4");
        textFieldTapeWidth.setColumns(5);

        lblPartPitch = new JLabel("Part Pitch");
        panelTapeSettings.add(lblPartPitch, "2, 6, right, default");

        textFieldPartPitch = new JTextField();
        panelTapeSettings.add(textFieldPartPitch, "4, 6");
        textFieldPartPitch.setColumns(5);

        lblFeedCount = new JLabel("Feed Count");
        panelTapeSettings.add(lblFeedCount, "8, 6, right, default");

        textFieldFeedCount = new JTextField();
        panelTapeSettings.add(textFieldFeedCount, "10, 6");
        textFieldFeedCount.setColumns(10);

        btnResetFeedCount = new JButton(new AbstractAction("Reset") {
            @Override
            public void actionPerformed(ActionEvent e) {
                textFieldFeedCount.setText("0");
                applyAction.actionPerformed(e);
            }
        });
        panelTapeSettings.add(btnResetFeedCount, "12, 6");

        lblUseVision = new JLabel("Use Vision?");
        panelTapeSettings.add(lblUseVision, "2, 8");

        chckbxUseVision = new JCheckBox("");
        panelTapeSettings.add(chckbxUseVision, "4, 8");

        panelLocations = new JPanel();
        contentPanel.add(panelLocations);
        panelLocations.setBorder(new TitledBorder(null, "Locations", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        panelLocations.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("left:default:grow"),},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblX = new JLabel("X");
        panelLocations.add(lblX, "4, 2");

        JLabel lblY = new JLabel("Y");
        panelLocations.add(lblY, "6, 2");

        JLabel lblZ_1 = new JLabel("Z");
        panelLocations.add(lblZ_1, "8, 2");

        JLabel lblFeedStartLocation = new JLabel("Reference Hole Location");
        lblFeedStartLocation.setToolTipText(
                "The location of the first tape hole past the first part in the direction of more parts.");
        panelLocations.add(lblFeedStartLocation, "2, 4, right, default");

        textFieldFeedStartX = new JTextField();
        panelLocations.add(textFieldFeedStartX, "4, 4");
        textFieldFeedStartX.setColumns(8);

        textFieldFeedStartY = new JTextField();
        panelLocations.add(textFieldFeedStartY, "6, 4");
        textFieldFeedStartY.setColumns(8);

        textFieldFeedStartZ = new JTextField();
        panelLocations.add(textFieldFeedStartZ, "8, 4");
        textFieldFeedStartZ.setColumns(8);

        locationButtonsPanelFeedStart = new LocationButtonsPanel(textFieldFeedStartX,
                textFieldFeedStartY, textFieldFeedStartZ, null);
        panelLocations.add(locationButtonsPanelFeedStart, "10, 4");

        JLabel lblFeedEndLocation = new JLabel("Next Hole Location");
        lblFeedEndLocation.setToolTipText(
                "The location of another hole after the reference hole. This can be any hole along the tape as long as it's past the reference hole.");
        panelLocations.add(lblFeedEndLocation, "2, 6, right, default");

        textFieldFeedEndX = new JTextField();
        panelLocations.add(textFieldFeedEndX, "4, 6");
        textFieldFeedEndX.setColumns(8);

        textFieldFeedEndY = new JTextField();
        panelLocations.add(textFieldFeedEndY, "6, 6");
        textFieldFeedEndY.setColumns(8);

        textFieldFeedEndZ = new JTextField();
        panelLocations.add(textFieldFeedEndZ, "8, 6");
        textFieldFeedEndZ.setColumns(8);

        locationButtonsPanelFeedEnd = new LocationButtonsPanel(textFieldFeedEndX, textFieldFeedEndY,
                textFieldFeedEndZ, null);
        panelLocations.add(locationButtonsPanelFeedEnd, "10, 6");
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        MutableLocationProxy location = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "location", location, "location");
        addWrappedBinding(location, "rotation", textFieldLocationRotation, "text", doubleConverter);

        addWrappedBinding(feeder, "part", comboBoxPart, "selectedItem");
        addWrappedBinding(feeder, "tapeType", comboBoxTapeType, "selectedItem");

        addWrappedBinding(feeder, "tapeWidth", textFieldTapeWidth, "text", lengthConverter);
        addWrappedBinding(feeder, "partPitch", textFieldPartPitch, "text", lengthConverter);
        addWrappedBinding(feeder, "feedCount", textFieldFeedCount, "text", intConverter);

        MutableLocationProxy feedStartLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "referenceHoleLocation", feedStartLocation,
                "location");
        addWrappedBinding(feedStartLocation, "lengthX", textFieldFeedStartX, "text",
                lengthConverter);
        addWrappedBinding(feedStartLocation, "lengthY", textFieldFeedStartY, "text",
                lengthConverter);
        addWrappedBinding(feedStartLocation, "lengthZ", textFieldFeedStartZ, "text",
                lengthConverter);

        MutableLocationProxy feedEndLocation = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, feeder, "lastHoleLocation", feedEndLocation, "location");
        addWrappedBinding(feedEndLocation, "lengthX", textFieldFeedEndX, "text", lengthConverter);
        addWrappedBinding(feedEndLocation, "lengthY", textFieldFeedEndY, "text", lengthConverter);
        addWrappedBinding(feedEndLocation, "lengthZ", textFieldFeedEndZ, "text", lengthConverter);

        addWrappedBinding(feeder, "visionEnabled", chckbxUseVision, "selected");

        ComponentDecorators.decorateWithAutoSelect(textFieldLocationRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldTapeWidth);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldPartPitch);
        ComponentDecorators.decorateWithAutoSelect(textFieldFeedCount);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedStartX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedStartY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedStartZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldFeedEndZ);
    }

    private Action autoSetup = new AbstractAction("Auto Setup") {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                autoSetupCamera =
                        Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
            }
            catch (Exception ex) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Auto Setup Failure", ex);
                return;
            }

            btnAutoSetup.setAction(autoSetupCancel);

            CameraView cameraView = MainFrame.cameraPanel.getCameraView(autoSetupCamera);
            cameraView.addActionListener(autoSetupPart1Clicked);
            cameraView.setText("Click on the center of the first part in the tape.");
            cameraView.flash();

            final boolean showDetails = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;

            cameraView.setCameraViewFilter(new CameraViewFilter() {
                @Override
                public BufferedImage filterCameraImage(Camera camera, BufferedImage image) {
                    return showHoles(camera, image, showDetails);
                }
            });
        }
    };

    private Action autoSetupCancel = new AbstractAction("Cancel Auto Setup") {
        @Override
        public void actionPerformed(ActionEvent e) {
            btnAutoSetup.setAction(autoSetup);
            CameraView cameraView = MainFrame.cameraPanel.getCameraView(autoSetupCamera);
            cameraView.setText(null);
            cameraView.setCameraViewFilter(null);
            cameraView.removeActionListener(autoSetupPart1Clicked);
            cameraView.removeActionListener(autoSetupPart2Clicked);
        }
    };

    private CameraViewActionListener autoSetupPart1Clicked = new CameraViewActionListener() {
        @Override
        public void actionPerformed(final CameraViewActionEvent action) {
            firstPartLocation = action.getLocation();
            final CameraView cameraView = MainFrame.cameraPanel.getCameraView(autoSetupCamera);
            cameraView.removeActionListener(this);
            Configuration.get().getMachine().submit(new Callable<Void>() {
                public Void call() throws Exception {
                    cameraView.setText("Checking first part...");
                    autoSetupCamera.moveTo(action.getLocation());
                    part1HoleLocations = findHoles(autoSetupCamera);

                    cameraView.setText("Now click on the center of the second part in the tape.");
                    cameraView.flash();

                    cameraView.addActionListener(autoSetupPart2Clicked);
                    return null;
                }
            }, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void result) {}

                @Override
                public void onFailure(final Throwable t) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            autoSetupCancel.actionPerformed(null);
                            MessageBoxes.errorBox(getTopLevelAncestor(), "Auto Setup Failure", t);
                        }
                    });
                }
            });
        }
    };

    private CameraViewActionListener autoSetupPart2Clicked = new CameraViewActionListener() {
        @Override
        public void actionPerformed(final CameraViewActionEvent action) {
            secondPartLocation = action.getLocation();
            final CameraView cameraView = MainFrame.cameraPanel.getCameraView(autoSetupCamera);
            cameraView.removeActionListener(this);
            Configuration.get().getMachine().submit(new Callable<Void>() {
                public Void call() throws Exception {
                    cameraView.setText("Checking second part...");
                    autoSetupCamera.moveTo(action.getLocation());
                    List<Location> part2HoleLocations = findHoles(autoSetupCamera);

                    List<Location> referenceHoles =
                            deriveReferenceHoles(part1HoleLocations, part2HoleLocations);
                    final Location referenceHole1 = referenceHoles.get(0);
                    final Location referenceHole2 = referenceHoles.get(1);

                    feeder.setReferenceHoleLocation(referenceHole1);
                    feeder.setLastHoleLocation(referenceHole2);

                    Length partPitch = firstPartLocation.getLinearLengthTo(secondPartLocation);
                    partPitch.setValue(2 * (Math.round(partPitch.getValue() / 2)));

                    final Length partPitch_ = partPitch;
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            Helpers.copyLocationIntoTextFields(referenceHole1, textFieldFeedStartX,
                                    textFieldFeedStartY, textFieldFeedStartZ);
                            Helpers.copyLocationIntoTextFields(referenceHole2, textFieldFeedEndX,
                                    textFieldFeedEndY, textFieldFeedEndZ);
                            textFieldPartPitch.setText(partPitch_.getValue() + "");
                        }
                    });

                    feeder.setFeedCount(1);
                    autoSetupCamera.moveTo(feeder.getPickLocation());
                    feeder.setFeedCount(0);

                    cameraView.setText("Setup complete!");
                    Thread.sleep(1500);
                    cameraView.setText(null);
                    cameraView.setCameraViewFilter(null);
                    btnAutoSetup.setAction(autoSetup);

                    return null;
                }
            }, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void result) {}

                @Override
                public void onFailure(final Throwable t) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            autoSetupCancel.actionPerformed(null);
                            MessageBoxes.errorBox(getTopLevelAncestor(), "Auto Setup Failure", t);
                        }
                    });
                }
            });
        }
    };

    private List<Location> findHoles(Camera camera) {
        List<Location> holeLocations = new ArrayList<>();
        new FluentCv().setCamera(camera).settleAndCapture().toGray()
                .blurGaussian(feeder.getHoleBlurKernelSize())
                .findCirclesHough(feeder.getHoleDiameterMin(), feeder.getHoleDiameterMax(),
                        feeder.getHolePitchMin())
                .filterCirclesByDistance(feeder.getHoleDistanceMin(), feeder.getHoleDistanceMax())
                .filterCirclesToLine(feeder.getHoleLineDistanceMax())
                .convertCirclesToLocations(holeLocations);
        return holeLocations;
    }

    /**
     * Show candidate holes in the image. Red are any holes that are found. Blue is holes that
     * passed the distance check but failed the line check. Green passed all checks and are good.
     * 
     * @param camera
     * @param image
     * @return
     */
    private BufferedImage showHoles(Camera camera, BufferedImage image, boolean showDetails) {
        if (showDetails) {
            return new FluentCv().setCamera(camera).toMat(image, "original").toGray()
                    .blurGaussian(feeder.getHoleBlurKernelSize())
                    .findCirclesHough(feeder.getHoleDiameterMin(), feeder.getHoleDiameterMax(),
                            feeder.getHolePitchMin(), "houghUnfiltered")
                    .drawCircles("original", Color.red, "unfiltered").recall("houghUnfiltered")
                    .filterCirclesByDistance(feeder.getHoleDistanceMin(),
                            feeder.getHoleDistanceMax(), "houghDistanceFiltered")
                    .drawCircles("unfiltered", Color.blue, "distanceFiltered")
                    .recall("houghDistanceFiltered")
                    .filterCirclesToLine(feeder.getHoleLineDistanceMax())
                    .drawCircles("distanceFiltered", Color.green).toBufferedImage();
        }
        else {
            return new FluentCv().setCamera(camera).toMat(image, "original").toGray()
                    .blurGaussian(feeder.getHoleBlurKernelSize())
                    .findCirclesHough(feeder.getHoleDiameterMin(), feeder.getHoleDiameterMax(),
                            feeder.getHolePitchMin())
                    .filterCirclesByDistance(feeder.getHoleDistanceMin(),
                            feeder.getHoleDistanceMax())
                    .filterCirclesToLine(feeder.getHoleLineDistanceMax())
                    .drawCircles("original", Color.green).toBufferedImage();
        }
    }

    private List<Location> deriveReferenceHoles(List<Location> part1HoleLocations,
            List<Location> part2HoleLocations) {
        // We are only interested in the pair of holes closest to each part
        part1HoleLocations = part1HoleLocations.subList(0, Math.min(2, part1HoleLocations.size()));
        part2HoleLocations = part2HoleLocations.subList(0, Math.min(2, part2HoleLocations.size()));

        // Part 1's reference hole is the one closest to either of part 2's holes.
        Location part1ReferenceHole = VisionUtils
                .sortLocationsByDistance(part2HoleLocations.get(0), part1HoleLocations).get(0);
        // Part 2's reference hole is the one farthest from part 1's reference hole.
        Location part2ReferenceHole = Lists
                .reverse(
                        VisionUtils.sortLocationsByDistance(part1ReferenceHole, part2HoleLocations))
                .get(0);

        List<Location> referenceHoles = new ArrayList<>();
        referenceHoles.add(part1ReferenceHole);
        referenceHoles.add(part2ReferenceHole);
        return referenceHoles;
    }

    private JCheckBox chckbxUseVision;
    private JLabel lblUseVision;
}
