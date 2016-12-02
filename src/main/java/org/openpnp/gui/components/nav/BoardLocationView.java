package org.openpnp.gui.components.nav;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.util.UiUtils;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

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
            FootprintView footprintView = new FootprintView(placement.getPart().getPackage().getFootprint(), Color.GOLD);
            Group group = new Group();
            group.getChildren().add(footprintView);
            
            // If the placement is to be processed, outline it.
            if (outlineColor != null) {
                double strokeWidth = 0.2d;
                Rectangle outline = new Rectangle(group.getBoundsInParent().getWidth() + strokeWidth, group.getBoundsInParent().getHeight() + strokeWidth);
                outline.setFill(null);
                outline.setStroke(outlineColor);
                outline.setStrokeWidth(strokeWidth);
                outline.setTranslateX(-outline.getWidth() / 2);
                outline.setTranslateY(-outline.getHeight() / 2);
                group.getChildren().add(outline);
            }
            
            group.setTranslateX(l.getX());
            group.setTranslateY(l.getY());
            group.setRotate(l.getRotation());
            getChildren().add(group);
            
            UiUtils.bindTooltip(group, new Tooltip(placement.getId()));

//            Text text = new Text(l.getX(), l.getY(), placement.getId());
//            text.setFont(new Font("monospace", 1));
//            text.setFill(Color.WHITE);
//            text.setScaleY(-1);
//            text.setTranslateX(footprintView.getBoundsInLocal().getWidth() / 2 + 0.1);
//            text.setTranslateY(text.getBoundsInLocal().getHeight() / 2);
//            getChildren().add(text);
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
