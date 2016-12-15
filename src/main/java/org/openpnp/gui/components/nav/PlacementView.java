package org.openpnp.gui.components.nav;

import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.util.UiUtils;

import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PlacementView extends Group {
    final Placement placement;
    Rectangle outline;
    
    public PlacementView(Placement placement) {
        this.placement = placement;
        
        FootprintView footprintView = new FootprintView(placement.getPart().getPackage().getFootprint(), Color.GOLD);
        getChildren().add(footprintView);

        // Create the outline rectangle
        double strokeWidth = 0.2d;
        outline = new Rectangle(getBoundsInParent().getWidth() + strokeWidth, getBoundsInParent().getHeight() + strokeWidth);
        outline.setFill(null);
        outline.setStrokeWidth(strokeWidth);
        outline.setTranslateX(-outline.getWidth() / 2);
        outline.setTranslateY(-outline.getHeight() / 2);
        getChildren().add(outline);
        
        UiUtils.bindTooltip(this, new Tooltip(placement.getId()));
        
        // TODO: Properties: side, part
        placement.addPropertyChangeListener("location", event -> updateLocation());
        placement.addPropertyChangeListener("type", event -> updateType());

        updateLocation();
        updateType();
    }
    
    void updateLocation() {
        Location l = placement.getLocation().convertToUnits(LengthUnit.Millimeters);
        setTranslateX(l.getX());
        setTranslateY(l.getY());
        setRotate(l.getRotation());
    }
    
    void updateType() {
        // TODO: Should probably be a green outline, or at least match the color in the table.
        if (placement.getType() == Type.Place) {
            outline.setStroke(Color.RED);
        }
        // TODO: Match color in table.
        else if (placement.getType() == Type.Fiducial) {
            outline.setStroke(Color.AQUA);
        }
        // TODO: match color in table, or will that be confusing?  
        else {
            outline.setStroke(null);
        }
    }
}
