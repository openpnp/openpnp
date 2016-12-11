package org.openpnp.gui.components.nav;

import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

public class FootprintView extends Group {
    public FootprintView(Footprint footprint, Paint fill) {
        if (footprint == null || (footprint.getPads().size() == 0 && (footprint.getBodyWidth() == 0 || footprint.getBodyHeight() == 0))) {
            Rectangle r = new Rectangle(2, 2, fill);
            r.setTranslateX(-r.getBoundsInLocal().getWidth() / 2);
            r.setTranslateY(-r.getBoundsInLocal().getHeight() / 2);
            getChildren().add(r);
        }
        else {
            if (footprint.getBodyWidth() > 0 && footprint.getBodyHeight() > 0) {
                double bodyWidth = new Length(footprint.getBodyWidth(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters).getValue();
                double bodyHeight = new Length(footprint.getBodyHeight(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters).getValue();
                Rectangle r = new Rectangle(bodyWidth, bodyHeight);
                r.setFill(null);
                r.setStroke(Color.WHITE);
                r.setStrokeWidth(0.2);
                r.setTranslateX(-r.getWidth() / 2);
                r.setTranslateY(-r.getHeight() / 2);
                getChildren().add(r);
            }
            for (Footprint.Pad pad : footprint.getPads()) {
                Length width = new Length(pad.getWidth(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters);
                Length height = new Length(pad.getWidth(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters);
                Length x = new Length(pad.getX(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters);
                Length y = new Length(pad.getY(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters);
                Rectangle r = new Rectangle(width.getValue(), height.getValue());
                r.setFill(fill);
                r.setTranslateX(x.getValue() - r.getWidth() / 2);
                r.setTranslateY(y.getValue() - r.getHeight() / 2);
                r.setRotate(pad.getRotation());
                r.setArcWidth(pad.getRoundness() * width.getValue());
                r.setArcHeight(pad.getRoundness() * height.getValue());
                getChildren().add(r);
            }
        }
    }
}
