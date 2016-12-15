package org.openpnp.gui.components.nav;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.util.UiUtils;

import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class BoardLocationView extends Group {
    final BoardLocation boardLocation;
    
    Translate translate;
    Rotate rotate;
    
    public BoardLocationView(BoardLocation boardLocation) {
        this.boardLocation = boardLocation;
        
        // First populate all the placements so that we can determine the bounds of the
        // board if it's not specified.
        for (Placement placement : boardLocation.getBoard().getPlacements()) {
            if (placement.getSide() != boardLocation.getSide()) {
                continue;
            }
            PlacementView placementView = new PlacementView(placement);
            getChildren().add(placementView);
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
        
        UiUtils.bindTooltip(board, new Tooltip(boardLocation.getBoard().getName()));
        
        // We need to control the order that the translate and rotate are done
        // to match how OpenPnP expects it, so instead of setting translate and
        // rotate properties we add distinct transforms.
        getTransforms().add(translate = new Translate());
        getTransforms().add(rotate = new Rotate());
        
        // TODO: Properties: width, height, side, enabled
        boardLocation.addPropertyChangeListener("location", event -> updateLocation());
        
        updateLocation();
    }
    
    private void updateLocation() {
        // TODO: Board bottom is wrong
        Location location =
                boardLocation.getLocation().convertToUnits(LengthUnit.Millimeters);
        translate.setX(location.getX());
        translate.setY(location.getY());
        rotate.setAngle(location.getRotation());
    }
}
