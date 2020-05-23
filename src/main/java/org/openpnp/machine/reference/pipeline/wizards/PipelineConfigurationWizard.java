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

package org.openpnp.machine.reference.pipeline.wizards;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.spi.Pipeline;
import org.openpnp.util.UiUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;
import org.openpnp.vision.pipeline.ui.CvPipelineEditorDialog;

import javax.swing.*;
import javax.swing.border.TitledBorder;

@SuppressWarnings("serial")
public class PipelineConfigurationWizard extends AbstractConfigurationWizard {

    private Pipeline pipeline;

    public PipelineConfigurationWizard(Pipeline pipeline) {
        this.pipeline = pipeline;
        createUi();
    }

    private void createUi() {

        JPanel pipelinePanel = new JPanel();
        pipelinePanel.setBorder(new TitledBorder(null, "Pipeline", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));

        contentPanel.add(pipelinePanel);

        pipelinePanel.setLayout(
            new FormLayout(
                new ColumnSpec[] {
                    FormSpecs.RELATED_GAP_COLSPEC,
                    ColumnSpec.decode("right:default"),
                    FormSpecs.RELATED_GAP_COLSPEC,
                    ColumnSpec.decode("default:grow"),},
                new RowSpec[] {
                    FormSpecs.RELATED_GAP_ROWSPEC,
                    FormSpecs.DEFAULT_ROWSPEC
                }
            )
        );

        JLabel lblPipeline = new JLabel("Pipeline");
        pipelinePanel.add(lblPipeline, "2, 2");

        JButton editPipelineButton = new JButton("Edit");
        editPipelineButton.addActionListener(e -> UiUtils.messageBoxOnException(this::editPipeline));

        pipelinePanel.add(editPipelineButton, "4, 2");
    }

    private void editPipeline() throws Exception {
        CvPipeline pipeline = this.pipeline.getCvPipeline();
        pipeline.setProperty("camera", this.pipeline.getCamera());
        pipeline.setProperty("nozzle", MainFrame.get().getMachineControls().getSelectedNozzle());
        CvPipelineEditor editor = new CvPipelineEditor(pipeline);
        JDialog dialog = new CvPipelineEditorDialog(MainFrame.get(), this.pipeline.getName(), editor);
        dialog.setVisible(true);
    }

    @Override
    public void createBindings() {
        //addWrappedBinding(signaler, "enableErrorSound", chckbxError, "selected");
    }
}
