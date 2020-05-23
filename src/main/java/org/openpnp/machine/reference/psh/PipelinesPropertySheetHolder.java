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

package org.openpnp.machine.reference.psh;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Pipeline;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.SimplePropertySheetHolder;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class PipelinesPropertySheetHolder extends SimplePropertySheetHolder {
    final Machine machine;

    public PipelinesPropertySheetHolder(Machine machine, String title, List<? extends PropertySheetHolder> children,
                                        Icon icon) {
        super(title, children, icon);
        this.machine = machine;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] { newAction };
    }

    public Action newAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Pipeline...");
            putValue(SHORT_DESCRIPTION, "Create a new pipeline.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration configuration = Configuration.get();
            ClassSelectionDialog<Pipeline> dialog = new ClassSelectionDialog<>(MainFrame.get(),
                    "Select Pipeline...", "Please select a Pipeline implemention from the list below.",
                    configuration.getMachine().getCompatiblePipelineClasses());
            dialog.setVisible(true);
            Class<? extends Pipeline> cls = dialog.getSelectedClass();
            if (cls == null) {
                return;
            }
            try {
                Pipeline pipeline = cls.newInstance();

                machine.addPipeline(pipeline);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Error", e);
            }
        }
    };
}
