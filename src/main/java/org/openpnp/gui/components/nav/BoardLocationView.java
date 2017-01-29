package org.openpnp.gui.components.nav;

import java.util.HashMap;

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
import javafx.scene.Node;
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

        // We need to control the order that the translate and rotate are done
        // to match how OpenPnP expects it, so instead of setting translate and
        // rotate properties we add distinct transforms.
        getTransforms().add(translate = new Translate());
        getTransforms().add(rotate = new Rotate());

        UiUtils.bindTooltip(board, new Tooltip(boardLocation.getBoard().getName()));

        setOnMouseClicked(event -> {
            Configuration.get().getBus().post(new BoardLocationSelectedEvent(boardLocation, BoardLocationView.this));
        });

        Configuration.get().getBus().register(this);

        // TODO: Properties: side, enabled
        // TODO: Lists: placements
        boardLocation.addPropertyChangeListener("location", event -> updateLocation());
        boardLocation.getBoard().addPropertyChangeListener("dimensions",
                event -> updateBoardBounds());
        boardLocation.getBoard().addPropertyChangeListener("placements", e -> updatePlacements());

        updatePlacements();
        updateBoardBounds();
        updateLocation();
    }

    private void updatePlacements() {
        Platform.runLater(() -> {
            HashMap<Placement, PlacementView> views = new HashMap<>();
            for (Node node : placements.getChildren()) {
                PlacementView view = (PlacementView) node;
                views.put(view.placement, view);
            }

            // Add any missing children
            for (Placement placement : boardLocation.getBoard().getPlacements()) {
                if (placement.getSide() != boardLocation.getSide()) {
                    continue;
                }
                if (!views.containsKey(placement)) {
                    PlacementView placementView = new PlacementView(boardLocation, placement);
                    placements.getChildren().add(placementView);
                    placementView.translateXProperty()
                            .addListener((observable, oldValue, newValue) -> updateBoardBounds());
                    placementView.translateYProperty()
                            .addListener((observable, oldValue, newValue) -> updateBoardBounds());
                    placementView.rotateProperty()
                            .addListener((observable, oldValue, newValue) -> updateBoardBounds());
                }
            }

            // Remove each child that is no longer in the job
            for (Placement placement : views.keySet()) {
                if (!boardLocation.getBoard().getPlacements().contains(placement)) {
                    placements.getChildren().remove(views.get(placement));
                }
            }

            updateBoardBounds();
        });
    }


    // TODO BUG: Doesn't handle case where there is a placement at -,- and board size is set. Leaves
    // the placement hanging in space.
    private void updateBoardBounds() {
        Platform.runLater(() -> {
            Bounds bounds = placements.getLayoutBounds();
            Location dimensions =
                    boardLocation.getBoard().getDimensions().convertToUnits(LengthUnit.Millimeters);
            double x = 0, y = 0;
            double width = dimensions.getX();
            double height = dimensions.getY();
            if (width <= 0 || width < bounds.getWidth()) {
                width = bounds.getWidth();
                // If nothing has provided us a valid width we use a default of 10 so that
                // the board is visible on screen.
                if (width <= 0) {
                    width = 10;
                }
                x = bounds.getMinX();
            }
            if (height <= 0 || height < bounds.getHeight()) {
                height = bounds.getHeight();
                // If nothing has provided us a valid height we use a default of 10 so that
                // the board is visible on screen.
                if (height <= 0) {
                    height = 10;
                }
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
            Location location = boardLocation.getLocation().convertToUnits(LengthUnit.Millimeters);
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
