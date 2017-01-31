package org.openpnp.gui.components.nav;

import org.openpnp.events.FeederSelectedEvent;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Feeder;
import org.openpnp.util.UiUtils;

import com.google.common.eventbus.Subscribe;

import javafx.application.Platform;
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
        
        Footprint footprint = null;
        if (feeder.getPart() != null) {
            if (feeder.getPart().getPackage() != null) {
                if (feeder.getPart().getPackage().getFootprint() != null) {
                    footprint = feeder.getPart().getPackage().getFootprint();
                }
            }
        }

        FootprintView footprintView = new FootprintView(footprint, Color.BLACK);
        getChildren().add(footprintView);
        
        UiUtils.bindTooltip(this, new Tooltip(feeder.getName()));
        
        setOnMouseClicked(event -> {
            Configuration.get().getBus().post(new FeederSelectedEvent(feeder, FeederView.this));
        });
        
        Configuration.get().getBus().register(this);

        updateLocation();
    }
    
    private void updateLocation() {
        Platform.runLater(() -> {
            try {
                Location l = feeder.getPickLocation().convertToUnits(LengthUnit.Millimeters);
                setTranslateX(l.getX());
                setTranslateY(l.getY());
                setRotate(l.getRotation());
            }
            catch (Exception e) {
                // Not all feeders will always have a valid pick location, and there's nothing
                // we can do about it, so we just ignore the error and hope that the feeder
                // eventually gets a valid pick and location.
            }
        });
    }
    
    @Subscribe
    public void feederSelected(FeederSelectedEvent e) {
        Platform.runLater(() -> {
            if (e.feeder == feeder) {
                setEffect(new SelectedEffect());
            }
            else {
                setEffect(null);
            }
        });
    }
}
