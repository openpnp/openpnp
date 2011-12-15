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

package org.openpnp.machine.reference.feeder;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.openpnp.Location;
import org.openpnp.Part;
import org.openpnp.gui.Wizard;
import org.openpnp.gui.WizardContainer;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.spi.Head;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

/**
 * Implemention of Feeder that indexes based on an offset. This allows a tray of
 * parts to be picked from without moving any tape. Can handle trays of
 * arbitrary X and Y count.
 * 
 * <pre>
 * {@code
 * <Configuration trayCountX="10" trayCountY="2">
 * 	<Offsets units="Millimeters" x="10" y="10" z="0" rotation="0"/>
 * </Configuration>
 * }
 * </pre>
 */
public class ReferenceTrayFeeder extends ReferenceFeeder {
	@Attribute
	private int trayCountX;
	@Attribute
	private int trayCountY;
	@Element
	private Location offsets;

	private int pickCount;

	@Override
	public boolean canFeedForHead(Part part, Head head) {
		return (pickCount < (trayCountX * trayCountY));
	}

	public Location feed(Head head_, Part part, Location pickLocation)
			throws Exception {
		ReferenceHead head = (ReferenceHead) head_;

		int partX = (pickCount / trayCountX);
		int partY = (pickCount - (partX * trayCountX));

		Location l = new Location();
		l.setX(pickLocation.getX() + (partX * offsets.getX()));
		l.setY(pickLocation.getY() + (partY * offsets.getY()));
		l.setZ(pickLocation.getZ());
		l.setRotation(pickLocation.getRotation());
		l.setUnits(pickLocation.getUnits());

		System.out.println(String.format(
				"Feeding part # %d, x %d, y %d, xPos %f, yPos %f", pickCount,
				partX, partY, l.getX(), l.getY()));

		pickCount++;

		return l;
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new ConfigurationWizard();
	}

	public int getTrayCountX() {
		return trayCountX;
	}

	public void setTrayCountX(int trayCountX) {
		this.trayCountX = trayCountX;
	}

	public int getTrayCountY() {
		return trayCountY;
	}

	public void setTrayCountY(int trayCountY) {
		this.trayCountY = trayCountY;
	}

	public Location getOffsets() {
		return offsets;
	}

	public void setOffsets(Location offsets) {
		this.offsets = offsets;
	}

	public int getPickCount() {
		return pickCount;
	}

	public void setPickCount(int pickCount) {
		this.pickCount = pickCount;
	}

	public class ConfigurationWizard extends JPanel implements Wizard {
		private WizardContainer wizardContainer;
		
		private JTextField offsetsX;
		private JTextField offsetsY;
		private JTextField offsetsZ;
		private JTextField trayCountX;
		private JTextField trayCountY;

		public ConfigurationWizard() {
			setLayout(new FormLayout(new ColumnSpec[] {
					FormFactory.RELATED_GAP_COLSPEC,
					FormFactory.DEFAULT_COLSPEC,
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"),
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"),
					FormFactory.RELATED_GAP_COLSPEC,
					ColumnSpec.decode("default:grow"), }, new RowSpec[] {
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC,
					FormFactory.RELATED_GAP_ROWSPEC,
					FormFactory.DEFAULT_ROWSPEC, }));

			JLabel lblX = new JLabel("X");
			add(lblX, "4, 2");

			JLabel lblY = new JLabel("Y");
			add(lblY, "6, 2");

			JLabel lblZ = new JLabel("Z");
			add(lblZ, "8, 2");

			JLabel lblFeedStartLocation = new JLabel("Offsets");
			add(lblFeedStartLocation, "2, 4, right, default");

			offsetsX = new JTextField();
			add(offsetsX, "4, 4, fill, default");
			offsetsX.setColumns(10);

			offsetsY = new JTextField();
			add(offsetsY, "6, 4, fill, default");
			offsetsY.setColumns(10);

			offsetsZ = new JTextField();
			add(offsetsZ, "8, 4, fill, default");
			offsetsZ.setColumns(10);

			JLabel lblTrayCount = new JLabel("Tray Count");
			add(lblTrayCount, "2, 6, right, default");

			trayCountX = new JTextField();
			add(trayCountX, "4, 6, fill, default");
			trayCountX.setColumns(10);

			trayCountY = new JTextField();
			add(trayCountY, "6, 6, fill, default");
			trayCountY.setColumns(10);

			JSeparator separator = new JSeparator();
			add(separator, "2, 8, 7, 1");

			JButton btnSave = new JButton("Save");
			btnSave.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					getOffsets().setX(
									Double.parseDouble(offsetsX.getText()));
					getOffsets().setY(
									Double.parseDouble(offsetsY.getText()));
					getOffsets().setZ(
									Double.parseDouble(offsetsZ.getText()));

					setTrayCountX(Integer.parseInt(trayCountX
									.getText()));
					setTrayCountY(Integer.parseInt(trayCountY
									.getText()));

					wizardContainer.wizardCompleted(ConfigurationWizard.this);
				}
			});
			add(btnSave, "8, 26");

			offsetsX.setText(String.format("%2.3f", getOffsets().getX()));
			offsetsY.setText(String.format("%2.3f", getOffsets().getY()));
			offsetsZ.setText(String.format("%2.3f", getOffsets().getZ()));

			trayCountX.setText(String.format("%d", getTrayCountX()));
			trayCountY.setText(String.format("%d", getTrayCountY()));
		}

		@Override
		public void setWizardContainer(WizardContainer wizardContainer) {
			this.wizardContainer = wizardContainer;
		}

		@Override
		public JPanel getWizardPanel() {
			return this;
		}

		@Override
		public String getWizardName() {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
