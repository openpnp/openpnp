package org.openpnp.machine.reference.psh;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.ClassSelectionDialog;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.reference.vision.OpenCvVisionProvider;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.SimplePropertySheetHolder;
import org.pmw.tinylog.Logger;

public class CamerasPropertySheetHolder extends SimplePropertySheetHolder {
    final Head head;
    
    public CamerasPropertySheetHolder(Head head, String title, List<? extends PropertySheetHolder> children,
            Icon icon) {
        super(title, children, icon);
        this.head = head;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] {newCameraAction};
    }
    
    public Action newCameraAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Camera...");
            putValue(SHORT_DESCRIPTION, "Create a new camera.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Configuration configuration = Configuration.get();
            ClassSelectionDialog<Camera> dialog = new ClassSelectionDialog<>(MainFrame.get(),
                    "Select Camera...", "Please select a Camera implemention from the list below.",
                    configuration.getMachine().getCompatibleCameraClasses());
            dialog.setVisible(true);
            Class<? extends Camera> cameraClass = dialog.getSelectedClass();
            if (cameraClass == null) {
                return;
            }
            try {
                Camera camera = cameraClass.newInstance();

                if (camera.getUnitsPerPixel() == null) {
                    camera.setUnitsPerPixel(new Location(Configuration.get().getSystemUnits()));
                }
                
                try {
                    if (camera.getVisionProvider() == null) {
                        camera.setVisionProvider(new OpenCvVisionProvider());
                    }
                }
                catch (Exception e) {
                    Logger.debug("Couldn't set default vision provider.");
                }
                
                if (head != null) {
                    head.addCamera(camera);
                }
                else {
                    configuration.getMachine().addCamera(camera);
                }

                MainFrame.get().getCameraViews().addCamera(camera);
            }
            catch (Exception e) {
                MessageBoxes.errorBox(MainFrame.get(), "Camera Error", e);
            }
        }
    };
}
