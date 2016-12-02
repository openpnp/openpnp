package org.openpnp.gui.components.nav;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import org.openpnp.ConfigurationListener;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.UiUtils;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

@SuppressWarnings("serial")
// TODO: All of the *Views need bindings so they can just keep themselves updated. This thing
// needs to be reactive.
public class FxNavigationView extends JFXPanel {
    public static FxNavigationView instance;

    Scene scene;
    Pane root;
    MachineView machineView;
    Group boards = new Group();
    Line jogTargetLine;

    Scale zoomTx = new Scale(1, 1, 0, 0);
    Translate viewTx = new Translate(0, 0);

    public FxNavigationView() {
        instance = this;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                setScene(createScene());
                Configuration.get().addListener(configurationListener);
            }
        });
        addComponentListener(componentListener);
    }

    private Scene createScene() {
        root = new Pane();
        // Flip Y so the coordinate system is that of OpenPnP
        root.setScaleY(-1);
        scene = new Scene(root, Color.ALICEBLUE);

        scene.setOnScroll(zoomHandler);
        scene.setOnDragDetected(jogDragStartHandler);
        scene.setOnMouseDragged(jogDragHandler);
        scene.setOnMouseDragReleased(jogDragEndHandler);

        return scene;
    }

    private void fitToViewPort() {
        double zoom = getMinimumZoom();
        zoomTx.setX(zoom);
        zoomTx.setY(zoom);
        viewTx.setX(0);
        viewTx.setY(0);
    }

    private double getMinimumZoom() {
        if (machineView == null) {
            return 1;
        }
        double viewWidth = getWidth() - getInsets().left - getInsets().right;
        double viewHeight = getHeight() - getInsets().top - getInsets().bottom;
        double width = machineView.getBoundsInLocal().getWidth();
        double height = machineView.getBoundsInLocal().getHeight();

        double widthRatio = width / viewWidth;
        double heightRatio = height / viewHeight;

        double scaledHeight, scaledWidth;

        if (heightRatio > widthRatio) {
            double aspectRatio = width / height;
            scaledHeight = (int) viewHeight;
            scaledWidth = (int) (scaledHeight * aspectRatio);
        }
        else {
            double aspectRatio = height / width;
            scaledWidth = (int) viewWidth;
            scaledHeight = (int) (scaledWidth * aspectRatio);
        }

        double minimumZoom = scaledWidth / width;

        return minimumZoom;
    }

    public void jobLoaded(final Job job) {
        Platform.runLater(() -> {
            boards.getChildren().clear();
            for (BoardLocation boardLocation : job.getBoardLocations()) {
                BoardLocationView boardLocationNode = new BoardLocationView(boardLocation);

                // TODO: Board bottom is wrong
                Location location =
                        boardLocation.getLocation().convertToUnits(LengthUnit.Millimeters);
                boardLocationNode.getTransforms()
                        .add(new Translate(location.getX(), location.getY()));
                boardLocationNode.getTransforms().add(new Rotate(location.getRotation()));

                boards.getChildren().add(boardLocationNode);
            }
            fitToViewPort();
        });
    }

    EventHandler<MouseEvent> jogDragStartHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent e) {
            scene.startFullDrag();
            try {
                Camera camera =
                        Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
                Location location = camera.getLocation().convertToUnits(LengthUnit.Millimeters);
                Point2D start = machineView.localToScene(location.getX(), location.getY());
                start = root.sceneToLocal(start);
                Point2D end = root.sceneToLocal(e.getX(), e.getY());
                jogTargetLine = new Line(start.getX(), start.getY(), end.getX(), end.getY());
                jogTargetLine.setStroke(Color.WHITE);
                root.getChildren().add(jogTargetLine);
            }
            catch (Exception ex) {

            }
        }
    };

    EventHandler<MouseEvent> jogDragHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent e) {
            if (jogTargetLine == null) {
                return;
            }
            Point2D end = root.sceneToLocal(e.getX(), e.getY());
            jogTargetLine.setEndX(end.getX());
            jogTargetLine.setEndY(end.getY());
        }
    };

    EventHandler<MouseEvent> jogDragEndHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent e) {
            try {
                root.getChildren().remove(jogTargetLine);
                final Camera camera =
                        Configuration.get().getMachine().getDefaultHead().getDefaultCamera();
                Point2D point = machineView.sceneToLocal(e.getX(), e.getY());
                final Location location =
                        camera.getLocation().derive(point.getX(), point.getY(), null, null);
                UiUtils.submitUiMachineTask(() -> {
                    camera.moveTo(location);
                });
            }
            catch (Exception ex) {

            }
        }
    };

    EventHandler<ScrollEvent> zoomHandler = new EventHandler<ScrollEvent>() {
        @Override
        public void handle(final ScrollEvent e) {
            e.consume();
            Point2D before = machineView.sceneToLocal(e.getX(), e.getY());
            double zoom = zoomTx.getX();
            zoom += (e.getDeltaY() * 0.01);
            if (zoom <= getMinimumZoom()) {
                fitToViewPort();
            }
            else {
                zoomTx.setX(zoom);
                zoomTx.setY(zoom);
                Point2D after = machineView.sceneToLocal(e.getX(), e.getY());
                Point2D delta = after.subtract(before);
                viewTx.setX(viewTx.getX() + delta.getX());
                viewTx.setY(viewTx.getY() + delta.getY());

            }
        }
    };

    ComponentListener componentListener = new ComponentListener() {
        @Override
        public void componentShown(ComponentEvent e) {
            fitToViewPort();
        }

        @Override
        public void componentResized(ComponentEvent e) {}

        @Override
        public void componentMoved(ComponentEvent e) {}

        @Override
        public void componentHidden(ComponentEvent e) {}
    };

    ConfigurationListener configurationListener = new ConfigurationListener.Adapter() {
        @Override
        public void configurationComplete(Configuration configuration) throws Exception {
            Platform.runLater(() -> {
                machineView = new MachineView(configuration.getMachine());
                machineView.getTransforms().add(zoomTx);
                machineView.getTransforms().add(viewTx);
                root.getChildren().add(machineView);

                machineView.getChildren().add(0, boards);

                fitToViewPort();
            });
        }
    };
}
