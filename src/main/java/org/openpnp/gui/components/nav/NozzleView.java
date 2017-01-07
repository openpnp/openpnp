package org.openpnp.gui.components.nav;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.UiUtils;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class NozzleView extends Group {
    final Nozzle nozzle;
    
    public NozzleView(Nozzle nozzle) {
        this.nozzle = nozzle;
        
        Circle c = new Circle(1, Color.RED);
        getChildren().add(c);
        UiUtils.bindTooltip(this, new Tooltip(nozzle.getName()));
        
        updateLocation();
        
        nozzle.getHead().getMachine().addListener(machineListener);
    }
    
    private void updateLocation() {
        Platform.runLater(() -> {
            Location location = nozzle.getLocation().convertToUnits(LengthUnit.Millimeters);
            setTranslateX(location.getX());
            setTranslateY(location.getY());
        });
    }
    
    MachineListener machineListener = new MachineListener.Adapter() {
        @Override
        public void machineHeadActivity(Machine machine, Head head) {
            updateLocation();
        }

        @Override
        public void machineEnabled(Machine machine) {
            updateLocation();
        }
    };
}
