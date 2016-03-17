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

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.ReferenceJobProcessor;
import org.openpnp.model.Configuration;
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
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
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
    }

    @Override
    public void createBindings() {
        addWrappedBinding(jobProcessor, "demoMode", chckbxDemoMode, "selected");
    }
}
