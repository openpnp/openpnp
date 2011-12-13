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

public class ReferenceTapeFeederConfigurationWizard extends JPanel implements
		Wizard {
	private JTextField feedStartX;
	private JTextField feedStartY;
	private JTextField feedStartZ;
	private JTextField feedEndX;
	private JTextField feedEndY;
	private JTextField feedEndZ;
	private JTextField feedRate;
	private ReferenceTapeFeeder feeder;
	
	public ReferenceTapeFeederConfigurationWizard(ReferenceTapeFeeder feeder) {
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
				ReferenceTapeFeederConfigurationWizard.this.feeder.getFeedStartLocation().setX(Double.parseDouble(feedStartX.getText()));
				ReferenceTapeFeederConfigurationWizard.this.feeder.getFeedStartLocation().setY(Double.parseDouble(feedStartY.getText()));
				ReferenceTapeFeederConfigurationWizard.this.feeder.getFeedStartLocation().setZ(Double.parseDouble(feedStartZ.getText()));
				
				ReferenceTapeFeederConfigurationWizard.this.feeder.getFeedEndLocation().setX(Double.parseDouble(feedEndX.getText()));
				ReferenceTapeFeederConfigurationWizard.this.feeder.getFeedEndLocation().setY(Double.parseDouble(feedEndY.getText()));
				ReferenceTapeFeederConfigurationWizard.this.feeder.getFeedEndLocation().setZ(Double.parseDouble(feedEndZ.getText()));
				
				ReferenceTapeFeederConfigurationWizard.this.feeder.setFeedRate(Double.parseDouble(feedRate.getText()));
				
				Configuration.get().setDirty(true);
			}
		});
		add(btnSave, "8, 26");
		
		feedStartX.setText(String.format("%2.3f", feeder.getFeedStartLocation().getX()));
		feedStartY.setText(String.format("%2.3f", feeder.getFeedStartLocation().getY()));
		feedStartZ.setText(String.format("%2.3f", feeder.getFeedStartLocation().getZ()));
		
		feedEndX.setText(String.format("%2.3f", feeder.getFeedEndLocation().getX()));
		feedEndY.setText(String.format("%2.3f", feeder.getFeedEndLocation().getY()));
		feedEndZ.setText(String.format("%2.3f", feeder.getFeedEndLocation().getZ()));
		
		feedRate.setText(String.format("%2.3f", feeder.getFeedRate()));
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
