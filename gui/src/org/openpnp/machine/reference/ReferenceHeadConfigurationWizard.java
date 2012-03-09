package org.openpnp.machine.reference;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.Binding.SyncFailure;
import org.openpnp.gui.support.Wizard;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.model.LengthUnit;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.AutoBinding;
import org.jdesktop.beansbinding.Bindings;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;

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
	private JComboBox comboBox;
	private JLabel lblUnits;
	private JComboBox comboBox_1;
	private JLabel lblUnits_1;
	
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
				
				lblUnits = new JLabel("Units");
				panelHoming.add(lblUnits, "12, 2");
				
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
				
				comboBox = new JComboBox();
				comboBox.setModel(new DefaultComboBoxModel(LengthUnit.values()));
				panelHoming.add(comboBox, "12, 4, left, default");
				
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
				
				lblUnits_1 = new JLabel("Units");
				panelVision.add(lblUnits_1, "10, 4");
				
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
				
				comboBox_1 = new JComboBox();
				comboBox_1.setModel(new DefaultComboBoxModel(LengthUnit.values()));
				panelVision.add(comboBox_1, "10, 6, left, default");
				
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
		
		btnSave = new JButton("Save");
		panelActions.add(btnSave);
		btnSave.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				wizardContainer.wizardCompleted(ReferenceHeadConfigurationWizard.this);
			}
		});
		initDataBindings();
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
	
	public class JComponentBackgroundBindingListener extends AbstractBindingListener {
		private JComponent component;
		private Color oldBackground;
		
		public JComponentBackgroundBindingListener(JComponent component) {
			this.component = component;
			oldBackground = component.getBackground();
		}
		
		@Override
		public void syncFailed(Binding binding, SyncFailure failure) {
			component.setBackground(Color.red);
		}

		@Override
		public void synced(Binding binding) {
			component.setBackground(oldBackground);
		}
	}
	protected void initDataBindings() {
		BeanProperty<ReferenceHead, String> referenceHeadBeanProperty = BeanProperty.create("id");
		BeanProperty<JTextField, String> jTextFieldBeanProperty = BeanProperty.create("text");
		AutoBinding<ReferenceHead, String, JTextField, String> autoBinding = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty, textFieldId, jTextFieldBeanProperty);
		autoBinding.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_1 = BeanProperty.create("feedRate");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_1 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_1 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_1, textFieldFeedRate, jTextFieldBeanProperty_1);
		autoBinding_1.bind();
		//
		BeanProperty<ReferenceHead, Integer> referenceHeadBeanProperty_2 = BeanProperty.create("pickDwellMilliseconds");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_2 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Integer, JTextField, String> autoBinding_2 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_2, textFieldPickDwell, jTextFieldBeanProperty_2);
		autoBinding_2.bind();
		//
		BeanProperty<ReferenceHead, Integer> referenceHeadBeanProperty_3 = BeanProperty.create("placeDwellMilliseconds");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_3 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Integer, JTextField, String> autoBinding_3 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_3, textFieldPlaceDwell, jTextFieldBeanProperty_3);
		autoBinding_3.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_4 = BeanProperty.create("softLimits.minX");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_4 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_4 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_4, textFieldSoftLimitsXMin, jTextFieldBeanProperty_4);
		autoBinding_4.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_5 = BeanProperty.create("softLimits.maxX");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_5 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_5 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_5, textFieldSoftLimitsXMax, jTextFieldBeanProperty_5);
		autoBinding_5.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_6 = BeanProperty.create("softLimits.minY");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_6 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_6 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_6, textFieldSoftLimitsYMin, jTextFieldBeanProperty_6);
		autoBinding_6.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_7 = BeanProperty.create("softLimits.maxY");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_7 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_7 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_7, textFieldSoftLimitsYMax, jTextFieldBeanProperty_7);
		autoBinding_7.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_8 = BeanProperty.create("softLimits.minZ");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_8 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_8 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_8, textFieldSoftLimitsZMin, jTextFieldBeanProperty_8);
		autoBinding_8.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_9 = BeanProperty.create("softLimits.maxZ");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_9 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_9 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_9, textFieldSoftLimitsZMax, jTextFieldBeanProperty_9);
		autoBinding_9.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_10 = BeanProperty.create("softLimits.minC");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_10 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_10 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_10, textFieldSoftLimitsCMin, jTextFieldBeanProperty_10);
		autoBinding_10.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_11 = BeanProperty.create("softLimits.maxC");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_11 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_11 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_11, textFieldSoftLimitsCMax, jTextFieldBeanProperty_11);
		autoBinding_11.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_12 = BeanProperty.create("homing.location.x");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_12 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_12 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_12, textFieldHomeLocationX, jTextFieldBeanProperty_12);
		autoBinding_12.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_13 = BeanProperty.create("homing.location.y");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_13 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_13 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_13, textFieldHomeLocationY, jTextFieldBeanProperty_13);
		autoBinding_13.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_14 = BeanProperty.create("homing.location.z");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_14 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_14 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_14, textFieldHomeLocationZ, jTextFieldBeanProperty_14);
		autoBinding_14.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_15 = BeanProperty.create("homing.location.rotation");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_15 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_15 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_15, textFieldHomeLocationC, jTextFieldBeanProperty_15);
		autoBinding_15.bind();
		//
		BeanProperty<ReferenceHead, LengthUnit> referenceHeadBeanProperty_16 = BeanProperty.create("homing.location.units");
		BeanProperty<JComboBox, Object> jComboBoxBeanProperty = BeanProperty.create("selectedItem");
		AutoBinding<ReferenceHead, LengthUnit, JComboBox, Object> autoBinding_16 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_16, comboBox, jComboBoxBeanProperty);
		autoBinding_16.bind();
		//
		BeanProperty<ReferenceHead, Boolean> referenceHeadBeanProperty_17 = BeanProperty.create("homing.vision.enabled");
		BeanProperty<JCheckBox, Boolean> jCheckBoxBeanProperty = BeanProperty.create("selected");
		AutoBinding<ReferenceHead, Boolean, JCheckBox, Boolean> autoBinding_17 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_17, chckbxVisionEnabled, jCheckBoxBeanProperty);
		autoBinding_17.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_18 = BeanProperty.create("homing.vision.homingDotDiameter");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_16 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_18 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_18, textFieldHomingDotDiameter, jTextFieldBeanProperty_16);
		autoBinding_18.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_19 = BeanProperty.create("homing.vision.homingDotLocation.x");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_17 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_19 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_19, textFieldHomingDotX, jTextFieldBeanProperty_17);
		autoBinding_19.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_20 = BeanProperty.create("homing.vision.homingDotLocation.y");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_18 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_20 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_20, textFieldHomingDotY, jTextFieldBeanProperty_18);
		autoBinding_20.bind();
		//
		BeanProperty<ReferenceHead, Double> referenceHeadBeanProperty_21 = BeanProperty.create("homing.vision.homingDotLocation.z");
		BeanProperty<JTextField, String> jTextFieldBeanProperty_19 = BeanProperty.create("text");
		AutoBinding<ReferenceHead, Double, JTextField, String> autoBinding_21 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_21, textFieldHomingDotZ, jTextFieldBeanProperty_19);
		autoBinding_21.bind();
		//
		BeanProperty<ReferenceHead, LengthUnit> referenceHeadBeanProperty_22 = BeanProperty.create("homing.vision.homingDotLocation.units");
		AutoBinding<ReferenceHead, LengthUnit, JComboBox, Object> autoBinding_22 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_22, comboBox_1, jComboBoxBeanProperty);
		autoBinding_22.bind();
		//
		BeanProperty<JTextField, Boolean> jTextFieldBeanProperty_20 = BeanProperty.create("enabled");
		AutoBinding<ReferenceHead, Boolean, JTextField, Boolean> autoBinding_23 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_17, textFieldHomingDotDiameter, jTextFieldBeanProperty_20);
		autoBinding_23.bind();
		//
		AutoBinding<ReferenceHead, Boolean, JTextField, Boolean> autoBinding_24 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_17, textFieldHomingDotZ, jTextFieldBeanProperty_20);
		autoBinding_24.bind();
		//
		AutoBinding<ReferenceHead, Boolean, JTextField, Boolean> autoBinding_25 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_17, textFieldHomingDotY, jTextFieldBeanProperty_20);
		autoBinding_25.bind();
		//
		AutoBinding<ReferenceHead, Boolean, JTextField, Boolean> autoBinding_26 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_17, textFieldHomingDotX, jTextFieldBeanProperty_20);
		autoBinding_26.bind();
		//
		BeanProperty<JComboBox, Boolean> jComboBoxBeanProperty_1 = BeanProperty.create("enabled");
		AutoBinding<ReferenceHead, Boolean, JComboBox, Boolean> autoBinding_27 = Bindings.createAutoBinding(UpdateStrategy.READ_WRITE, head, referenceHeadBeanProperty_17, comboBox_1, jComboBoxBeanProperty_1);
		autoBinding_27.bind();
	}
}
