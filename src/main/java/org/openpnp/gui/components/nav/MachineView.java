package org.openpnp.gui.components.nav;

import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;

import javafx.scene.Group;

public class MachineView extends Group {
    public MachineView(Machine machine) {
        for (Feeder feeder : machine.getFeeders()) {
            FeederView feederView = new FeederView(feeder);
            getChildren().add(feederView);
        }
        for (Camera camera : machine.getCameras()) {
            CameraImageView view = new CameraImageView(camera);
            getChildren().add(view);
        }
        for (Head head : machine.getHeads()) {
            for (Camera camera : head.getCameras()) {
                CameraImageView view = new CameraImageView(camera);
                getChildren().add(view);
            }
            for (Nozzle nozzle : head.getNozzles()) {
                NozzleView view = new NozzleView(nozzle);
                getChildren().add(view);
            }
        }
    }
}
