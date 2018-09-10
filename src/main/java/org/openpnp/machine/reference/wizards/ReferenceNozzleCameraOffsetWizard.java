package org.openpnp.machine.reference.wizards;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.jdesktop.beansbinding.AutoBinding;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.gui.support.CameraItem;
import org.openpnp.gui.support.LengthConverter;
import org.openpnp.gui.support.MutableLocationProxy;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.pmw.tinylog.Logger;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.ActionEvent;
import java.util.List;

public class ReferenceNozzleCameraOffsetWizard extends AbstractConfigurationWizard {

    private JComboBox<CameraItem> camerasComboBox;

    private JTextField nozzleMarkLocationX, nozzleMarkLocationY, nozzleMarkLocationZ;
    private JTextField nozzleOffsetLocationX, nozzleOffsetLocationY, nozzleOffsetLocationZ;

    private ReferenceNozzle nozzle;
    private MutableLocationProxy nozzleMarkLocation, nozzleOffsetLocation;

    public ReferenceNozzleCameraOffsetWizard(ReferenceNozzle nozzle) {

        this.nozzle = nozzle;
        this.nozzleMarkLocation = new MutableLocationProxy();
        this.nozzleOffsetLocation = new MutableLocationProxy();

        JPanel instructionPanel = new JPanel();
        instructionPanel.setBorder(new TitledBorder(null, "Nozzle Offset Wizard Steps", TitledBorder.LEADING, TitledBorder.TOP, null, null));

        instructionPanel.setLayout(new FormLayout(
                new ColumnSpec[] {
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                },

                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC
                })
        );

        JLabel introductionLabel = new JLabel("The Semi Automatic Nozzle Offset Wizard will help you to measure the offset between your head and this nozzle.");
        instructionPanel.add(introductionLabel, "2, 2, fill, default");

        JLabel step1Label = new JLabel("1. Select the head camera which you will use to set the offset.");
        instructionPanel.add(step1Label, "2, 4");

        // We need to know through which camera we are doing the wizard, only relevant if there is more than one
        camerasComboBox = new JComboBox<>();

        List<Camera> cameraList = nozzle.getHead().getCameras();

        for (Camera camera : cameraList) {
            camerasComboBox.addItem(new CameraItem(camera));
        }

        if(cameraList.size() == 1) {
            camerasComboBox.setSelectedIndex(0);
        }

        instructionPanel.add(camerasComboBox, "2, 6");

        JLabel step2Label = new JLabel("2. Place an object on the table where the nozzle tip can leave a mark. Putty or carbon paper is a good choice for this.");
        instructionPanel.add(step2Label, "2, 8");

        JLabel step3Label = new JLabel("3. Move the nozzle tip over the object.");
        instructionPanel.add(step3Label, "2, 10");

        JLabel step4Label = new JLabel("4. Lower the nozzle tip (jog panel) until the tip leaves a mark on the object and press \"Store nozzle mark position\".");
        instructionPanel.add(step4Label, "2, 12");

        JPanel nozzleMarkPositionPanel = new JPanel();
        nozzleMarkPositionPanel.setBorder(new TitledBorder(null, "Nozzle mark position", TitledBorder.LEADING, TitledBorder.TOP, null, null));

        nozzleMarkPositionPanel.setLayout(new FormLayout(
                new ColumnSpec[] {
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                },

                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                })
        );

        JLabel nozzleMarkLocationXLabel = new JLabel("X");
        nozzleMarkPositionPanel.add(nozzleMarkLocationXLabel, "2, 2");

        JLabel nozzleMarkLocationYLabel = new JLabel("Y");
        nozzleMarkPositionPanel.add(nozzleMarkLocationYLabel, "4, 2");

        JLabel nozzleMarkLocationZLabel = new JLabel("Z");
        nozzleMarkPositionPanel.add(nozzleMarkLocationZLabel, "6, 2");

        nozzleMarkLocationX = new JTextField();
        nozzleMarkPositionPanel.add(nozzleMarkLocationX, "2, 4");
        nozzleMarkLocationX.setColumns(5);

        nozzleMarkLocationY = new JTextField();
        nozzleMarkPositionPanel.add(nozzleMarkLocationY, "4, 4");
        nozzleMarkLocationY.setColumns(5);

        nozzleMarkLocationZ = new JTextField();
        nozzleMarkPositionPanel.add(nozzleMarkLocationZ, "6, 4");
        nozzleMarkLocationZ.setColumns(5);

        instructionPanel.add(nozzleMarkPositionPanel, "2, 14");

        JButton measureButton = new JButton("Store position");
        measureButton.setAction(storePositionAction);
        instructionPanel.add(measureButton, "2, 16");

        JLabel step5Label = new JLabel("5. Jog the camera over the center of the mark. Press \"Calculate nozzle offset\" when finished.");
        instructionPanel.add(step5Label, "2, 18");

        JButton applyNozzleOffsetButton = new JButton("Calculate nozzle offset");
        applyNozzleOffsetButton.setAction(applyNozzleOffsetAction);
        instructionPanel.add(applyNozzleOffsetButton, "2, 20");

        JPanel nozzleOffsetPanel = new JPanel();
        nozzleOffsetPanel.setBorder(new TitledBorder(null, "Nozzle head offsets", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        nozzleOffsetPanel.setLayout(new FormLayout(
                new ColumnSpec[] {
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                },

                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                })
        );

        JLabel nozzleOffsetXLabel = new JLabel("X");
        nozzleOffsetPanel.add(nozzleOffsetXLabel, "2, 2");

        JLabel nozzleOffsetYLabel = new JLabel("Y");
        nozzleOffsetPanel.add(nozzleOffsetYLabel, "4, 2");

        JLabel nozzleOffsetZLabel = new JLabel("Z");
        nozzleOffsetPanel.add(nozzleOffsetZLabel, "6, 2");

        nozzleOffsetLocationX = new JTextField();
        nozzleOffsetPanel.add(nozzleOffsetLocationX, "2, 4");
        nozzleOffsetLocationX.setColumns(5);

        nozzleOffsetLocationY = new JTextField();
        nozzleOffsetPanel.add(nozzleOffsetLocationY, "4, 4");
        nozzleOffsetLocationY.setColumns(5);

        nozzleOffsetLocationZ = new JTextField();
        nozzleOffsetPanel.add(nozzleOffsetLocationZ, "6, 4");
        nozzleOffsetLocationZ.setColumns(5);

        instructionPanel.add(nozzleOffsetPanel, "2, 22");

        JLabel step6Label = new JLabel("6. Confirm the new settings by toggling the position camera / nozzle buttons from the Jog Panel.");
        instructionPanel.add(step6Label, "2, 24");

        JLabel step7Label = new JLabel("7. Apply the new nozzle offsets by pressing the \"Apply\" button");
        instructionPanel.add(step7Label, "2, 26");

        contentPanel.add(instructionPanel);
    }

    private Action storePositionAction = new AbstractAction("Store nozzle mark position") {
        @Override
        public void actionPerformed(ActionEvent e) {
            nozzleMarkLocation.setLocation(nozzle.getLocation());;
        }
    };

    private Action applyNozzleOffsetAction = new AbstractAction("Calculate nozzle offset") {
        @Override
        public void actionPerformed(ActionEvent e) {
            CameraItem selectedCameraItem = (CameraItem) camerasComboBox.getSelectedItem();

            if(selectedCameraItem != null) {
                Camera currentCamera = selectedCameraItem.getCamera();
                Location cameraLocation = currentCamera.getLocation();
                Location headOffsets = cameraLocation.subtract(nozzleMarkLocation.getLocation());

                nozzleOffsetLocation.setLocation(headOffsets);
                Logger.info("Nozzle offset wizard set head offset to location: " + headOffsets.toString());
            }
        }
    };

    @Override
    public void createBindings() {
        LengthConverter lengthConverter = new LengthConverter();

        addWrappedBinding(nozzleMarkLocation, "lengthX", nozzleMarkLocationX, "text", lengthConverter);
        addWrappedBinding(nozzleMarkLocation, "lengthY", nozzleMarkLocationY, "text", lengthConverter);
        addWrappedBinding(nozzleMarkLocation, "lengthZ", nozzleMarkLocationZ, "text", lengthConverter);

        bind(AutoBinding.UpdateStrategy.READ_WRITE, nozzle, "headOffsets", nozzleOffsetLocation, "location");
        addWrappedBinding(nozzleOffsetLocation, "lengthX", nozzleOffsetLocationX, "text", lengthConverter);
        addWrappedBinding(nozzleOffsetLocation, "lengthY", nozzleOffsetLocationY, "text", lengthConverter);
        addWrappedBinding(nozzleOffsetLocation, "lengthZ", nozzleOffsetLocationZ, "text", lengthConverter);
    }
}
