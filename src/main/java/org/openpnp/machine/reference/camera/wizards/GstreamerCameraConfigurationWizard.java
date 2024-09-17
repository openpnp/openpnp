/*
 * Copyright (C) 2020,2024 Ian Jamison <ian.dev@arkver.com>
 *
 * Based on a Wizard which is
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

package org.openpnp.machine.reference.camera.wizards;

import javax.swing.border.TitledBorder;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.camera.GstreamerCamera;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class GstreamerCameraConfigurationWizard extends AbstractConfigurationWizard {
    private static final long serialVersionUID = 1L;

    private final GstreamerCamera camera;

    private JPanel panelPipe;
    private JTextField gstPipeTextField;

    public GstreamerCameraConfigurationWizard(GstreamerCamera camera) {
        this.camera = camera;

        panelPipe = new JPanel();
        contentPanel.add(panelPipe);
        panelPipe.setBorder(
                new TitledBorder(null, "GStreamer Pipeline", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        panelPipe.setLayout(new FormLayout(
                new ColumnSpec[] { FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), },
                new RowSpec[] { FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC, }));

        JLabel lblPipeline = new JLabel("Pipeline launch string");
        panelPipe.add(lblPipeline, "2, 2, right, default");
        gstPipeTextField = new JTextField(40);
        panelPipe.add(gstPipeTextField, "4, 2, fill, default");
        gstPipeTextField.setColumns(5);
    }

    @Override
    public void createBindings() {
        addWrappedBinding(camera, "gstPipeline", gstPipeTextField, "text");
    }

}
