/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.model.Configuration;

public class MachinePanel extends JPanel implements WizardContainer {
    public MachinePanel() {
        setLayout(new BorderLayout(0, 0));
        addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				removeAll();
                Wizard wizard = Configuration.get().getMachine()
                        .getConfigurationWizard();
                if (wizard != null) {
                    wizard.setWizardContainer(MachinePanel.this);
                    JPanel panel = wizard.getWizardPanel();
                    add(panel);
                }
                revalidate();
                repaint();
			}
		});
    }

    @Override
    public void wizardCompleted(Wizard wizard) {
        Configuration.get().setDirty(true);
    }

    @Override
    public void wizardCancelled(Wizard wizard) {
    }
}
