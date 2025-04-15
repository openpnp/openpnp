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


package org.openpnp.gui.wizards;

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

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.Converter;
import org.openpnp.Translations;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.VisionCompositingPreview;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.VisionCompositing;
import org.openpnp.model.VisionCompositing.Composite;
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
public class PackageCompositingWizard extends AbstractConfigurationWizard {
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

    public PackageCompositingWizard(org.openpnp.model.Package pkg) {
        this.pkg = pkg;
        this.visionCompositing = pkg.getVisionCompositing();
        createUi();
        UiUtils.messageBoxOnExceptionLater(() -> computeCompositingAction.actionPerformed(null));
    }

    private void createUi() {
        compositingPanel = new JPanel();
        contentPanel.add(compositingPanel);
        compositingPanel.setBorder(new TitledBorder(null, Translations.getString("PackageCompositingWizard.Border.title"), TitledBorder.LEADING, TitledBorder.TOP, null, null)); //$NON-NLS-1$
        add(compositingPanel, BorderLayout.NORTH);
        compositingPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"), //$NON-NLS-1$
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),}, //$NON-NLS-1$
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,}));

        lblMethod = new JLabel(Translations.getString("PackageCompositingWizard.MethodLabel.text")); //$NON-NLS-1$
        lblMethod.setToolTipText(Translations.getString("PackageCompositingWizard.MethodLabel.toolTipText")); //$NON-NLS-1$
        compositingPanel.add(lblMethod, "2, 2, right, default"); //$NON-NLS-1$

        compositingMethod = new JComboBox(VisionCompositing.CompositingMethod.values());
        compositingPanel.add(compositingMethod, "4, 2, fill, default"); //$NON-NLS-1$

        lblExtraShots = new JLabel(Translations.getString("PackageCompositingWizard.ExtraShotsLabel.text")); //$NON-NLS-1$
        lblExtraShots.setToolTipText(Translations.getString("PackageCompositingWizard.ExtraShotsLabel.toolTipText")); //$NON-NLS-1$
        compositingPanel.add(lblExtraShots, "2, 4, right, default"); //$NON-NLS-1$

        extraShots = new JTextField();
        compositingPanel.add(extraShots, "4, 4, fill, default"); //$NON-NLS-1$
        extraShots.setColumns(10);

        lblMaxPickTolerance = new JLabel(Translations.getString("PackageCompositingWizard.MaxPickToleranceLabel.text")); //$NON-NLS-1$
        lblMaxPickTolerance.setToolTipText(Translations.getString("PackageCompositingWizard.MaxPickToleranceLabel.toolTipText")); //$NON-NLS-1$
        compositingPanel.add(lblMaxPickTolerance, "6, 4, right, default"); //$NON-NLS-1$

        maxPickTolerance = new JTextField();
        compositingPanel.add(maxPickTolerance, "8, 4, fill, default"); //$NON-NLS-1$
        maxPickTolerance.setColumns(10);

        lblMinAngleLeverage = new JLabel(Translations.getString("PackageCompositingWizard.MinAngleLeverageLabel.text")); //$NON-NLS-1$
        lblMinAngleLeverage.setToolTipText(Translations.getString("PackageCompositingWizard.MinAngleLeverageLabel.toolTipText")); //$NON-NLS-1$
        compositingPanel.add(lblMinAngleLeverage, "2, 6, right, default"); //$NON-NLS-1$

        minLeverageFactor = new JTextField();
        compositingPanel.add(minLeverageFactor, "4, 6, fill, default"); //$NON-NLS-1$
        minLeverageFactor.setColumns(10);

        lblAllowInsideCorner = new JLabel(Translations.getString("PackageCompositingWizard.AllowInsideCornerLabel.text")); //$NON-NLS-1$
        lblAllowInsideCorner.setToolTipText(Translations.getString("PackageCompositingWizard.AllowInsideCornerLabel.toolTipText")); //$NON-NLS-1$
        compositingPanel.add(lblAllowInsideCorner, "6, 6, right, default"); //$NON-NLS-1$

        allowInside = new JCheckBox(""); //$NON-NLS-1$
        compositingPanel.add(allowInside, "8, 6"); //$NON-NLS-1$

        btnTest = new JButton(computeCompositingAction);
        btnTest.setToolTipText(Translations.getString("PackageCompositingWizard.TestButton.toolTipText")); //$NON-NLS-1$
        compositingPanel.add(btnTest, "2, 8"); //$NON-NLS-1$

        compositeSolution = new JTextField();
        compositeSolution.setEditable(false);
        compositingPanel.add(compositeSolution, "4, 8, 7, 1, fill, default"); //$NON-NLS-1$
        compositeSolution.setColumns(10);

        visionPreview = new VisionCompositingPreview();
        visionPreview.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
        add(visionPreview, BorderLayout.CENTER);

        pkg.addPropertyChangeListener("footprint", (e) -> computeCompositingAction.actionPerformed(null)); //$NON-NLS-1$
    }

    public final Action computeCompositingAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.refresh);
            putValue(NAME, Translations.getString("PackageCompositingWizard.ComputeCompositing.Name")); //$NON-NLS-1$
            putValue(SHORT_DESCRIPTION, Translations.getString("PackageCompositingWizard.ComputeCompositing.ShortDescription")); //$NON-NLS-1$
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
        firePropertyChange("status", oldValue, status); //$NON-NLS-1$
    }

    public VisionCompositing.Composite getComposite() {
        return composite;
    }

    public void setComposite(VisionCompositing.Composite composite) {
        Object oldValue = this.composite;
        this.composite = composite;
        firePropertyChange("composite", oldValue, composite); //$NON-NLS-1$
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
                throw new Exception(Translations.getString("PackageCompositingWizard.NoCompatibleNozzleTipError.text")+pkg.getId()+"."); //$NON-NLS-1$ //$NON-NLS-2$
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
                setStatus("Error: "+composite.getCompositingSolution()+" | "+composite.getDiagnostics()); //$NON-NLS-1$ //$NON-NLS-2$
            }
            else {
                setStatus("Solution: "+composite.getCompositingSolution()+" | Min. shots: "+minShots+" | Max. shots: "+maxShots //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        +" | Computation: "+String.format("%.2f", composite.getComputeTime())+"ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
            return composite;
        }
        catch (Exception e) {
            setStatus(Translations.getString("PackageCompositingWizard.Error.text")+e.getMessage()); //$NON-NLS-1$
            return null;
        }
    }

    @Override
    public void createBindings() {
        Converter lengthConverter = new LengthConverter();
        Converter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        Converter intConverter = new IntegerConverter();
        
        bind(UpdateStrategy.READ_WRITE, visionCompositing, "compositingMethod", compositingMethod, "selectedItem");
        bind(UpdateStrategy.READ_WRITE, visionCompositing, "maxPickTolerance", maxPickTolerance, "text", lengthConverter);
        bind(UpdateStrategy.READ_WRITE, visionCompositing, "minLeverageFactor", minLeverageFactor, "text", doubleConverter);
        bind(UpdateStrategy.READ_WRITE, visionCompositing, "allowInside", allowInside, "selected");
        bind(UpdateStrategy.READ_WRITE, visionCompositing, "extraShots", extraShots, "text", intConverter);
        bind(UpdateStrategy.READ, this, "status", compositeSolution, "text");
        bind(UpdateStrategy.READ, this, "composite", visionPreview, "composite");

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(maxPickTolerance);
        ComponentDecorators.decorateWithAutoSelect(minLeverageFactor);
        ComponentDecorators.decorateWithAutoSelect(extraShots);
        
    }
}
