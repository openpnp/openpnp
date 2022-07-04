package org.openpnp.machine.reference.wizards;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.gui.components.LocationButtonsPanel;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.AxesComboBoxModel;
import org.openpnp.gui.support.DoubleConverter;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.gui.support.NamedConverter;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractAxis;
import org.openpnp.spi.base.AbstractMachine;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class ReferenceCameraPositionConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceCamera referenceCamera;

    private JTextField textFieldOffX;
    private JTextField textFieldOffY;
    private JTextField textFieldOffZ;
    private JPanel panelOffsets;
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
    private JTextField textFieldSafeZ;
    private JComboBox axisX;
    private JComboBox axisY;
    private JComboBox axisZ;
    private JLabel lblAxis;
    private JLabel lblOffset;
    private JComboBox axisRotation;
    private JLabel lblRotation;
    private JTextField textFieldOffRotation;
    private JLabel lblLocation;
    private JLabel lblRoamingRadius;
    private JTextField roamingRadius;


    public ReferenceCameraPositionConfigurationWizard(AbstractMachine machine, ReferenceCamera referenceCamera) {
        this.referenceCamera = referenceCamera;

        panelOffsets = new JPanel();
        contentPanel.add(panelOffsets);
        panelOffsets.setBorder(new TitledBorder(null,
                "Coordinate System", TitledBorder.LEADING, TitledBorder.TOP, null));
        panelOffsets.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel olblX = new JLabel("X");
        panelOffsets.add(olblX, "4, 2");

        JLabel olblY = new JLabel("Y");
        panelOffsets.add(olblY, "6, 2");

        JLabel olblZ = new JLabel("Z");
        panelOffsets.add(olblZ, "8, 2");
        
        lblRotation = new JLabel("Rotation");
        panelOffsets.add(lblRotation, "10, 2");
        
        lblAxis = new JLabel("Axis");
        panelOffsets.add(lblAxis, "2, 4, right, default");
        
        axisX = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.X, true));
        panelOffsets.add(axisX, "4, 4, fill, default");
        
        axisY = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Y, true));
        panelOffsets.add(axisY, "6, 4, fill, default");
        
        axisZ = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Z, true));
        panelOffsets.add(axisZ, "8, 4, fill, default");
        
        axisRotation = new JComboBox(new AxesComboBoxModel(machine, AbstractAxis.class, Axis.Type.Rotation, true));
        panelOffsets.add(axisRotation, "10, 4, fill, default");
        
        lblOffset = new JLabel("Offset");
        panelOffsets.add(lblOffset, "2, 6, right, default");


        textFieldOffX = new JTextField();
        panelOffsets.add(textFieldOffX, "4, 6");
        textFieldOffX.setColumns(10);

        textFieldOffY = new JTextField();
        panelOffsets.add(textFieldOffY, "6, 6");
        textFieldOffY.setColumns(10);

        textFieldOffZ = new JTextField();
        panelOffsets.add(textFieldOffZ, "8, 6");
        textFieldOffZ.setColumns(10);

        textFieldOffRotation = new JTextField();
        panelOffsets.add(textFieldOffRotation, "10, 6, fill, default");
        textFieldOffRotation.setColumns(10);

        JLabel lblSafeZ = new JLabel("Safe Z");
        panelOffsets.add(lblSafeZ, "2, 10, right, default");

        textFieldSafeZ = new JTextField();
        panelOffsets.add(textFieldSafeZ, "8, 10");
        textFieldSafeZ.setEditable(false);
        textFieldSafeZ.setColumns(10);

        panelLocation = new JPanel();
        panelLocation.setBorder(new TitledBorder(null, "Location", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        contentPanel.add(panelLocation);
        panelLocation.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("max(70dlu;default)"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("left:default"),},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));

        lblX = new JLabel("X");
        panelLocation.add(lblX, "4, 2");

        lblY = new JLabel("Y");
        panelLocation.add(lblY, "6, 2");

        lblZ = new JLabel("Z");
        panelLocation.add(lblZ, "8, 2");

        lblRotation_1 = new JLabel("Rotation");
        panelLocation.add(lblRotation_1, "10, 2");
        
        lblLocation = new JLabel("Location");
        panelLocation.add(lblLocation, "2, 4, right, default");

        textFieldLocationX = new JTextField();
        panelLocation.add(textFieldLocationX, "4, 4, fill, default");
        textFieldLocationX.setColumns(10);

        textFieldLocationY = new JTextField();
        panelLocation.add(textFieldLocationY, "6, 4, fill, default");
        textFieldLocationY.setColumns(10);

        textFieldLocationZ = new JTextField();
        panelLocation.add(textFieldLocationZ, "8, 4, fill, default");
        textFieldLocationZ.setColumns(10);

        textFieldLocationRotation = new JTextField();
        panelLocation.add(textFieldLocationRotation, "10, 4, fill, default");
        textFieldLocationRotation.setColumns(10);
        
        lblRoamingRadius = new JLabel("Roaming Radius");
        lblRoamingRadius.setToolTipText("<html>\r\n<p>The maximum <em>nominal</em> roaming radius over the camera<br/>\r\nwhich also indicates the largest part diagonal that can be<br/>\r\nsupported.</p>\r\n<br/>\r\n<p>If set to zero, this switches off multi-shot vision<br/> \r\n(see package <strong>Vision Compositing</strong>).</p> \r\n<br/>\r\n<p>During bottom vision, the nozzle movement will be <br/>\r\nrestricted, taking into consideration the following:</p>\r\n<ul>\r\n<li>The distance of the nozzle from the camera <br/>\r\ncenter.</li>\r\n<li>How much the part footprint is portruding <br/>\r\nfrom there (approximated by octogonal hull).</li>\r\n</ul>\r\n<p>Inside the roaming radius, the nozzle will also be <br/>\r\nfreely moved at camera Z, i.e. without going to Safe Z.</p> \r\n<p>Note, this is the <em>nominal</em> radius, i.e. there must be<br/>\r\nextra space available for pick offsets and other <br/>\r\ndeviations.</p>\r\n<br/>\r\n<p><strong color=\"red\">Caution:</strong> the roaming radius is not enforced \r\nwhen jogging.</p>\r\n</html>");
        panelLocation.add(lblRoamingRadius, "2, 6, right, default");
        
        roamingRadius = new JTextField();
        panelLocation.add(roamingRadius, "4, 6, fill, default");
        roamingRadius.setColumns(10);
        locationButtonsPanel = new LocationButtonsPanel(textFieldLocationX,
                textFieldLocationY, textFieldLocationZ, textFieldLocationRotation);
        panelLocation.add(locationButtonsPanel, "12, 4, fill, fill");
        try {
            // Causes WindowBuilder to fail, so just throw away the error.
            if (referenceCamera.getHead() == null) {
                // Fixed camera, add the location fields and buttons and turn off offsets.
                panelOffsets.setVisible(false);    
            }
            else {
                // Moving camera, hide location and show only offsets.
                panelLocation.setVisible(false);
            }
        }
        catch (Exception e) {

        }
    }

    @Override
    public void createBindings() {
        AbstractMachine machine = (AbstractMachine) Configuration.get().getMachine();
        DoubleConverter doubleConverter =
                new DoubleConverter(Configuration.get().getLengthDisplayFormat());
        LengthConverter lengthConverter = new LengthConverter();
        NamedConverter<Axis> axisConverter = new NamedConverter<>(machine.getAxes()); 

        if (referenceCamera.getHead() == null) {
            // fixed camera
            MutableLocationProxy headOffsets = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, referenceCamera, "headOffsets", headOffsets,
                    "location");
            addWrappedBinding(headOffsets, "lengthX", textFieldLocationX, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthY", textFieldLocationY, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthZ", textFieldLocationZ, "text", lengthConverter);
            addWrappedBinding(headOffsets, "rotation", textFieldLocationRotation, "text",
                    doubleConverter);
        }
        else {
            // moving camera
            addWrappedBinding(referenceCamera, "axisX", axisX, "selectedItem", axisConverter);
            addWrappedBinding(referenceCamera, "axisY", axisY, "selectedItem", axisConverter);
            addWrappedBinding(referenceCamera, "axisZ", axisZ, "selectedItem", axisConverter);
            addWrappedBinding(referenceCamera, "axisRotation", axisRotation, "selectedItem", axisConverter);

            MutableLocationProxy headOffsets = new MutableLocationProxy();
            bind(UpdateStrategy.READ_WRITE, referenceCamera, "headOffsets", headOffsets,
                    "location");
            addWrappedBinding(headOffsets, "lengthX", textFieldOffX, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthY", textFieldOffY, "text", lengthConverter);
            addWrappedBinding(headOffsets, "lengthZ", textFieldOffZ, "text", lengthConverter);
            addWrappedBinding(headOffsets, "rotation", textFieldOffRotation, "text", doubleConverter);
            
            addWrappedBinding(referenceCamera, "safeZ", textFieldSafeZ, "text", lengthConverter);
        }
        addWrappedBinding(referenceCamera, "roamingRadius", roamingRadius, "text", lengthConverter);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldOffRotation);

        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationX);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationY);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationZ);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldLocationRotation);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(roamingRadius);
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(textFieldSafeZ);
    }
}
