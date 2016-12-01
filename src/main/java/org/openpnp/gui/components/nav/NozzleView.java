package org.openpnp.gui.components.nav;

import org.openpnp.spi.Nozzle;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class NozzleView extends Group {
    public NozzleView(Nozzle nozzle) {
        Circle c = new Circle(1, Color.RED);
        getChildren().add(c);
    }
}
