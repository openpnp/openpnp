package org.openpnp.gui.components.nav;

import org.openpnp.events.PlacementSelectedEvent;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.util.UiUtils;

import com.google.common.eventbus.Subscribe;

import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PlacementView extends Group {
    final BoardLocation boardLocation;
    final Placement placement;
    Rectangle outline;
    
    public PlacementView(BoardLocation boardLocation, Placement placement) {
        this.boardLocation = boardLocation;
        this.placement = placement;
        
        Footprint footprint = null;
        if (placement.getPart() != null) {
            if (placement.getPart().getPackage() != null) {
                if (placement.getPart().getPackage().getFootprint() != null) {
                    footprint = placement.getPart().getPackage().getFootprint();
                }
            }
        }
        FootprintView footprintView = new FootprintView(footprint, Color.GOLD);
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
        
        setOnMouseClicked(event -> {
            Configuration.get().getBus().post(new PlacementSelectedEvent(placement, boardLocation, PlacementView.this));
        });
        
        Configuration.get().getBus().register(this);

        // TODO: Properties: side, part
        placement.addPropertyChangeListener("location", event -> updateLocation());
        placement.addPropertyChangeListener("type", event -> updateType());
        
        updateLocation();
        updateType();
    }

    void updateLocation() {
        Platform.runLater(() -> {
            Location l = placement.getLocation().convertToUnits(LengthUnit.Millimeters);
            setTranslateX(l.getX());
            setTranslateY(l.getY());
            setRotate(l.getRotation());
        });
    }
    
    void updateType() {
        Platform.runLater(() -> {
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
        });
    }
    
    
    @Subscribe
    public void placementSelected(PlacementSelectedEvent e) {
        Platform.runLater(() -> {
            if (e.boardLocation == boardLocation && e.placement == placement) {
                setEffect(new SelectedEffect());
            }
            else {
                setEffect(null);
            }
        });
    }

}
