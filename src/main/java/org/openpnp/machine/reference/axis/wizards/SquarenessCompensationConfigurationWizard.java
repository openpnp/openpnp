/*
 * Copyright (C) 2021 <mark@makr.zone>
 * inspired and based on work
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

package org.openpnp.machine.reference.axis.wizards;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.SimpleGraphView;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.axis.ReferenceLinearTransformAxis;
import org.openpnp.machine.reference.camera.ReferenceCamera;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Camera.Looking;
import org.openpnp.spi.Driver;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractControllerAxis;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class SquarenessCompensationConfigurationWizard extends AbstractConfigurationWizard {
    protected NamedConverter<Driver> driverConverter;
    private JLabel lblBacklashOffset;
    private JTextField backlashOffset;
    private JLabel lblBacklashSpeedFactor;
    private JTextField backlashSpeedFactor;

    private JPanel panelSquarenessDiagnostics;
    
    private JLabel lblStepTest;
    private SimpleGraphView stepTestGraph;
    private JLabel lblBacklashSpeedTest;
    private SimpleGraphView backlashSpeedTestGraph;
    private JLabel lblBacklashDistanceTest;
    private SimpleGraphView backlashDistanceTestGraph;
    private JLabel lblSneakupDistance;
    private JTextField sneakUpOffset;
    private JButton btnCalibrate;
    private JLabel lblAcceptableTolerance;
    private JTextField acceptableTolerance;
    private ReferenceLinearTransformAxis axis;
    protected Axis.Type type;

    public Axis.Type getType() {
        return type;
    }
    public void setType(Axis.Type type) {
        this.type = type;
    }

    private Action squarenessCalibrateAction = new AbstractAction("Calibrate now", Icons.axisCartesian) {
        {
            putValue(Action.SHORT_DESCRIPTION,
                    "<html>\r\n" + 
                            "<p>Calibrate the axis squaress using a constant non square reference angle.</p>\r\n" + 
                            "<p>Consider using this function from Issues & Solutions where you get step by step instructions,<br/>\r\n" + 
                            "for the needed preparatory steps and the calibration in proper sequence. You also get extensive Wiki links.</p>\r\n" + 
                    "</html>");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            applyAction.actionPerformed(e);
            UiUtils.submitUiMachineTask(() -> {
                AbstractAxis primaryAxis = axis.getPrimaryInputAxis();
                String primaryAxisId = primaryAxis.getId();
                
                
                HeadMountable hm = ((AbstractCoordinateAxis) axis.getPrimaryInputAxis()).getDefaultHeadMountable();
                if (hm instanceof ReferenceCamera) {
                    ReferenceCamera camera = (ReferenceCamera) hm;
                    if (camera.getHead() != null && camera.getLooking() == Looking.Down) {
                        if (Configuration.get().getMachine() instanceof ReferenceMachine) {
//                            ReferenceMachine refMachine = (ReferenceMachine) Configuration.get().getMachine();
//                            refMachine.getCalibrationSolutions()
//                            .calibrateAxisBacklash((ReferenceHead)(camera.getHead()), camera,
//                                    camera, (ReferenceControllerAxis)axis);
//                            MainFrame.get().getMachineSetupTab().selectCurrentTreePath();
                            return true;
                        }
                    }
                }
                throw new Exception("Only an axis on a down-looking camera can be calibrated.");
            });
        }
    };    

    public SquarenessCompensationConfigurationWizard(ReferenceLinearTransformAxis axis) {
        this.axis = axis;
        panelSquarenessDiagnostics = new JPanel();
        panelSquarenessDiagnostics.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Squareness Compensation and Calibration", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        contentPanel.add(panelSquarenessDiagnostics);
        
        panelSquarenessDiagnostics.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
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
                        RowSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("default:grow"),
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("default:grow"),}));

//        lblAcceptableTolerance = new JLabel("Tolerance ±");
//        lblAcceptableTolerance.setToolTipText("<html>\r\n<p>Acceptable backlash compensation tolerance (±) for calibration.</p>\r\n<p>A larger tolerance will possibly allow for a more efficient backlash compensation method.</p>\r\n</html>");
//        panelSquarenessDiagnostics.add(lblAcceptableTolerance, "6, 2, right, default");

//        acceptableTolerance = new JTextField();
//        panelBacklashDiagnostics.add(acceptableTolerance, "8, 2, fill, default");
//        acceptableTolerance.setColumns(10);

        btnCalibrate = new JButton(squarenessCalibrateAction);
        panelSquarenessDiagnostics.add(btnCalibrate, "10, 1, 1, 3, left, default");

//        lblBacklashOffset = new JLabel("Backlash Offset");
//        panelBacklashDiagnostics.add(lblBacklashOffset, "2, 4, right, default");
//
//        backlashOffset = new JTextField();
//        panelBacklashDiagnostics.add(backlashOffset, "4, 4");
//        backlashOffset.setColumns(10);
//
//        lblSneakupDistance = new JLabel("Sneak-up Distance");
//        panelBacklashDiagnostics.add(lblSneakupDistance, "6, 4, right, default");
//
//        sneakUpOffset = new JTextField();
//        panelBacklashDiagnostics.add(sneakUpOffset, "8, 4");
//        sneakUpOffset.setColumns(10);
//
//        lblBacklashSpeedFactor = new JLabel("Speed Factor");
//        panelBacklashDiagnostics.add(lblBacklashSpeedFactor, "2, 6, right, default");
//
//        backlashSpeedFactor = new JTextField();
//        panelBacklashDiagnostics.add(backlashSpeedFactor, "4, 6");
//        backlashSpeedFactor.setColumns(10);
//
//        lblStepTest = new JLabel("<html>\r\n<body style=\"text-align:right\">\r\n<p>\r\nAbsolute <span style=\"color:#FF0000\">&mdash;&mdash;</span>\r\n</p>\r\n<p>\r\nRelative <span style=\"color:#005BD9\">&mdash;&mdash;</span>\r\n</p>\r\n<p>\r\nRandom <span style=\"color:#BB7700\">&mdash;&mdash;</span>\r\n</p>\r\n<br/>\r\n<p>\r\nTolerance <span style=\"color:#000077\">&mdash;&mdash;</span>\r\n</p>\r\n<br/>\r\n<p>\r\nerror at step.\r\n</p>\r\n</body>\r\n</html>");
//        panelBacklashDiagnostics.add(lblStepTest, "2, 8, right, default");
//
//        stepTestGraph = new SimpleGraphView();
//        stepTestGraph.setPreferredSize(new Dimension(300, 100));
//        stepTestGraph.setFont(new Font("Dialog", Font.PLAIN, 11));
//        panelBacklashDiagnostics.add(stepTestGraph, "4, 8, 7, 1, fill, fill");
//
//        lblBacklashDistanceTest = new JLabel("<html>\r\n<body style=\"text-align:right\">\r\n<p>\r\nBacklash <span style=\"color:#005BD9\">&mdash;&mdash;</span>\r\n</p>\r\n<p>\r\nEnvelope <span style=\"color:#000077\">&mdash;&mdash;</span>\r\n</p>\r\n<p>\r\nOvershoot <span style=\"color:#FF0000\">&mdash;&mdash;</span>\r\n</p>\r\n<p>\r\nRandom <span style=\"color:#BB7700\">&mdash;&mdash;</span>\r\n</p>\r\n<br/>\r\n<p>\r\nMove Time <span style=\"color:#00AA00\">&mdash;&mdash;</span>\r\n</p>\r\n<br/>\r\n<br/>\r\n<p>\r\nforward/ <span style=\"color:#777777\">reverse</span><br/>\r\nat move distance.\r\n<p/>\r\n</body>\r\n</html>");
//        panelBacklashDiagnostics.add(lblBacklashDistanceTest, "2, 10, right, default");
//
//        backlashDistanceTestGraph = new SimpleGraphView();
//        backlashDistanceTestGraph.setPreferredSize(new Dimension(300, 200));
//        backlashDistanceTestGraph.setFont(new Font("Dialog", Font.PLAIN, 11));
//        panelBacklashDiagnostics.add(backlashDistanceTestGraph, "4, 10, 7, 1, fill, fill");
//
//        lblBacklashSpeedTest = new JLabel("<html>\r\n<body style=\"text-align:right\">\r\n<p>\r\nBacklash <span style=\"color:#FF0000\">&mdash;&mdash;</span>\r\n</p>\r\n<br/>\r\n<p>\r\nEffective Speed <span style=\"color:#00AA00\">&mdash;&mdash;</span>\r\n</p>\r\n<br/>\r\n<br/>\r\n<p>\r\nforward/ <span style=\"color:#777777\">reverse</span><br/>\r\nat nominal speed.\r\n</p>\r\n</body>\r\n</html>");
//        panelBacklashDiagnostics.add(lblBacklashSpeedTest, "2, 12, right, default");
//
//        backlashSpeedTestGraph = new SimpleGraphView();
//        backlashSpeedTestGraph.setPreferredSize(new Dimension(300, 100));
//        backlashSpeedTestGraph.setFont(new Font("Dialog", Font.PLAIN, 11));
//        panelBacklashDiagnostics.add(backlashSpeedTestGraph, "4, 12, 7, 1, fill, fill");
    }


    @Override
    public void createBindings() {
//        LengthConverter lengthConverter = new LengthConverter();
//        DoubleConverter doubleConverter = new DoubleConverter("%f"); 
//
//        addWrappedBinding(axis, "type", this, "type");
//
//        addWrappedBinding(axis, "acceptableTolerance", acceptableTolerance, "text", lengthConverter);
//        addWrappedBinding(axis, "backlashOffset", backlashOffset, "text", lengthConverter);
//        addWrappedBinding(axis, "sneakUpOffset", sneakUpOffset, "text", lengthConverter);
//        addWrappedBinding(axis, "backlashSpeedFactor", backlashSpeedFactor, "text", doubleConverter);
//
//        addWrappedBinding(axis, "stepTestGraph", stepTestGraph, "graph");
//        addWrappedBinding(axis, "backlashDistanceTestGraph", backlashDistanceTestGraph, "graph");
//        addWrappedBinding(axis, "backlashSpeedTestGraph", backlashSpeedTestGraph, "graph");
//
//        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(acceptableTolerance);
//        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(backlashOffset);
//        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(sneakUpOffset);

    }
}
