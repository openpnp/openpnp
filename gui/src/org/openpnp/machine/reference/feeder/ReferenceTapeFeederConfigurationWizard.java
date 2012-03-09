package org.openpnp.machine.reference.feeder;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.Binding.SyncFailure;
import org.jdesktop.beansbinding.BindingListener;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.JBindings.WrappedBinding;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.model.Location;

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
	private JButton feedStartAutoFill;
	private JButton feedEndAutoFill;
	private JButton btnSave;
	
	private List<WrappedBinding> wrappedBindings = new ArrayList<WrappedBinding>();

	public ReferenceTapeFeederConfigurationWizard(
			ReferenceTapeFeeder referenceTapeFeeder) {
		feeder = referenceTapeFeeder;

		setLayout(new BorderLayout());

		JPanel panelFields = new JPanel();

		panelFields.setLayout(new FormLayout(
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
		panelFields.add(lblX, "4, 2");

		JLabel lblY = new JLabel("Y");
		panelFields.add(lblY, "6, 2");

		JLabel lblZ = new JLabel("Z");
		panelFields.add(lblZ, "8, 2");

		JLabel lblFeedStartLocation = new JLabel("Feed Start Location");
		panelFields.add(lblFeedStartLocation, "2, 4, right, default");

		feedStartX = new JTextField();
		panelFields.add(feedStartX, "4, 4, fill, default");
		feedStartX.setColumns(10);

		feedStartY = new JTextField();
		panelFields.add(feedStartY, "6, 4, fill, default");
		feedStartY.setColumns(10);

		feedStartZ = new JTextField();
		panelFields.add(feedStartZ, "8, 4, fill, default");
		feedStartZ.setColumns(10);

		feedStartAutoFill = new JButton("Set to Current");
		panelFields.add(feedStartAutoFill, "10, 4");

		JLabel lblFeedEndLocation = new JLabel("Feed End Location");
		panelFields.add(lblFeedEndLocation, "2, 6, right, default");

		feedEndX = new JTextField();
		panelFields.add(feedEndX, "4, 6, fill, default");
		feedEndX.setColumns(10);

		feedEndY = new JTextField();
		panelFields.add(feedEndY, "6, 6, fill, default");
		feedEndY.setColumns(10);

		feedEndZ = new JTextField();
		panelFields.add(feedEndZ, "8, 6, fill, default");
		feedEndZ.setColumns(10);

		feedEndAutoFill = new JButton("Set to Current");
		panelFields.add(feedEndAutoFill, "10, 6");

		JSeparator separator = new JSeparator();
		panelFields.add(separator, "2, 8, 7, 1");

		JLabel lblFeedRate = new JLabel("Feed Rate");
		panelFields.add(lblFeedRate, "2, 10, right, default");

		feedRate = new JTextField();
		panelFields.add(feedRate, "4, 10, fill, default");
		feedRate.setColumns(10);

		JScrollPane scrollPane = new JScrollPane(panelFields);
		scrollPane.setBorder(null);
		add(scrollPane, BorderLayout.CENTER);

		JPanel panelActions = new JPanel();
		panelActions.setLayout(new FlowLayout(FlowLayout.RIGHT));
		add(panelActions, BorderLayout.SOUTH);
		
		btnCancel = new JButton(cancelAction);
		panelActions.add(btnCancel);

		btnSave = new JButton(saveAction);
		panelActions.add(btnSave);
		
		feedStartAutoFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Location l = wizardContainer.getMachineControlsPanel().getDisplayedLocation();
				l = l.convertToUnits(feeder.getFeedStartLocation().getUnits());
				feedStartX.setText(l.getLengthX().toString());
				feedStartY.setText(l.getLengthY().toString());
				feedStartZ.setText(l.getLengthZ().toString());
			}
		});
		
		feedEndAutoFill.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				Location l = wizardContainer.getMachineControlsPanel().getDisplayedLocation();
				l = l.convertToUnits(feeder.getFeedEndLocation().getUnits());
				feedEndX.setText(l.getLengthX().toString());
				feedEndY.setText(l.getLengthY().toString());
				feedEndZ.setText(l.getLengthZ().toString());
			}
		});
		
		createBindings();
		loadFromModel();
	}
	
	private void createBindings() {
		LengthConverter lengthConverter = new LengthConverter();
		DoubleConverter doubleConverter = new DoubleConverter("%2.3f");
		BindingListener listener = new AbstractBindingListener() {
			@Override
			public void synced(Binding binding) {
				saveAction.setEnabled(true);
				cancelAction.setEnabled(true);
			}
		};
		
		wrappedBindings.add(JBindings.bind(feeder, "feedStartLocation.lengthX", feedStartX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedStartLocation.lengthY", feedStartY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedStartLocation.lengthZ", feedStartZ, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedEndLocation.lengthX", feedEndX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedEndLocation.lengthY", feedEndY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedEndLocation.lengthZ", feedEndZ, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "feedRate", feedRate, "text", doubleConverter, listener));
	}
	
	private void loadFromModel() {
		for (WrappedBinding wrappedBinding : wrappedBindings) {
			wrappedBinding.reset();
		}
		saveAction.setEnabled(false);
		cancelAction.setEnabled(false);
	}
	
	private void saveToModel() {
		for (WrappedBinding wrappedBinding : wrappedBindings) {
			wrappedBinding.save();
		}
		saveAction.setEnabled(false);
		cancelAction.setEnabled(false);
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
	
	private Action saveAction = new AbstractAction("Apply") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			saveToModel();
			wizardContainer.wizardCompleted(ReferenceTapeFeederConfigurationWizard.this);
		}
	};
	
	private Action cancelAction = new AbstractAction("Reset") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			loadFromModel();
		}
	};
	private JButton btnCancel;
}