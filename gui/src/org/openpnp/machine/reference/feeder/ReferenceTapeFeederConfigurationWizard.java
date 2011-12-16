package org.openpnp.machine.reference.feeder;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.openpnp.Location;
import org.openpnp.gui.Wizard;
import org.openpnp.gui.WizardContainer;
import org.openpnp.util.LengthUtil;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

class ReferenceTapeFeederConfigurationWizard extends JPanel implements Wizard {
	private final ReferenceTapeFeeder feeder;

	private WizardContainer wizardContainer;

	private JTextField feedStartX;
	private JTextField feedStartY;
	private JTextField feedStartZ;
	private JTextField feedEndX;
	private JTextField feedEndY;
	private JTextField feedEndZ;
	private JTextField feedRate;

	public ReferenceTapeFeederConfigurationWizard(
			ReferenceTapeFeeder referenceTapeFeeder) {
		feeder = referenceTapeFeeder;

		setLayout(new BorderLayout());

		JPanel panel = new JPanel();

		panel.setLayout(new FormLayout(
				new ColumnSpec[] { FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC,
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("default:grow"),
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("default:grow"),
						FormFactory.RELATED_GAP_COLSPEC,
						ColumnSpec.decode("default:grow"),
						FormFactory.RELATED_GAP_COLSPEC,
						FormFactory.DEFAULT_COLSPEC, }, new RowSpec[] {
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
		panel.add(lblX, "4, 2");

		JLabel lblY = new JLabel("Y");
		panel.add(lblY, "6, 2");

		JLabel lblZ = new JLabel("Z");
		panel.add(lblZ, "8, 2");

		JLabel lblFeedStartLocation = new JLabel("Feed Start Location");
		panel.add(lblFeedStartLocation, "2, 4, right, default");

		feedStartX = new JTextField();
		panel.add(feedStartX, "4, 4, fill, default");
		feedStartX.setColumns(10);

		feedStartY = new JTextField();
		panel.add(feedStartY, "6, 4, fill, default");
		feedStartY.setColumns(10);

		feedStartZ = new JTextField();
		panel.add(feedStartZ, "8, 4, fill, default");
		feedStartZ.setColumns(10);

		JButton feedStartAutoFill = new JButton("Set");
		panel.add(feedStartAutoFill, "10, 4");

		JLabel lblFeedEndLocation = new JLabel("Feed End Location");
		panel.add(lblFeedEndLocation, "2, 6, right, default");

		feedEndX = new JTextField();
		panel.add(feedEndX, "4, 6, fill, default");
		feedEndX.setColumns(10);

		feedEndY = new JTextField();
		panel.add(feedEndY, "6, 6, fill, default");
		feedEndY.setColumns(10);

		feedEndZ = new JTextField();
		panel.add(feedEndZ, "8, 6, fill, default");
		feedEndZ.setColumns(10);

		JButton feedEndAutoFill = new JButton("Set");
		panel.add(feedEndAutoFill, "10, 6");

		JSeparator separator = new JSeparator();
		panel.add(separator, "2, 8, 7, 1");

		JLabel lblFeedRate = new JLabel("Feed Rate");
		panel.add(lblFeedRate, "2, 10, right, default");

		feedRate = new JTextField();
		panel.add(feedRate, "4, 10, fill, default");
		feedRate.setColumns(10);

		add(panel, BorderLayout.CENTER);

		feedStartX.setText(String.format("%2.3f", feeder.getFeedStartLocation()
				.getX()));
		feedStartY.setText(String.format("%2.3f", feeder.getFeedStartLocation()
				.getY()));
		feedStartZ.setText(String.format("%2.3f", feeder.getFeedStartLocation()
				.getZ()));

		feedEndX.setText(String.format("%2.3f", feeder.getFeedEndLocation()
				.getX()));
		feedEndY.setText(String.format("%2.3f", feeder.getFeedEndLocation()
				.getY()));
		feedEndZ.setText(String.format("%2.3f", feeder.getFeedEndLocation()
				.getZ()));

		feedRate.setText(String.format("%2.3f", feeder.getFeedRate()));

		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new FlowLayout(FlowLayout.RIGHT));
		add(panel_1, BorderLayout.SOUTH);

		JButton btnSave = new JButton("Save");
		panel_1.add(btnSave);
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ReferenceTapeFeederConfigurationWizard.this.feeder
						.getFeedStartLocation().setX(
								Double.parseDouble(feedStartX.getText()));
				ReferenceTapeFeederConfigurationWizard.this.feeder
						.getFeedStartLocation().setY(
								Double.parseDouble(feedStartY.getText()));
				ReferenceTapeFeederConfigurationWizard.this.feeder
						.getFeedStartLocation().setZ(
								Double.parseDouble(feedStartZ.getText()));

				ReferenceTapeFeederConfigurationWizard.this.feeder
						.getFeedEndLocation().setX(
								Double.parseDouble(feedEndX.getText()));
				ReferenceTapeFeederConfigurationWizard.this.feeder
						.getFeedEndLocation().setY(
								Double.parseDouble(feedEndY.getText()));
				ReferenceTapeFeederConfigurationWizard.this.feeder
						.getFeedEndLocation().setZ(
								Double.parseDouble(feedEndZ.getText()));

				ReferenceTapeFeederConfigurationWizard.this.feeder
						.setFeedRate(Double.parseDouble(feedRate.getText()));

				wizardContainer
						.wizardCompleted(ReferenceTapeFeederConfigurationWizard.this);
			}
		});
		
		feedStartAutoFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Location l = wizardContainer.getMachineControlsPanel().getDisplayedLocation();
				l = LengthUtil.convertLocation(l, feeder.getFeedStartLocation().getUnits());
				feedStartX.setText(String.format("%2.3f", l.getX()));
				feedStartY.setText(String.format("%2.3f", l.getY()));
				feedStartZ.setText(String.format("%2.3f", l.getZ()));
			}
		});
		
		feedEndAutoFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Location l = wizardContainer.getMachineControlsPanel().getDisplayedLocation();
				l = LengthUtil.convertLocation(l, feeder.getFeedEndLocation().getUnits());
				feedEndX.setText(String.format("%2.3f", l.getX()));
				feedEndY.setText(String.format("%2.3f", l.getY()));
				feedEndZ.setText(String.format("%2.3f", l.getZ()));
			}
		});
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
		return null;
	}
}