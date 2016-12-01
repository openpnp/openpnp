package org.openpnp.gui.components.nav;

import java.util.HashMap;
import java.util.Map;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Node;

public class MachineView extends Group {
    Map<Camera, CameraImageView> cameras = new HashMap<>();
    Map<Nozzle, NozzleView> nozzles = new HashMap<>();
    
    public MachineView(Machine machine) {
        for (Feeder feeder : machine.getFeeders()) {
            try {
                Location l = feeder.getPickLocation().convertToUnits(LengthUnit.Millimeters);
                FeederView node = new FeederView(feeder);
                node.setTranslateX(l.getX());
                node.setTranslateY(l.getY());
                node.setRotate(l.getRotation());
                getChildren().add(node);
            }
            catch (Exception e) {
                
            }
        }
        for (Camera camera : machine.getCameras()) {
            CameraImageView view = new CameraImageView(camera);
            cameras.put(camera, view);
            getChildren().add(view);
            updateCameraLocation(camera);
        }
        for (Head head : machine.getHeads()) {
            for (Camera camera : head.getCameras()) {
                CameraImageView view = new CameraImageView(camera);
                cameras.put(camera, view);
                getChildren().add(view);
                updateCameraLocation(camera);
            }
            for (Nozzle nozzle : head.getNozzles()) {
                NozzleView view = new NozzleView(nozzle);
                nozzles.put(nozzle, view);
                getChildren().add(view);
                updateNozzleLocation(nozzle);
            }
        }
        machine.addListener(machineListener);
    }
    
    private void updateCameraLocation(Camera camera) {
        Location location = camera.getLocation().convertToUnits(LengthUnit.Millimeters);
        CameraImageView view = cameras.get(camera);
        view.setX(location.getX());
        view.setY(location.getY());
    }

    private void updateNozzleLocation(Nozzle nozzle) {
        Location location = nozzle.getLocation().convertToUnits(LengthUnit.Millimeters);
        Node view = nozzles.get(nozzle);
        view.setTranslateX(location.getX());
        view.setTranslateY(location.getY());
    }
    
    MachineListener machineListener = new MachineListener.Adapter() {
        @Override
        public void machineHeadActivity(Machine machine, Head head) {
            if (head == null) {
                return;
            }
            // Reposition anything that might have moved.
            Platform.runLater(() -> {
                for (Camera camera : head.getCameras()) {
                    updateCameraLocation(camera);
                }
                for (Nozzle nozzle : head.getNozzles()) {
                    updateNozzleLocation(nozzle);
                }
            });
        }

        @Override
        public void machineEnabled(Machine machine) {
            Platform.runLater(() -> {
                for (Camera camera : machine.getCameras()) {
                    updateCameraLocation(camera);
                }
                for (Head head : machine.getHeads()) {
                    for (Camera camera : head.getCameras()) {
                        updateCameraLocation(camera);
                    }
                    for (Nozzle nozzle : head.getNozzles()) {
                        updateNozzleLocation(nozzle);
                    }
                }
            });
        }
    };
}
