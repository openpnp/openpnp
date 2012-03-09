package org.openpnp.machine.reference.feeder;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingListener;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.JBindings.WrappedBinding;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;

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
	
	private List<WrappedBinding> wrappedBindings = new ArrayList<WrappedBinding>();

	public ReferenceTrayFeederConfigurationWizard(ReferenceTrayFeeder referenceTrayFeeder) {
		feeder = referenceTrayFeeder;
		
		JPanel panelFields = new JPanel();
		
		panelFields.setLayout(new FormLayout(new ColumnSpec[] {
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
		panelFields.add(lblX, "4, 2");

		JLabel lblY = new JLabel("Y");
		panelFields.add(lblY, "6, 2");

		JLabel lblZ = new JLabel("Z");
		panelFields.add(lblZ, "8, 2");

		JLabel lblFeedStartLocation = new JLabel("Offsets");
		panelFields.add(lblFeedStartLocation, "2, 4, right, default");

		offsetsX = new JTextField();
		panelFields.add(offsetsX, "4, 4, fill, default");
		offsetsX.setColumns(10);

		offsetsY = new JTextField();
		panelFields.add(offsetsY, "6, 4, fill, default");
		offsetsY.setColumns(10);

		offsetsZ = new JTextField();
		panelFields.add(offsetsZ, "8, 4, fill, default");
		offsetsZ.setColumns(10);

		JLabel lblTrayCount = new JLabel("Tray Count");
		panelFields.add(lblTrayCount, "2, 6, right, default");

		trayCountX = new JTextField();
		panelFields.add(trayCountX, "4, 6, fill, default");
		trayCountX.setColumns(10);

		trayCountY = new JTextField();
		panelFields.add(trayCountY, "6, 6, fill, default");
		trayCountY.setColumns(10);

		JButton btnSave = new JButton(saveAction);
		setLayout(new BorderLayout(0, 0));
		
		add(new JScrollPane(panelFields), BorderLayout.CENTER);
		
		JPanel panelActions = new JPanel();
		panelActions.setLayout(new FlowLayout(FlowLayout.RIGHT));
		
		JButton btnCancel = new JButton(cancelAction);
		panelActions.add(btnCancel);
		
		panelActions.add(btnSave, "8, 26");
		
		add(panelActions, BorderLayout.SOUTH);

		createBindings();
		loadFromModel();
	}
	
	private void createBindings() {
		LengthConverter lengthConverter = new LengthConverter();
		IntegerConverter integerConverter = new IntegerConverter("%d");
		BindingListener listener = new AbstractBindingListener() {
			@Override
			public void synced(Binding binding) {
				saveAction.setEnabled(true);
				cancelAction.setEnabled(true);
			}
		};
		
		wrappedBindings.add(JBindings.bind(feeder, "offsets.lengthX", offsetsX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "offsets.lengthY", offsetsY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "offsets.lengthZ", offsetsZ, "text", lengthConverter, listener));
		
		wrappedBindings.add(JBindings.bind(feeder, "trayCountX", trayCountX, "text", integerConverter, listener));
		wrappedBindings.add(JBindings.bind(feeder, "trayCountY", trayCountY, "text", integerConverter, listener));
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
			wizardContainer.wizardCompleted(ReferenceTrayFeederConfigurationWizard.this);
		}
	};
	
	private Action cancelAction = new AbstractAction("Reset") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			loadFromModel();
		}
	};
}