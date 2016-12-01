package org.openpnp.gui.components.nav;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class BoardLocationView extends Group {
    public BoardLocationView(BoardLocation boardLocation) {
        // First populate all the placements so that we can determine the bounds of the
        // board if it's not specified.
        for (Placement placement : boardLocation.getBoard().getPlacements()) {
            if (placement.getSide() != boardLocation.getSide()) {
                continue;
            }
            Color outlineColor = null;
            if (placement.getType() == Type.Place) {
                outlineColor = Color.RED;
            }
            else if (placement.getType() == Type.Fiducial) {
                outlineColor = Color.AQUA;
            }
            
            Location l = placement.getLocation().convertToUnits(LengthUnit.Millimeters);
            FootprintView footprintNode = new FootprintView(placement.getPart().getPackage().getFootprint(), Color.GOLD);
            Group group = new Group();
            group.getChildren().add(footprintNode);
            
            // If the placement is to be processed, outline it.
            if (outlineColor != null) {
//                DropShadow outline = new DropShadow();
//                outline.setOffsetY(0f);
//                outline.setOffsetX(0f);
//                outline.setColor(outlineColor);
//                outline.setRadius(0.2);
//                outline.setSpread(1);
//                footprintNode.setEffect(outline); 
                
                Rectangle outline = new Rectangle(group.getBoundsInLocal().getWidth(), group.getBoundsInLocal().getHeight());
                outline.setFill(null);
                outline.setStroke(outlineColor);
                outline.setStrokeWidth(0.2);
                outline.setTranslateX(-outline.getWidth() / 2);
                outline.setTranslateY(-outline.getHeight() / 2);
                group.getChildren().add(outline);
            }
            
            group.setTranslateX(l.getX());
            group.setTranslateY(l.getY());
            group.setRotate(l.getRotation());
            getChildren().add(group);

            Text text = new Text(l.getX(), l.getY(), placement.getId());
            text.setFont(new Font("monospace", 1));
            text.setFill(Color.WHITE);
            text.setScaleY(-1);
            text.setTranslateX(footprintNode.getBoundsInLocal().getWidth() / 2 + 0.1);
            text.setTranslateY(text.getBoundsInLocal().getHeight() / 2);
            getChildren().add(text);
        }

        // Now create the board itself, using the calculated bounds if needed.
        Bounds bounds = getBoundsInLocal();
        Location dimensions = boardLocation.getBoard().getDimensions().convertToUnits(LengthUnit.Millimeters);
        double x = 0, y = 0;
        double width = dimensions.getX();
        double height = dimensions.getY();
        if (width == 0) {
            width = bounds.getWidth();
            x = bounds.getMinX();
        }
        if (height == 0) {
            height = bounds.getHeight();
            y = bounds.getMinY();
        }
        Rectangle board = new Rectangle(width, height, Color.GREEN);
        board.setTranslateX(x);
        board.setTranslateY(y);
        getChildren().add(0, board);
    }
}
