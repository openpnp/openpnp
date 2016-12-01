package org.openpnp.gui.components.nav;

import org.openpnp.spi.Feeder;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class FeederView extends Group {
    public FeederView(Feeder feeder) {
        Rectangle outline = new Rectangle(8, 8);
        outline.setFill(Color.SLATEGRAY);
        outline.setTranslateX(-outline.getWidth() / 2);
        outline.setTranslateY(-outline.getHeight() / 2);
        getChildren().add(outline);
        
        FootprintView footprintNode = new FootprintView(feeder.getPart().getPackage().getFootprint(), Color.BLACK);
        getChildren().add(footprintNode);
    }
}
