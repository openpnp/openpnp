package org.openpnp.gui.components.nav;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Feeder;
import org.openpnp.util.UiUtils;

import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class FeederView extends Group {
    final Feeder feeder;
    
    public FeederView(Feeder feeder) {
        this.feeder = feeder;
        
        Rectangle outline = new Rectangle(8, 8);
        outline.setFill(Color.SLATEGRAY);
        outline.setTranslateX(-outline.getWidth() / 2);
        outline.setTranslateY(-outline.getHeight() / 2);
        getChildren().add(outline);
        
        FootprintView footprintNode = new FootprintView(feeder.getPart().getPackage().getFootprint(), Color.BLACK);
        getChildren().add(footprintNode);
        UiUtils.bindTooltip(this, new Tooltip(feeder.getName()));
        
        updateLocation();
    }
    
    private void updateLocation() {
        try {
            Location l = feeder.getPickLocation().convertToUnits(LengthUnit.Millimeters);
            setTranslateX(l.getX());
            setTranslateY(l.getY());
            setRotate(l.getRotation());
        }
        catch (Exception e) {
            
        }
    }
}
