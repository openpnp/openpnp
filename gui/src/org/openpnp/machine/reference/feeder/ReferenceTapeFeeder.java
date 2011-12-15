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
 * Implemention of Feeder that allows the head to index the current part and
 * then pick from a pre-specified position. It is intended that the Head is
 * carrying a pin of some type that can be extended past end of the tool to
 * index the tape. The steps this Feeder takes to feed a part are as follows:
 * Move head to Safe Z Move head to FeedStartLocation x, y Actuate ACTUATOR_PIN
 * Lower head to FeedStartLocation z Move head to FeedEndLocation x, y, z Move
 * head to Safe Z Retract ACTUATOR_PIN
 * 
 * <pre>
 * {@code
 * <!--
 * 	feedRate: Feed rate in machine units per minute for movement during the
 * 		drag operation.
 * -->
 * <Configuration feedRate="10">
 * 	<FeedStartLocation units="Millimeters" x="100" y="150" z="50" />
 * 	<FeedEndLocation units="Millimeters" x="102" y="150" z="50" />
 * </Configuration>
 * }
 * </pre>
 */
public class ReferenceTapeFeeder extends ReferenceFeeder {
	@Element
	private Location feedStartLocation;
	@Element
	private Location feedEndLocation;
	@Attribute
	private double feedRate;

	@Override
	public boolean canFeedForHead(Part part, Head head) {
		return true;
	}

	public Location feed(Head head_, Part part, Location pickLocation)
			throws Exception {

		ReferenceHead head = (ReferenceHead) head_;

		// move to safe Z
		head.moveTo(head.getX(), head.getY(), 0, head.getC());

		// move the head so that the pin is positioned above the feed hole
		head.moveTo(feedStartLocation.getX(), feedStartLocation.getY(),
				head.getZ(), head.getC());

		// extend the pin
		head.actuate(ReferenceHead.PIN_ACTUATOR_NAME, true);

		// insert the pin
		head.moveTo(head.getX(), head.getY(), feedStartLocation.getZ(),
				head.getC());

		// drag the tape
		head.moveTo(feedEndLocation.getX(), feedEndLocation.getY(),
				feedEndLocation.getZ(), head.getC(), feedRate);

		// move to safe Z
		head.moveTo(head.getX(), head.getY(), 0, head.getC());

		// retract the pin
		head.actuate(ReferenceHead.PIN_ACTUATOR_NAME, false);

		return pickLocation;
	}

	@Override
	public String toString() {
		return String.format("ReferenceTapeFeeder id %s", id);
	}

	@Override
	public Wizard getConfigurationWizard() {
		return new ConfigurationWizard();
	}

	public Location getFeedStartLocation() {
		return feedStartLocation;
	}

	public void setFeedStartLocation(Location feedStartLocation) {
		this.feedStartLocation = feedStartLocation;
	}

	public Location getFeedEndLocation() {
		return feedEndLocation;
	}

	public void setFeedEndLocation(Location feedEndLocation) {
		this.feedEndLocation = feedEndLocation;
	}

	public double getFeedRate() {
		return feedRate;
	}

	public void setFeedRate(double feedRate) {
		this.feedRate = feedRate;
	}

	class ConfigurationWizard extends JPanel implements Wizard {
		private WizardContainer wizardContainer;

		private JTextField feedStartX;
		private JTextField feedStartY;
		private JTextField feedStartZ;
		private JTextField feedEndX;
		private JTextField feedEndY;
		private JTextField feedEndZ;
		private JTextField feedRate;

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

			JLabel lblFeedStartLocation = new JLabel("Feed Start Location");
			add(lblFeedStartLocation, "2, 4, right, default");

			feedStartX = new JTextField();
			add(feedStartX, "4, 4, fill, default");
			feedStartX.setColumns(10);

			feedStartY = new JTextField();
			add(feedStartY, "6, 4, fill, default");
			feedStartY.setColumns(10);

			feedStartZ = new JTextField();
			add(feedStartZ, "8, 4, fill, default");
			feedStartZ.setColumns(10);

			JLabel lblFeedEndLocation = new JLabel("Feed End Location");
			add(lblFeedEndLocation, "2, 6, right, default");

			feedEndX = new JTextField();
			add(feedEndX, "4, 6, fill, default");
			feedEndX.setColumns(10);

			feedEndY = new JTextField();
			add(feedEndY, "6, 6, fill, default");
			feedEndY.setColumns(10);

			feedEndZ = new JTextField();
			add(feedEndZ, "8, 6, fill, default");
			feedEndZ.setColumns(10);

			JSeparator separator = new JSeparator();
			add(separator, "2, 8, 7, 1");

			JLabel lblFeedRate = new JLabel("Feed Rate");
			add(lblFeedRate, "2, 10, right, default");

			feedRate = new JTextField();
			add(feedRate, "4, 10, fill, default");
			feedRate.setColumns(10);

			JButton btnSave = new JButton("Save");
			btnSave.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					getFeedStartLocation().setX(Double.parseDouble(feedStartX.getText()));
					getFeedStartLocation().setY(Double.parseDouble(feedStartY.getText()));
					getFeedStartLocation().setZ(Double.parseDouble(feedStartZ.getText()));

					getFeedEndLocation().setX(Double.parseDouble(feedEndX.getText()));
					getFeedEndLocation().setY(Double.parseDouble(feedEndY.getText()));
					getFeedEndLocation().setZ(Double.parseDouble(feedEndZ.getText()));

					setFeedRate(Double.parseDouble(feedRate.getText()));
					
					wizardContainer.wizardCompleted(ConfigurationWizard.this);
				}
			});
			add(btnSave, "8, 26");

			feedStartX.setText(String.format("%2.3f", getFeedStartLocation()
					.getX()));
			feedStartY.setText(String.format("%2.3f", getFeedStartLocation()
					.getY()));
			feedStartZ.setText(String.format("%2.3f", getFeedStartLocation()
					.getZ()));

			feedEndX.setText(String
					.format("%2.3f", getFeedEndLocation().getX()));
			feedEndY.setText(String
					.format("%2.3f", getFeedEndLocation().getY()));
			feedEndZ.setText(String
					.format("%2.3f", getFeedEndLocation().getZ()));

			feedRate.setText(String.format("%2.3f", getFeedRate()));
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
