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

package org.openpnp.machine.reference.wizards;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.ReferenceJobProcessor;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceJobProcessorConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceJobProcessor jobProcessor;
    private JPanel panelGeneral;
    private JCheckBox chckbxDemoMode;
    private JTextField heightTextField;
    private JLabel lblNewLabel;
    private JButton btnGo;
    private JLabel lblName;
    private JTextField nameTextField;

    public ReferenceJobProcessorConfigurationWizard(ReferenceJobProcessor jobProcessor) {
        this.jobProcessor = jobProcessor;

        panelGeneral = new JPanel();
        panelGeneral.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Settings", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelGeneral);
        panelGeneral
                .setLayout(
                        new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        chckbxDemoMode = new JCheckBox("Demo Mode?");
        panelGeneral.add(chckbxDemoMode, "2, 2");
        
        JButton btnBottomVision = new JButton("Bottom Vision");
        btnBottomVision.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO: Move this into CvPipelineEditor as showDialog for convenience.
                CvPipeline pipeline = jobProcessor.getBottomVisionPipeline();
                pipeline.setCamera(Configuration.get().getMachine().getCameras().get(0));
                CvPipelineEditor editor = new CvPipelineEditor(pipeline);
                JDialog dialog = new JDialog(MainFrame.mainFrame, "Bottom Vision Pipeline");
                dialog.getContentPane().setLayout(new BorderLayout());
                dialog.getContentPane().add(editor);
                dialog.setSize(1024,  768);
                dialog.setVisible(true);
            }
        });
        panelGeneral.add(btnBottomVision, "2, 4");
        
        lblNewLabel = new JLabel("Height");
        panelGeneral.add(lblNewLabel, "2, 6");
        
        lblName = new JLabel("Name");
        panelGeneral.add(lblName, "4, 6");
        
        heightTextField = new JTextField();
        panelGeneral.add(heightTextField, "2, 8, fill, default");
        heightTextField.setColumns(10);
        
        btnGo = new JButton("Go");
        btnGo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                goBottomVisionTestThing();
            }
        });
        
        nameTextField = new JTextField();
        panelGeneral.add(nameTextField, "4, 8, fill, default");
        nameTextField.setColumns(10);
        panelGeneral.add(btnGo, "6, 8");
    }
    
    private void goBottomVisionTestThing() {
        UiUtils.submitUiMachineTask(() -> {
            Camera camera = Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
            Nozzle nozzle = Configuration.get().getMachine().getDefaultHead().getDefaultNozzle();
            Camera bottomCamera = Configuration.get().getMachine().getCameras().get(0);
            Location originalLocation = camera.getLocation();

            String name = nameTextField.getText();
            double height = Double.parseDouble(heightTextField.getText());
            double z = -21.3 + height;
            System.out.println("height " + height);
            System.out.println("z " + z);

            // pick the part currently under the camera
            Location pickLocation = originalLocation.derive(null, null, z, null);
            System.out.println("pickLocation " + pickLocation);
            MovableUtils.moveToLocationAtSafeZ(nozzle, pickLocation, 1.0);
            nozzle.pick();
            Thread.sleep(750);

            // image the part
            Location imageLocation = bottomCamera.getLocation();
            imageLocation = imageLocation.add(new Location(LengthUnit.Millimeters, 0, 0, height, 0));
            System.out.println("imageLocation " + imageLocation);
            
            BufferedImage image;
            
            MovableUtils.moveToLocationAtSafeZ(nozzle, imageLocation, 1.0);
            Thread.sleep(500);
            image = bottomCamera.settleAndCapture();
            ImageIO.write(image, "png", new File("/Users/jason/Desktop/test/" + name + "_r00.png"));

            imageLocation = imageLocation.derive(null, null, null, 45d);
            MovableUtils.moveToLocationAtSafeZ(nozzle, imageLocation, 1.0);
            Thread.sleep(500);
            image = bottomCamera.settleAndCapture();
            ImageIO.write(image, "png", new File("/Users/jason/Desktop/test/" + name + "_r45.png"));

            imageLocation = imageLocation.derive(null, null, null, 90d);
            MovableUtils.moveToLocationAtSafeZ(nozzle, imageLocation, 1.0);
            Thread.sleep(500);
            image = bottomCamera.settleAndCapture();
            ImageIO.write(image, "png", new File("/Users/jason/Desktop/test/" + name + "_r90.png"));

            // replace the part
            MovableUtils.moveToLocationAtSafeZ(nozzle, pickLocation, 1.0);
            nozzle.place();
            Thread.sleep(750);
            
            // move camera back to where it started
            MovableUtils.moveToLocationAtSafeZ(camera, originalLocation, 1.0);
        });
    }

    @Override
    public void createBindings() {
        addWrappedBinding(jobProcessor, "demoMode", chckbxDemoMode, "selected");
    }
}
