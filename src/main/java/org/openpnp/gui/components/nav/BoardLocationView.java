package org.openpnp.gui.components.nav;

import org.openpnp.events.BoardLocationSelectedEvent;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.util.UiUtils;

import com.google.common.eventbus.Subscribe;

import javafx.application.Platform;
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
    
    Rectangle board;
    Group placements = new Group();
    
    public BoardLocationView(BoardLocation boardLocation) {
        this.boardLocation = boardLocation;
        
        board = new Rectangle();
        board.setFill(Color.GREEN);
        getChildren().add(board);
        
        getChildren().add(placements);
        
        // First populate all the placements so that we can determine the bounds of the
        // board if it's not specified.
        for (Placement placement : boardLocation.getBoard().getPlacements()) {
            if (placement.getSide() != boardLocation.getSide()) {
                continue;
            }
            PlacementView placementView = new PlacementView(boardLocation, placement);
            placements.getChildren().add(placementView);
            placementView.translateXProperty().addListener(e -> updateBoardBounds());
            placementView.translateYProperty().addListener(e -> updateBoardBounds());
            placementView.rotateProperty().addListener(e -> updateBoardBounds());
        }

        // We need to control the order that the translate and rotate are done
        // to match how OpenPnP expects it, so instead of setting translate and
        // rotate properties we add distinct transforms.
        getTransforms().add(translate = new Translate());
        getTransforms().add(rotate = new Rotate());
        
        UiUtils.bindTooltip(board, new Tooltip(boardLocation.getBoard().getName()));
        
        setOnMouseClicked(event -> {
            Configuration.get().getBus().post(new BoardLocationSelectedEvent(boardLocation));
        });

        Configuration.get().getBus().register(this);
        
        // TODO: Properties: side, enabled
        boardLocation.addPropertyChangeListener("location", event -> updateLocation());
        boardLocation.getBoard().addPropertyChangeListener("dimensions", event -> updateBoardBounds());
        
        updateBoardBounds();
        updateLocation();
    }
    
    private void updateBoardBounds() {
        Platform.runLater(() -> {
            Bounds bounds = placements.getLayoutBounds();
            Location dimensions = boardLocation.getBoard().getDimensions().convertToUnits(LengthUnit.Millimeters);
            double x = 0, y = 0;
            double width = dimensions.getX();
            double height = dimensions.getY();
            if (width < bounds.getWidth()) {
                width = bounds.getWidth();
                x = bounds.getMinX();
            }
            if (height < bounds.getHeight()) {
                height = bounds.getHeight();
                y = bounds.getMinY();
            }
            board.setWidth(width);
            board.setHeight(height);
            board.setTranslateX(x);
            board.setTranslateY(y);
        });
    }
    
    private void updateLocation() {
        // TODO: Board bottom is wrong
        Platform.runLater(() -> {
            Location location =
                    boardLocation.getLocation().convertToUnits(LengthUnit.Millimeters);
            translate.setX(location.getX());
            translate.setY(location.getY());
            rotate.setAngle(location.getRotation());
        });
    }
    
    @Subscribe
    public void boardLocationSelected(BoardLocationSelectedEvent e) {
        Platform.runLater(() -> {
            if (e.boardLocation == boardLocation) {
                setEffect(new SelectedEffect());
            }
            else {
                setEffect(null);
            }
        });
    }
}
