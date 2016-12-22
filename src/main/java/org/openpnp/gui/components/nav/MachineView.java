package org.openpnp.gui.components.nav;

import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;

import javafx.scene.Group;

public class MachineView extends Group {
    JobView jobView;
    
    public MachineView(Machine machine) {
        getChildren().add(jobView = new JobView());
        
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
        
        Configuration.get().getBus().register(this);
        // TODO: Handle new feeeders, cameras, nozzles, etc. Everything from above, basically.
    }
    
    public JobView getJobView() {
        return jobView;
    }
}
