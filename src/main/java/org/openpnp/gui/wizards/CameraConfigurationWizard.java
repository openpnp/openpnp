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

package org.openpnp.gui.wizards;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.SimpleGraphView;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.LongConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.spi.base.AbstractCamera;
import org.openpnp.spi.base.AbstractCamera.SettleMethod;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JCheckBox;
import java.awt.Font;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

@SuppressWarnings("serial")
public class CameraConfigurationWizard extends AbstractConfigurationWizard {
    private final Camera camera;
    
    private static String uppFormat = "%.8f";

    private JPanel panelUpp;
    private JButton btnMeasure;
    private JButton btnCancelMeasure;
    private JLabel lblUppInstructions;

    public CameraConfigurationWizard(Camera camera) {
        this.camera = camera;
        
        panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Properties", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        lblName = new JLabel("Name");
        panel.add(lblName, "2, 2, right, default");
        
        nameTf = new JTextField();
        panel.add(nameTf, "4, 2");
        nameTf.setColumns(20);
        
        lblLooking = new JLabel("Looking");
        panel.add(lblLooking, "2, 4, right, default");
        
        lookingCb = new JComboBox(Camera.Looking.values());
        panel.add(lookingCb, "4, 4");

        panelUpp = new JPanel();
        contentPanel.add(panelUpp);
        panelUpp.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
                "Units Per Pixel", TitledBorder.LEADING, TitledBorder.TOP, null,
                new Color(0, 0, 0)));
        panelUpp.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        lblWidth = new JLabel("Width");
        panelUpp.add(lblWidth, "2, 2");

        lblHeight = new JLabel("Length");
        panelUpp.add(lblHeight, "4, 2");

        lblX = new JLabel("X");
        panelUpp.add(lblX, "6, 2");

        lblY = new JLabel("Y");
        panelUpp.add(lblY, "8, 2");

        textFieldWidth = new JTextField();
        textFieldWidth.setText("1");
        panelUpp.add(textFieldWidth, "2, 4");
        textFieldWidth.setColumns(8);

        textFieldHeight = new JTextField();
        textFieldHeight.setText("1");
        panelUpp.add(textFieldHeight, "4, 4");
        textFieldHeight.setColumns(8);

        textFieldUppX = new JTextField();
        textFieldUppX.setColumns(8);
        panelUpp.add(textFieldUppX, "6, 4, fill, default");

        textFieldUppY = new JTextField();
        textFieldUppY.setColumns(8);
        panelUpp.add(textFieldUppY, "8, 4, fill, default");

        btnMeasure = new JButton("Measure");
        btnMeasure.setAction(measureAction);
        panelUpp.add(btnMeasure, "10, 4");

        btnCancelMeasure = new JButton("Cancel");
        btnCancelMeasure.setAction(cancelMeasureAction);
        panelUpp.add(btnCancelMeasure, "12, 4");

        lblUppInstructions = new JLabel(
                "<html>\n<ol>\n<li>Place an object with a known width and length on the table. Graphing paper is a good, easy choice for this.\n<li>Enter the width and length of the object into the Width and Length fields.\n<li>Jog the camera to where it is centered over the object and in focus.\n<li>Press Measure and use the camera selection rectangle to measure the object. Press Confirm when finished.\n<li>The calculated units per pixel values will be inserted into the X and Y fields.\n</ol>\n</html>");
        panelUpp.add(lblUppInstructions, "2, 6, 10, 1, default, fill");

        panelVision = new JPanel();
        panelVision.setBorder(new TitledBorder(null, "Vision", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelVision);
        panelVision.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                RowSpec.decode("default:grow"),}));
        
        lblSettleMethod = new JLabel("Settle Method");
        panelVision.add(lblSettleMethod, "2, 2, right, default");
        
        settleMethod = new JComboBox(AbstractCamera.SettleMethod.values());
        settleMethod.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adaptDialog();
            }
        });
        panelVision.add(settleMethod, "4, 2, fill, default");

        lblSettleTimeMs = new JLabel("Settle Time (ms)");
        panelVision.add(lblSettleTimeMs, "2, 4, right, default");

        settleTimeMs = new JTextField();
        panelVision.add(settleTimeMs, "4, 4, fill, default");
        settleTimeMs.setColumns(10);
        
        lblSettleTimeoutMs = new JLabel("Settle Timeout (ms)");
        panelVision.add(lblSettleTimeoutMs, "2, 6, right, default");
        
        settleTimeoutMs = new JTextField();
        panelVision.add(settleTimeoutMs, "4, 6, fill, default");
        settleTimeoutMs.setColumns(10);
        
        lblSettleThreshold = new JLabel("Settle Threshold");
        panelVision.add(lblSettleThreshold, "2, 8, right, default");
        
        settleThreshold = new JTextField();
        panelVision.add(settleThreshold, "4, 8, fill, default");
        settleThreshold.setColumns(10);
        
        lblSettleFullColor = new JLabel("Full Color?");
        panelVision.add(lblSettleFullColor, "2, 10, right, default");
        
        settleFullColor = new JCheckBox("");
        settleFullColor.setToolTipText("Compare as full color image, i.e. different colors with same brightness will register");
        panelVision.add(settleFullColor, "4, 10");
        
        lblSettleGaussianBlur = new JLabel("Gaussian Blur (Pixel)");
        panelVision.add(lblSettleGaussianBlur, "2, 12, right, default");
        
        settleGaussianBlur = new JTextField();
        panelVision.add(settleGaussianBlur, "4, 12, fill, default");
        settleGaussianBlur.setColumns(10);
        
        lblSettleMaskCircle = new JLabel("Center Mask Ratio");
        lblSettleMaskCircle.setToolTipText("<html>\r\n<p>Analyze the movement inside a central circular mask of given size, relative to the camera<br/> dimension (height or width, whichever is smaller.</p>\r\n<p>Examples:</p>\r\n<ul>\r\n<li>0.0 No mask</li>\r\n<li>0.5 Circular center area of half the size of the camera view</li>\r\n<li>1.0 Circular center area to the edge of the camera view</li>\r\n</ul>\r\n</html>");
        panelVision.add(lblSettleMaskCircle, "2, 14, right, default");
        
        settleMaskCircle = new JTextField();
        panelVision.add(settleMaskCircle, "4, 14, fill, default");
        settleMaskCircle.setColumns(10);
        
        lblSettleDiagnostics = new JLabel("Diagnostics?");
        panelVision.add(lblSettleDiagnostics, "2, 16, right, default");
        
        settleDiagnostics = new JCheckBox("");
        settleDiagnostics.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                adaptDialog();
            }
        });
        panelVision.add(settleDiagnostics, "4, 16");
        
        lblSettleGraph = new JLabel("<html>\r\n<body style=\"text-align:right\">\r\n<p>\r\nDifference <span style=\"color:#FF0000\">&mdash;&mdash;</span>\r\n</p>\r\n<p>\r\nThreshold <span style=\"color:#00BB00\">&mdash;&mdash;</span>\r\n</p>\r\n<p>\r\nCapture <span style=\"color:#005BD9\">&mdash;&mdash;</span>\r\n</p>\r\n</body>\r\n</html>");
        panelVision.add(lblSettleGraph, "2, 18");
        
        settleGraph = new SimpleGraphView();
        settleGraph.setFont(new Font("SansSerif", Font.PLAIN, 9));
        panelVision.add(settleGraph, "4, 18, 3, 1, default, fill");
    }

    private void adaptDialog() {
        AbstractCamera.SettleMethod method = (SettleMethod) settleMethod.getSelectedItem();
        boolean fixedTime = (method == SettleMethod.FixedTime);

        lblSettleTimeMs.setVisible(fixedTime);
        settleTimeMs.setVisible(fixedTime);
        lblSettleTimeoutMs.setVisible(!fixedTime);
        settleTimeoutMs.setVisible(!fixedTime);

        lblSettleThreshold.setVisible(!fixedTime);
        settleThreshold.setVisible(!fixedTime);

        lblSettleFullColor.setVisible(!fixedTime);
        settleFullColor.setVisible(!fixedTime);

        lblSettleGaussianBlur.setVisible(!fixedTime);
        settleGaussianBlur.setVisible(!fixedTime);

        lblSettleMaskCircle.setVisible(!fixedTime);
        settleMaskCircle.setVisible(!fixedTime);

        lblSettleDiagnostics.setVisible(!fixedTime);
        settleDiagnostics.setVisible(!fixedTime);

        lblSettleGraph.setVisible(settleDiagnostics.isSelected() && !fixedTime);
        settleGraph.setVisible(settleDiagnostics.isSelected() && !fixedTime);
    }

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter(uppFormat);
        LongConverter longConverter = new LongConverter();
        IntegerConverter intConverter = new IntegerConverter();
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());

        addWrappedBinding(camera, "name", nameTf, "text");
        addWrappedBinding(camera, "looking", lookingCb, "selectedItem");
        
        MutableLocationProxy unitsPerPixel = new MutableLocationProxy();
        bind(UpdateStrategy.READ_WRITE, camera, "unitsPerPixel", unitsPerPixel, "location");
        addWrappedBinding(unitsPerPixel, "lengthX", textFieldUppX, "text", lengthConverter);
        addWrappedBinding(unitsPerPixel, "lengthY", textFieldUppY, "text", lengthConverter);

        addWrappedBinding(camera, "settleMethod", settleMethod, "selectedItem");
        addWrappedBinding(camera, "settleTimeMs", settleTimeMs, "text", longConverter);
        addWrappedBinding(camera, "settleTimeoutMs", settleTimeoutMs, "text", longConverter);
        addWrappedBinding(camera, "settleThreshold", settleThreshold, "text", doubleConverter);
        addWrappedBinding(camera, "settleFullColor", settleFullColor, "selected");
        addWrappedBinding(camera, "settleGaussianBlur", settleGaussianBlur, "text", intConverter);
        addWrappedBinding(camera, "settleMaskCircle", settleMaskCircle, "text", doubleConverter);
        addWrappedBinding(camera, "settleDiagnostics", settleDiagnostics, "selected");
        addWrappedBinding(camera, "settleGraph", settleGraph, "graph");
        
        ComponentDecorators.decorateWithAutoSelect(textFieldUppX);
        ComponentDecorators.decorateWithAutoSelect(textFieldUppY);
        ComponentDecorators.decorateWithLengthConversion(textFieldUppX, uppFormat);
        ComponentDecorators.decorateWithLengthConversion(textFieldUppY, uppFormat);

        ComponentDecorators.decorateWithAutoSelect(nameTf);
        ComponentDecorators.decorateWithAutoSelect(textFieldWidth);
        ComponentDecorators.decorateWithAutoSelect(textFieldHeight);
        ComponentDecorators.decorateWithAutoSelect(settleTimeMs);

        ComponentDecorators.decorateWithAutoSelect(settleTimeMs);
        ComponentDecorators.decorateWithAutoSelect(settleTimeoutMs);
        ComponentDecorators.decorateWithAutoSelect(settleThreshold);
        ComponentDecorators.decorateWithAutoSelect(settleGaussianBlur);
        ComponentDecorators.decorateWithAutoSelect(settleMaskCircle);
    }

    private Action measureAction = new AbstractAction("Measure") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure.setAction(confirmMeasureAction);
            cancelMeasureAction.setEnabled(true);
            CameraView cameraView = MainFrame.get().getCameraViews().setSelectedCamera(camera);
            cameraView.setSelectionEnabled(true);
            cameraView.setSelection(0, 0, 100, 100);
        }
    };

    private Action confirmMeasureAction = new AbstractAction("Confirm") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure.setAction(measureAction);
            cancelMeasureAction.setEnabled(false);
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
            cameraView.setSelectionEnabled(false);
            Rectangle selection = cameraView.getSelection();
            double width = Double.parseDouble(textFieldWidth.getText());
            double height = Double.parseDouble(textFieldHeight.getText());
            textFieldUppX.setText(String.format(Locale.US, uppFormat, (width / Math.abs(selection.width))));
            textFieldUppY.setText(String.format(Locale.US, uppFormat, (height / Math.abs(selection.height))));
        }
    };

    private Action cancelMeasureAction = new AbstractAction("Cancel") {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            btnMeasure.setAction(measureAction);
            cancelMeasureAction.setEnabled(false);
            CameraView cameraView = MainFrame.get().getCameraViews().getCameraView(camera);
            cameraView.setSelectionEnabled(false);
        }
    };
    private JTextField textFieldWidth;
    private JTextField textFieldHeight;
    private JTextField textFieldUppX;
    private JTextField textFieldUppY;
    private JLabel lblWidth;
    private JLabel lblHeight;
    private JLabel lblX;
    private JLabel lblY;
    private JPanel panelVision;
    private JLabel lblSettleTimeMs;
    private JTextField settleTimeMs;
    private JPanel panel;
    private JLabel lblName;
    private JLabel lblLooking;
    private JComboBox lookingCb;
    private JTextField nameTf;
    private JLabel lblSettleMethod;
    private JComboBox settleMethod;
    private JLabel lblSettleTimeoutMs;
    private JTextField settleTimeoutMs;
    private JTextField settleThreshold;
    private JLabel lblSettleThreshold;
    private JTextField settleGaussianBlur;
    private JLabel lblSettleGaussianBlur;
    private JLabel lblSettleFullColor;
    private JCheckBox settleFullColor;
    private JLabel lblSettleMaskCircle;
    private JTextField settleMaskCircle;
    private JLabel lblSettleDiagnostics;
    private JCheckBox settleDiagnostics;
    private SimpleGraphView settleGraph;
    private JLabel lblSettleGraph;
}
