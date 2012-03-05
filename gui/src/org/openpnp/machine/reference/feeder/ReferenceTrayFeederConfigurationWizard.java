package org.openpnp.machine.reference.feeder;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.model.Location;
import org.openpnp.util.LengthUtil;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceTrayFeederConfigurationWizard extends JPanel implements Wizard {
	private final ReferenceTrayFeeder feeder;

	private WizardContainer wizardContainer;
	
	private JTextField offsetsX;
	private JTextField offsetsY;
	private JTextField offsetsZ;
	private JTextField trayCountX;
	private JTextField trayCountY;

	public ReferenceTrayFeederConfigurationWizard(ReferenceTrayFeeder referenceTrayFeeder) {
		feeder = referenceTrayFeeder;
		
		JPanel panel = new JPanel();
		
		panel.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
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
				FormFactory.DEFAULT_ROWSPEC,}));

		JLabel lblX = new JLabel("X");
		panel.add(lblX, "4, 2");

		JLabel lblY = new JLabel("Y");
		panel.add(lblY, "6, 2");

		JLabel lblZ = new JLabel("Z");
		panel.add(lblZ, "8, 2");

		JLabel lblFeedStartLocation = new JLabel("Offsets");
		panel.add(lblFeedStartLocation, "2, 4, right, default");

		offsetsX = new JTextField();
		panel.add(offsetsX, "4, 4, fill, default");
		offsetsX.setColumns(10);

		offsetsY = new JTextField();
		panel.add(offsetsY, "6, 4, fill, default");
		offsetsY.setColumns(10);

		offsetsZ = new JTextField();
		panel.add(offsetsZ, "8, 4, fill, default");
		offsetsZ.setColumns(10);

		JLabel lblTrayCount = new JLabel("Tray Count");
		panel.add(lblTrayCount, "2, 6, right, default");

		trayCountX = new JTextField();
		panel.add(trayCountX, "4, 6, fill, default");
		trayCountX.setColumns(10);

		trayCountY = new JTextField();
		panel.add(trayCountY, "6, 6, fill, default");
		trayCountY.setColumns(10);

		JButton btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				ReferenceTrayFeederConfigurationWizard.this.feeder.getOffsets().setX(
								Double.parseDouble(offsetsX.getText()));
				ReferenceTrayFeederConfigurationWizard.this.feeder.getOffsets().setY(
								Double.parseDouble(offsetsY.getText()));
				ReferenceTrayFeederConfigurationWizard.this.feeder.getOffsets().setZ(
								Double.parseDouble(offsetsZ.getText()));

				ReferenceTrayFeederConfigurationWizard.this.feeder.setTrayCountX(Integer.parseInt(trayCountX
								.getText()));
				ReferenceTrayFeederConfigurationWizard.this.feeder.setTrayCountY(Integer.parseInt(trayCountY
								.getText()));

				wizardContainer.wizardCompleted(ReferenceTrayFeederConfigurationWizard.this);
			}
		});
		setLayout(new BorderLayout(0, 0));
		
		add(panel, BorderLayout.CENTER);
		
		JPanel panel_1 = new JPanel();
		panel_1.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		panel_1.add(btnSave, "8, 26");
		
		add(panel_1, BorderLayout.SOUTH);

		offsetsX.setText(String.format("%2.3f", feeder.getOffsets().getX()));
		offsetsY.setText(String.format("%2.3f", feeder.getOffsets().getY()));
		offsetsZ.setText(String.format("%2.3f", feeder.getOffsets().getZ()));

		trayCountX.setText(String.format("%d", feeder.getTrayCountX()));
		trayCountY.setText(String.format("%d", feeder.getTrayCountY()));
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