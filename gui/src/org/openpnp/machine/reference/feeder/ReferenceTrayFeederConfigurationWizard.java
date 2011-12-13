package org.openpnp.machine.reference.feeder;

import javax.swing.JPanel;

import org.openpnp.Configuration;
import org.openpnp.gui.Wizard;
import org.openpnp.gui.WizardContainer;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import java.awt.GridLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import com.jgoodies.forms.factories.FormFactory;
import java.awt.FlowLayout;
import javax.swing.JSeparator;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class ReferenceTrayFeederConfigurationWizard extends JPanel implements
		Wizard {
	private JTextField offsetsX;
	private JTextField offsetsY;
	private JTextField offsetsZ;
	private ReferenceTrayFeeder feeder;
	private JTextField trayCountX;
	private JTextField trayCountY;
	
	public ReferenceTrayFeederConfigurationWizard(ReferenceTrayFeeder feeder) {
		this.feeder = feeder;
		
		setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),},
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
				ReferenceTrayFeederConfigurationWizard.this.feeder.getOffsets().setX(Double.parseDouble(offsetsX.getText()));
				ReferenceTrayFeederConfigurationWizard.this.feeder.getOffsets().setY(Double.parseDouble(offsetsY.getText()));
				ReferenceTrayFeederConfigurationWizard.this.feeder.getOffsets().setZ(Double.parseDouble(offsetsZ.getText()));
				
				ReferenceTrayFeederConfigurationWizard.this.feeder.setTrayCountX(Integer.parseInt(trayCountX.getText()));
				ReferenceTrayFeederConfigurationWizard.this.feeder.setTrayCountY(Integer.parseInt(trayCountY.getText()));

				Configuration.get().setDirty(true);
			}
		});
		add(btnSave, "8, 26");
		
		offsetsX.setText(String.format("%2.3f", feeder.getOffsets().getX()));
		offsetsY.setText(String.format("%2.3f", feeder.getOffsets().getY()));
		offsetsZ.setText(String.format("%2.3f", feeder.getOffsets().getZ()));
		
		trayCountX.setText(String.format("%d", feeder.getTrayCountX()));
		trayCountY.setText(String.format("%d", feeder.getTrayCountY()));
	}

	@Override
	public void setWizardContainer(WizardContainer wizardContainer) {
		// TODO Auto-generated method stub

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
