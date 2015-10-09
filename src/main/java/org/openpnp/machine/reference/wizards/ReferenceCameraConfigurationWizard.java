package org.openpnp.machine.reference.wizards;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.model.Configuration;

import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.gui.components.LocationButtonsPanel;
import javax.swing.JCheckBox;

public class ReferenceCameraConfigurationWizard extends
        AbstractConfigurationWizard {
    private final ReferenceCamera referenceCamera;
    
    private JTextField textFieldOffX;
    private JTextField textFieldOffY;
    private JTextField textFieldOffZ;
    private JPanel panelOffsets;
    private JPanel panelGeneral;
    private JLabel lblRotation;
    private JTextField textFieldRotation;
    private JPanel panelLocation;
    private JLabel lblX;
    private JLabel lblY;
    private JLabel lblZ;
    private JLabel lblRotation_1;
    private JTextField textFieldLocationX;
    private JTextField textFieldLocationY;
    private JTextField textFieldLocationZ;
    private JTextField textFieldLocationRotation;
    private LocationButtonsPanel locationButtonsPanel;
    private JCheckBox chckbxFlipX;
    private JLabel lblFlipX;
    private JLabel lblFlipY;
    private JCheckBox checkBoxFlipY;
    
    
    public ReferenceCameraConfigurationWizard(ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;
        
        panelGeneral = new JPanel();
        panelGeneral.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelGeneral);
        panelGeneral.setLayout(new FormLayout(new ColumnSpec[] {
                FormFactory.RELATED_GAP_COLSPEC,
                FormFactory.DEFAULT_COLSPEC,
                FormFactory.RELATED_GAP_COLSPEC,
                FormFactory.DEFAULT_COLSPEC,
                ColumnSpec.decode("default:grow"),},
            new RowSpec[] {
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,}));
        
        lblRotation = new JLabel("Rotation");
        panelGeneral.add(lblRotation, "2, 2, right, default");
        
        textFieldRotation = new JTextField();
        panelGeneral.add(textFieldRotation, "4, 2");
        textFieldRotation.setColumns(10);
        
        lblFlipX = new JLabel("Flip Vertical");
        panelGeneral.add(lblFlipX, "2, 4, right, default");
        
        chckbxFlipX = new JCheckBox("");
        panelGeneral.add(chckbxFlipX, "4, 4");
        
        lblFlipY = new JLabel("Flip Horizontal");
        panelGeneral.add(lblFlipY, "2, 6, right, default");
        
        checkBoxFlipY = new JCheckBox("");
        panelGeneral.add(checkBoxFlipY, "4, 6");

        panelOffsets = new JPanel();
        contentPanel.add(panelOffsets);
        panelOffsets.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Offsets", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
        panelOffsets.setLayout(new FormLayout(new ColumnSpec[] {
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
                FormFactory.DEFAULT_ROWSPEC,
                FormFactory.RELATED_GAP_ROWSPEC,
                FormFactory.DEFAULT_ROWSPEC,}));

        JLabel olblX = new JLabel("X");
        panelOffsets.add(olblX, "2, 2");

        JLabel olblY = new JLabel("Y");
        panelOffsets.add(olblY, "4, 2");

        JLabel olblZ = new JLabel("Z");
        panelOffsets.add(olblZ, "6, 2");
        
        
        textFieldOffX = new JTextField();
        panelOffsets.add(textFieldOffX, "2, 4");
        textFieldOffX.setColumns(8);

        textFieldOffY = new JTextField();
        panelOffsets.add(textFieldOffY, "4, 4");
        textFieldOffY.setColumns(8);

        textFieldOffZ = new JTextField();
        panelOffsets.add(textFieldOffZ, "6, 4");
        textFieldOffZ.setColumns(8);
        
        panelLocation = new JPanel();
        panelLocation.setBorder(new TitledBorder(null, "Location", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panelLocation);
        panelLocation.setLayout(new FormLayout(new ColumnSpec[] {
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
                RowSpec.decode("default:grow"),}));
        
        lblX = new JLabel("X");
        panelLocation.add(lblX, "2, 2");
        
        lblY = new JLabel("Y");
        panelLocation.add(lblY, "4, 2");
        
        lblZ = new JLabel("Z");
        panelLocation.add(lblZ, "6, 2");
        
        lblRotation_1 = new JLabel("Rotation");
        panelLocation.add(lblRotation_1, "8, 2");
        
        textFieldLocationX = new JTextField();
        panelLocation.add(textFieldLocationX, "2, 4, fill, default");
        textFieldLocationX.setColumns(8);
        
        textFieldLocationY = new JTextField();
        panelLocation.add(textFieldLocationY, "4, 4, fill, default");
        textFieldLocationY.setColumns(8);
        
        textFieldLocationZ = new JTextField();
        panelLocation.add(textFieldLocationZ, "6, 4, fill, default");
        textFieldLocationZ.setColumns(8);
        
        textFieldLocationRotation = new JTextField();
        panelLocation.add(textFieldLocationRotation, "8, 4, fill, default");
        textFieldLocationRotation.setColumns(8);
        
        try {
            // Causes WindowBuilder to fail, so just throw away the error.
            if (referenceCamera.getHead() == null) {
                locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX, textFieldLocationY, textFieldLocationZ, textFieldLocationRotation);
                panelLocation.add(locationButtonsPanel, "10, 4, fill, fill");
                panelOffsets.setVisible(false);
            }
            else {
                panelLocation.setVisible(false);
            }
        }
        catch (Exception e) {
            
        }
    }
    
    @Override
    public void createBindings() {
        DoubleConverter doubleConverter = new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        LengthConverter lengthConverter = new LengthConverter();

        if (referenceCamera.getHead() == null) {
            // fixed camera
            MutableLocationProxy headOffsets = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, referenceCamera, "headOffsets", headOffsets, "location");
            addWrappedBinding(headOffsets, "lengthX", textFieldLocationX, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthY", textFieldLocationY, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthZ", textFieldLocationZ, "text", lengthConverter);
            addWrappedBinding(headOffsets, "rotation", textFieldLocationRotation, "text", doubleConverter);
        }
        else {
            // moving camera
            MutableLocationProxy headOffsets = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, referenceCamera, "headOffsets", headOffsets, "location");
            addWrappedBinding(headOffsets, "lengthX", textFieldOffX, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthY", textFieldOffY, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthZ", textFieldOffZ, "text", lengthConverter);
        }

        addWrappedBinding(referenceCamera, "rotation", textFieldRotation, "text", doubleConverter);
        addWrappedBinding(referenceCamera, "flipX", chckbxFlipX, "selected");
        addWrappedBinding(referenceCamera, "flipY", checkBoxFlipY, "selected");
        
        ComponentDecorators.decorateWithAutoSelect(textFieldRotation);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffZ);
        
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationRotation);
    }
}
