package org.openpnp.machine.reference;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.BindingListener;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.IntegerConverter;
import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.JBindings.WrappedBinding;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceHeadConfigurationWizard extends JPanel implements Wizard {
	private final ReferenceHead head;
	
	private JTextField textFieldFeedRate;
	private JTextField textFieldId;
	private JTextField textFieldPickDwell;
	private JTextField textFieldPlaceDwell;
	private JLabel lblNewLabel;
	private JLabel lblX;
	private JLabel lblY;
	private JLabel lblZ;
	private JLabel lblC;
	private JTextField textFieldSoftLimitsXMin;
	private JTextField textFieldSoftLimitsXMax;
	private JLabel lblMinimum;
	private JLabel lblMacimum;
	private JTextField textFieldSoftLimitsYMin;
	private JTextField textFieldSoftLimitsYMax;
	private JTextField textFieldSoftLimitsZMin;
	private JTextField textFieldSoftLimitsZMax;
	private JTextField textFieldSoftLimitsCMin;
	private JTextField textFieldSoftLimitsCMax;
	private JCheckBox chckbxVisionEnabled;
	private JLabel lblHomingDotDiameter;
	private JLabel lblNewLabel_1;
	private JTextField textFieldHomingDotDiameter;
	private JLabel lblX_1;
	private JLabel lblY_1;
	private JLabel lblZ_1;
	private JTextField textFieldHomingDotX;
	private JTextField textFieldHomingDotY;
	private JTextField textFieldHomingDotZ;
	private JButton btnSave;
	private JButton btnCancel;
	
	private WizardContainer wizardContainer; 
	private JPanel panelGeneral;
	private JPanel panelSoftLimits;
	private JPanel panelHoming;
	private JPanel panelVision;
	private JPanel panelActions;
	private JLabel lblX_2;
	private JLabel lblY_2;
	private JLabel lblZ_2;
	private JLabel lblC_1;
	private JLabel lblHomeLocation;
	private JTextField textFieldHomeLocationX;
	private JTextField textFieldHomeLocationY;
	private JTextField textFieldHomeLocationZ;
	private JTextField textFieldHomeLocationC;
	private JScrollPane scrollPane;
	private JPanel panelMain;
	
	private List<WrappedBinding> wrappedBindings = new ArrayList<WrappedBinding>();
	
	public ReferenceHeadConfigurationWizard(ReferenceHead head) {
		this.head = head;
		
		setLayout(new BorderLayout(0, 0));
		
		panelMain = new JPanel();
		
		scrollPane = new JScrollPane(panelMain);
		scrollPane.setBorder(null);
		panelMain.setLayout(new BoxLayout(panelMain, BoxLayout.Y_AXIS));
		
		panelGeneral = new JPanel();
		panelMain.add(panelGeneral);
		panelGeneral.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,},
			new RowSpec[] {
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,
				FormFactory.RELATED_GAP_ROWSPEC,
				FormFactory.DEFAULT_ROWSPEC,}));
		
		JLabel lblId = new JLabel("Id");
		panelGeneral.add(lblId, "2, 2, right, default");
		
		textFieldId = new JTextField();
		panelGeneral.add(textFieldId, "4, 2");
		textFieldId.setColumns(5);
		
		JLabel lblFeedRate = new JLabel("Feed Rate (mm/sec)");
		panelGeneral.add(lblFeedRate, "6, 2, right, default");
		
		textFieldFeedRate = new JTextField();
		panelGeneral.add(textFieldFeedRate, "8, 2");
		textFieldFeedRate.setColumns(5);
		
		lblNewLabel = new JLabel("Pick Dwell (ms)");
		panelGeneral.add(lblNewLabel, "2, 4, right, default");
		
		textFieldPickDwell = new JTextField();
		panelGeneral.add(textFieldPickDwell, "4, 4");
		textFieldPickDwell.setColumns(5);
		
		JLabel lblPlaceDwell = new JLabel("Place Dwell (ms)");
		panelGeneral.add(lblPlaceDwell, "6, 4, right, default");
		
		textFieldPlaceDwell = new JTextField();
		panelGeneral.add(textFieldPlaceDwell, "8, 4");
		textFieldPlaceDwell.setColumns(5);
		
		panelSoftLimits = new JPanel();
		panelMain.add(panelSoftLimits);
		panelSoftLimits.setBorder(new TitledBorder(null, "Soft Limits", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelSoftLimits.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
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
				FormFactory.DEFAULT_ROWSPEC,}));
		
		lblMinimum = new JLabel("Minimum (mm)");
		panelSoftLimits.add(lblMinimum, "4, 2");
		
		lblMacimum = new JLabel("Maximum (mm)");
		panelSoftLimits.add(lblMacimum, "6, 2");
		
		lblX = new JLabel("X");
		panelSoftLimits.add(lblX, "2, 4, right, default");
		
		textFieldSoftLimitsXMin = new JTextField();
		panelSoftLimits.add(textFieldSoftLimitsXMin, "4, 4");
		textFieldSoftLimitsXMin.setColumns(5);
		
		textFieldSoftLimitsXMax = new JTextField();
		panelSoftLimits.add(textFieldSoftLimitsXMax, "6, 4");
		textFieldSoftLimitsXMax.setColumns(5);
		
		lblY = new JLabel("Y");
		panelSoftLimits.add(lblY, "2, 6, right, default");
		
		textFieldSoftLimitsYMin = new JTextField();
		panelSoftLimits.add(textFieldSoftLimitsYMin, "4, 6");
		textFieldSoftLimitsYMin.setColumns(5);
		
		textFieldSoftLimitsYMax = new JTextField();
		panelSoftLimits.add(textFieldSoftLimitsYMax, "6, 6");
		textFieldSoftLimitsYMax.setColumns(5);
		
		lblZ = new JLabel("Z");
		panelSoftLimits.add(lblZ, "2, 8, right, default");
		
		textFieldSoftLimitsZMin = new JTextField();
		panelSoftLimits.add(textFieldSoftLimitsZMin, "4, 8");
		textFieldSoftLimitsZMin.setColumns(5);
		
		textFieldSoftLimitsZMax = new JTextField();
		panelSoftLimits.add(textFieldSoftLimitsZMax, "6, 8");
		textFieldSoftLimitsZMax.setColumns(5);
		
		lblC = new JLabel("C");
		panelSoftLimits.add(lblC, "2, 10, right, default");
		
		textFieldSoftLimitsCMin = new JTextField();
		panelSoftLimits.add(textFieldSoftLimitsCMin, "4, 10");
		textFieldSoftLimitsCMin.setColumns(5);
		
		textFieldSoftLimitsCMax = new JTextField();
		panelSoftLimits.add(textFieldSoftLimitsCMax, "6, 10");
		textFieldSoftLimitsCMax.setColumns(5);
		
		panelHoming = new JPanel();
		panelMain.add(panelHoming);
		panelHoming.setBorder(new TitledBorder(null, "Homing", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelHoming.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormFactory.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
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
				FormFactory.DEFAULT_ROWSPEC,}));
		
		lblX_2 = new JLabel("X");
		panelHoming.add(lblX_2, "4, 2, center, default");
		
		lblY_2 = new JLabel("Y");
		panelHoming.add(lblY_2, "6, 2, center, default");
		
		lblZ_2 = new JLabel("Z");
		panelHoming.add(lblZ_2, "8, 2, center, default");
		
		lblC_1 = new JLabel("C");
		panelHoming.add(lblC_1, "10, 2, center, default");
		
		lblHomeLocation = new JLabel("Home Location");
		panelHoming.add(lblHomeLocation, "2, 4, right, default");
		
		textFieldHomeLocationX = new JTextField();
		panelHoming.add(textFieldHomeLocationX, "4, 4, fill, default");
		textFieldHomeLocationX.setColumns(5);
		
		textFieldHomeLocationY = new JTextField();
		panelHoming.add(textFieldHomeLocationY, "6, 4, fill, default");
		textFieldHomeLocationY.setColumns(5);
		
		textFieldHomeLocationZ = new JTextField();
		panelHoming.add(textFieldHomeLocationZ, "8, 4, fill, default");
		textFieldHomeLocationZ.setColumns(5);
		
		textFieldHomeLocationC = new JTextField();
		panelHoming.add(textFieldHomeLocationC, "10, 4, fill, default");
		textFieldHomeLocationC.setColumns(5);
		
		panelVision = new JPanel();
		panelMain.add(panelVision);
		panelVision.setBorder(new TitledBorder(null, "Vision", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panelVision.setLayout(new FormLayout(new ColumnSpec[] {
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
				FormFactory.RELATED_GAP_COLSPEC,
				FormFactory.DEFAULT_COLSPEC,
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
				FormFactory.DEFAULT_ROWSPEC,}));
		
		chckbxVisionEnabled = new JCheckBox("Vision Enabled?");
		panelVision.add(chckbxVisionEnabled, "2, 2");
		
		lblX_1 = new JLabel("X");
		panelVision.add(lblX_1, "4, 4, center, default");
		
		lblY_1 = new JLabel("Y");
		panelVision.add(lblY_1, "6, 4, center, default");
		
		lblZ_1 = new JLabel("Z");
		panelVision.add(lblZ_1, "8, 4, center, default");
		
		lblNewLabel_1 = new JLabel("Homing Dot Location");
		panelVision.add(lblNewLabel_1, "2, 6, right, default");
		
		textFieldHomingDotX = new JTextField();
		panelVision.add(textFieldHomingDotX, "4, 6");
		textFieldHomingDotX.setColumns(5);
		
		textFieldHomingDotY = new JTextField();
		panelVision.add(textFieldHomingDotY, "6, 6");
		textFieldHomingDotY.setColumns(5);
		
		textFieldHomingDotZ = new JTextField();
		panelVision.add(textFieldHomingDotZ, "8, 6");
		textFieldHomingDotZ.setColumns(5);
		
		lblHomingDotDiameter = new JLabel("Homing Dot Diameter (mm)");
		panelVision.add(lblHomingDotDiameter, "2, 8, right, default");
		
		textFieldHomingDotDiameter = new JTextField();
		panelVision.add(textFieldHomingDotDiameter, "4, 8");
		textFieldHomingDotDiameter.setColumns(5);
		add(scrollPane, BorderLayout.CENTER);
		
		panelActions = new JPanel();
		FlowLayout fl_panelActions = (FlowLayout) panelActions.getLayout();
		fl_panelActions.setAlignment(FlowLayout.RIGHT);
		add(panelActions, BorderLayout.SOUTH);
		
		btnCancel = new JButton(cancelAction);
		panelActions.add(btnCancel);
		
		btnSave = new JButton(saveAction);
		panelActions.add(btnSave);
		
		createBindings();
		loadFromModel();
	}
	
	private void createBindings() {
		LengthConverter lengthConverter = new LengthConverter();
		IntegerConverter integerConverter = new IntegerConverter("%d");
		DoubleConverter doubleConverter = new DoubleConverter("%2.3f");
		BindingListener listener = new AbstractBindingListener() {
			@Override
			public void synced(Binding binding) {
				saveAction.setEnabled(true);
				cancelAction.setEnabled(true);
			}
		};
		
		wrappedBindings.add(JBindings.bind(head, "id", textFieldId, "text", listener));
		wrappedBindings.add(JBindings.bind(head, "feedRate", textFieldFeedRate, "text", doubleConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "pickDwellMilliseconds", textFieldPickDwell, "text", integerConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "placeDwellMilliseconds", textFieldPlaceDwell, "text", integerConverter, listener));

		wrappedBindings.add(JBindings.bind(head, "softLimits.minX", textFieldSoftLimitsXMin, "text", doubleConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "softLimits.maxX", textFieldSoftLimitsXMax, "text", doubleConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "softLimits.minY", textFieldSoftLimitsYMin, "text", doubleConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "softLimits.maxY", textFieldSoftLimitsYMax, "text", doubleConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "softLimits.minZ", textFieldSoftLimitsZMin, "text", doubleConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "softLimits.maxZ", textFieldSoftLimitsZMax, "text", doubleConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "softLimits.minC", textFieldSoftLimitsCMin, "text", doubleConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "softLimits.maxC", textFieldSoftLimitsCMax, "text", doubleConverter, listener));
		
		wrappedBindings.add(JBindings.bind(head, "homing.location.lengthX", textFieldHomeLocationX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "homing.location.lengthY", textFieldHomeLocationY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "homing.location.lengthZ", textFieldHomeLocationZ, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "homing.location.rotation", textFieldHomeLocationC, "text", doubleConverter, listener));
		
		wrappedBindings.add(JBindings.bind(head, "homing.vision.enabled", chckbxVisionEnabled, "selected", listener));
		wrappedBindings.add(JBindings.bind(head, "homing.vision.homingDotDiameter", textFieldHomingDotDiameter, "text", doubleConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "homing.vision.homingDotLocation.lengthX", textFieldHomingDotX, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "homing.vision.homingDotLocation.lengthY", textFieldHomingDotY, "text", lengthConverter, listener));
		wrappedBindings.add(JBindings.bind(head, "homing.vision.homingDotLocation.lengthZ", textFieldHomingDotZ, "text", lengthConverter, listener));
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
			wizardContainer.wizardCompleted(ReferenceHeadConfigurationWizard.this);
		}
	};
	
	private Action cancelAction = new AbstractAction("Reset") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			loadFromModel();
		}
	};
}
