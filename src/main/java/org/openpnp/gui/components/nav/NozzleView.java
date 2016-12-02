package org.openpnp.gui.components.nav;

import org.openpnp.spi.Nozzle;
import org.openpnp.util.UiUtils;

import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class NozzleView extends Group {
    public NozzleView(Nozzle nozzle) {
        Circle c = new Circle(1, Color.RED);
        getChildren().add(c);
        UiUtils.bindTooltip(this, new Tooltip(nozzle.getName()));
    }
}
