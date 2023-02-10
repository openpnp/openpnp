/*
 * Copyright (C) 2022 <mark@makr.zone>
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


package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.Converter;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.VisionCompositingPreview;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.VisionCompositing;
import org.openpnp.model.VisionCompositing.Composite;
import org.openpnp.model.VisionCompositing.CompositingMethod;
import org.openpnp.model.VisionCompositing.Shot;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.NozzleTip;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class PackageCompositingPanel extends JPanel {
    private final org.openpnp.model.Package pkg;
    private JPanel compositingPanel;
    private JLabel lblMethod;
    private JLabel lblMaxPickTolerance;
    private JTextField maxPickTolerance;
    private JComboBox compositingMethod;
    private JLabel lblAllowInsideCorner;
    private JCheckBox allowInside;
    private JLabel lblMinAngleLeverage;
    private JTextField minLeverageFactor;
    private JButton btnTest;

    private VisionCompositing visionCompositing;
    private VisionCompositingPreview visionPreview;
    private JLabel lblExtraShots;
    private JTextField extraShots;
    private JTextField compositeSolution;

    public PackageCompositingPanel(org.openpnp.model.Package pkg) {
        this.pkg = pkg;
        this.visionCompositing = pkg.getVisionCompositing();
        createUi();
        initDataBindings();

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxPickTolerance);
        ComponentDecorators.decorateWithAutoSelect(minLeverageFactor);
        ComponentDecorators.decorateWithAutoSelect(extraShots);
        
        UiUtils.messageBoxOnExceptionLater(() -> computeCompositingAction.actionPerformed(null));
    }

    private void createUi() {
        setLayout(new BorderLayout(0, 0));

        compositingPanel = new JPanel();
        compositingPanel.setBorder(new TitledBorder(null, "Compositing", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        add(compositingPanel, BorderLayout.NORTH);
        compositingPanel.setLayout(new FormLayout(new ColumnSpec[] {
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
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblMethod = new JLabel("Method");
        lblMethod.setToolTipText("<html>\n<p>Compositing method:</p>\n<ul>\n<li><strong>None:</strong> no compositing is performed.</li>\n<li><strong>Restricted:</strong> compositing is only performed, when the <br/>\nfootprint size is too large, or if it is asymmetric.</li>\n<li><strong>Automatic:</strong> compositing is always performed. It can still <br/>\nresult in a one-shot solution, if the corners are all convex <br/>\nand symmetric.</li>\n<li><strong>SingleCorner:</strong> compositing is always performed. Each <br/>\ncorner is individually shot. Reduces parallax errors, as <br/>\neach corner is centered in the camera.</li>\n</ul>\n</html>\n\n");
        compositingPanel.add(lblMethod, "2, 2, right, default");

        compositingMethod = new JComboBox(VisionCompositing.CompositingMethod.values());
        compositingPanel.add(compositingMethod, "4, 2, fill, default");

        lblExtraShots = new JLabel("Extra Shots");
        lblExtraShots.setToolTipText("<html>\n<p>Vision Compositing determines the optimal minimum set of required shot to determine<br/>\nthe part position and angle. On top of that, it suggests extra shots, that can be used to <br/>\nimprove positional and angular accuracy. </p>\n<br/>\n<p>Example: a square part can be determined in two diagonal corner shots. The opposite two<br/>\ncorners of the square are proposed as extra shots.</p>\n<br/>\n<p><strong>Extra Shots</strong> indicates how man of these extra shots should be used. \n</html>");
        compositingPanel.add(lblExtraShots, "2, 4, right, default");

        extraShots = new JTextField();
        compositingPanel.add(extraShots, "4, 4, fill, default");
        extraShots.setColumns(10);

        lblMaxPickTolerance = new JLabel("Max. Pick Tolerance");
        lblMaxPickTolerance.setToolTipText("<html>\n<p>If zero, the <strong>Max. Pick Tolerance</strong> on the nozzle tip is taken.</p>\n<br/>\n<p>If non-zero, this <strong>Max. Pick Tolerance</strong> overrides it for this package.</p>\n<br/>\n<p>In both cases, the <strong>Max. Pick Tolerance</strong> must cover linear <br/>\noffsets of the picked part on the nozzle, and corner offsets <br/>\ncaused by rotational offsets, as well as combinations of the two.</p>\n</html>");
        compositingPanel.add(lblMaxPickTolerance, "6, 4, right, default");

        maxPickTolerance = new JTextField();
        compositingPanel.add(maxPickTolerance, "8, 4, fill, default");
        maxPickTolerance.setColumns(10);

        lblMinAngleLeverage = new JLabel("Min. Angle Leverage");
        lblMinAngleLeverage.setToolTipText("<html>\n<p>Minimum leverage required to derive angular information from two corners.</p>\n<br/>\n<p><strong>Min. Angle Leverage</strong> is given <em>relative</em> to the footprint width and height<br/>\n(the lesser of the two).</p>\n<br/>\n<p>Example: setting 0.5 requires two corners to span at least half the part dimension<br/>\nto provide angular information.</p>\n</html>");
        compositingPanel.add(lblMinAngleLeverage, "2, 6, right, default");

        minLeverageFactor = new JTextField();
        compositingPanel.add(minLeverageFactor, "4, 6, fill, default");
        minLeverageFactor.setColumns(10);

        lblAllowInsideCorner = new JLabel("Allow inside corner?");
        lblAllowInsideCorner.setToolTipText("<html>\n<p>Allows the inclusion of corners facing inside, towards the center of the part. <br/>\nThis can be used to align parts that are still too large, i.e., where the <strong>Roaming Radius</strong><br/>\nof the camera is too small. It typically requires large, well-spaced pads.</p>\n<br/>\n<p>Example: SMD power rectifiers.</p>\n</html>");
        compositingPanel.add(lblAllowInsideCorner, "6, 6, right, default");

        allowInside = new JCheckBox("");
        compositingPanel.add(allowInside, "8, 6");

        btnTest = new JButton(computeCompositingAction);
        btnTest.setToolTipText("Compute the vision compositing, and preview the result.");
        compositingPanel.add(btnTest, "2, 8");

        compositeSolution = new JTextField();
        compositeSolution.setEditable(false);
        compositingPanel.add(compositeSolution, "4, 8, 7, 1, fill, default");
        compositeSolution.setColumns(10);

        visionPreview = new VisionCompositingPreview();
        visionPreview.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        add(visionPreview, BorderLayout.CENTER);

        pkg.addPropertyChangeListener("footprint", (e) -> computeCompositingAction.actionPerformed(null));
    }

    public final Action computeCompositingAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.refresh);
            putValue(NAME, "Compute");
            putValue(SHORT_DESCRIPTION, "Recompute the vision compositing.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            computePreviewComposite();
        }
    };

    private VisionCompositing.Composite composite;
    private String status;


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        Object oldValue = this.status;
        this.status = status;
        firePropertyChange("status", oldValue, status);
    }

    public VisionCompositing.Composite getComposite() {
        return composite;
    }

    public void setComposite(VisionCompositing.Composite composite) {
        Object oldValue = this.composite;
        this.composite = composite;
        firePropertyChange("composite", oldValue, composite);
    }

    private VisionCompositing.Composite computePreviewComposite() {
        try {
            Machine machine = Configuration.get().getMachine();
            Camera camera = VisionUtils.getBottomVisionCamera();
            Nozzle nozzle = null;
            NozzleTip nozzleTip = null;
            for (Head head : machine.getHeads()) {
                for (Nozzle n :  head.getNozzles()) {
                    for (NozzleTip nt : n.getCompatibleNozzleTips()) {
                        if (pkg.getCompatibleNozzleTips().contains(nt)) {
                            // We got us a winning pair.
                            nozzle = n;
                            nozzleTip = nt;
                            break;
                        }
                    }
                    if (nozzleTip != null) {
                        break;
                    }
                }
                if (nozzleTip != null) {
                    break;
                }
            }
            BottomVisionSettings bottomVisionSettings = ReferenceBottomVision.getDefault()
                    .getInheritedVisionSettings(pkg);
            if (nozzleTip == null) {
                throw new Exception("No compatible nozzle tip found for "+pkg.getId()+".");
            }
            Composite composite = visionCompositing.new Composite(pkg, bottomVisionSettings, nozzle, nozzleTip, camera, Location.origin);
            int minShots = 0;
            int maxShots = 0;
            for (Shot shot : composite.getCompositeShots()) {
                maxShots++;
                if (!shot.isOptional()) {
                    minShots++;
                }
            }
            setComposite(composite);
            if (composite.getCompositingSolution().isInvalid()) {
                setStatus("Error: "+composite.getCompositingSolution()+" | "+composite.getDiagnostics());
            }
            else {
                setStatus("Solution: "+composite.getCompositingSolution()+" | Min. shots: "+minShots+" | Max. shots: "+maxShots
                        +" | Computation: "+String.format("%.2f", composite.getComputeTime())+"ms");
            }
            return composite;
        }
        catch (Exception e) {
            setStatus("Error: "+e.getMessage());
            return null;
        }
    }

    protected void initDataBindings() {
        BeanProperty<VisionCompositing, CompositingMethod> visionCompositingBeanProperty = BeanProperty.create("compositingMethod");
        BeanProperty<JComboBox, Object> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
        AutoBinding<VisionCompositing, CompositingMethod, JComboBox, Object> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, visionCompositing, visionCompositingBeanProperty, compositingMethod, jComboBoxBeanProperty);
        autoBinding.bind();
        //
        BeanProperty<VisionCompositing, Length> visionCompositingBeanProperty_1 = BeanProperty.create("maxPickTolerance");
        BeanProperty<JTextField, String> jTextFieldBeanProperty = BeanProperty.create("text");
        AutoBinding<VisionCompositing, Length, JTextField, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, visionCompositing, visionCompositingBeanProperty_1, maxPickTolerance, jTextFieldBeanProperty);
        Converter lengthConverter = new LengthConverter();
        autoBinding_1.setConverter(lengthConverter);
        autoBinding_1.bind();
        //
        BeanProperty<VisionCompositing, Double> visionCompositingBeanProperty_3 = BeanProperty.create("minLeverageFactor");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_2 = BeanProperty.create("text");
        AutoBinding<VisionCompositing, Double, JTextField, String> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, visionCompositing, visionCompositingBeanProperty_3, minLeverageFactor, jTextFieldBeanProperty_2);
        Converter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        autoBinding_3.setConverter(doubleConverter);
        autoBinding_3.bind();
        //
        BeanProperty<VisionCompositing, Boolean> visionCompositingBeanProperty_4 = BeanProperty.create("allowInside");
        BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
        AutoBinding<VisionCompositing, Boolean, JCheckBox, Boolean> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, visionCompositing, visionCompositingBeanProperty_4, allowInside, jCheckBoxBeanProperty);
        autoBinding_4.bind();
        //
        BeanProperty<VisionCompositing, Integer> extraShotsBeanProperty_5 = BeanProperty.create("extraShots");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_3 = BeanProperty.create("text");
        AutoBinding<VisionCompositing, Integer, JTextField, String> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, visionCompositing, extraShotsBeanProperty_5, extraShots, jTextFieldBeanProperty_3);
        autoBinding_5.bind();

        //
        BeanProperty<PackageCompositingPanel, String> statusBeanProperty_2 = BeanProperty.create("status");
        BeanProperty<JTextField, String> jTextFieldBeanProperty_1 = BeanProperty.create("text");
        AutoBinding<PackageCompositingPanel, String, JTextField, String> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ, this, statusBeanProperty_2, compositeSolution, jTextFieldBeanProperty_1);
        autoBinding_2.bind();

        BeanProperty<PackageCompositingPanel,  VisionCompositing.Composite> cameraBeanProperty = BeanProperty.create("composite");
        BeanProperty<VisionCompositingPreview,  VisionCompositing.Composite> cameraProperty = BeanProperty.create("composite");
        AutoBinding<PackageCompositingPanel,  VisionCompositing.Composite, VisionCompositingPreview,  VisionCompositing.Composite> autoBinding_6 = 
                Bindings.createAutoBinding(UpdateStrategy.READ, this, cameraBeanProperty, visionPreview, cameraProperty);
        autoBinding_6.bind();
    }
}
