package org.openpnp.gui.components.nav;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.event.ChangeListener;

import org.openpnp.ConfigurationListener;
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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

@SuppressWarnings("serial")
public class FxNavigationView extends JFXPanel {
    Scene scene;
    Pane root;
    MachineView machineView;
    Line jogTargetLine;

    Scale zoomTx = new Scale(1, 1, 0, 0);
    Translate viewTx = new Translate(0, 0);

    public FxNavigationView() {
        Platform.runLater(() -> {
            setScene(createScene());
            Configuration.get().addListener(configurationListener);
            addComponentListener(componentListener);
            Configuration.get().getBus().register(this);
        });
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
        
//        root.setOnMouseClicked(e -> {
//            if (e.getClickCount() == 2) {
//                zoomToFit((Node) e.getTarget());
//            }
//        });
        
        return scene;
    }
    
    private void zoomToFit() {
        zoomToFit(machineView);
    }

    private void zoomToFit(Node node) {
        if (node == null) {
            return;
        }
        Platform.runLater(() -> {
            double zoom = getMinimumZoom(node);
            zoomTx.setX(zoom);
            zoomTx.setY(zoom);
            viewTx.setX(-node.getBoundsInLocal().getMinX());
            viewTx.setY(-node.getBoundsInLocal().getMinY());
        });
    }
    
    private double getMinimumZoom() {
        return getMinimumZoom(machineView);
    }

    /**
     * Returns the minimum zoom level that will allow the Node to fit within the bounds
     * of the view.
     * @return
     */
    private double getMinimumZoom(Node node) {
        if (machineView == null) {
            return 1;
        }
        double viewWidth = getWidth() - getInsets().left - getInsets().right;
        double viewHeight = getHeight() - getInsets().top - getInsets().bottom;
        double width = node.getBoundsInLocal().getWidth();
        double height = node.getBoundsInLocal().getHeight();

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

    EventHandler<MouseEvent> jogDragStartHandler = e -> {
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
    };

    EventHandler<MouseEvent> jogDragHandler = e -> {
        if (jogTargetLine == null) {
            return;
        }
        Point2D end = root.sceneToLocal(e.getX(), e.getY());
        jogTargetLine.setEndX(end.getX());
        jogTargetLine.setEndY(end.getY());
    };

    EventHandler<MouseEvent> jogDragEndHandler = e -> {
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
    };

    EventHandler<ScrollEvent> zoomHandler = e -> {
        e.consume();
        Point2D before = machineView.sceneToLocal(e.getX(), e.getY());
        double zoom = zoomTx.getX();
        zoom += (e.getDeltaY() * 0.01);
        if (zoom <= getMinimumZoom()) {
            zoomToFit();
        }
        else {
            zoomTx.setX(zoom);
            zoomTx.setY(zoom);
            Point2D after = machineView.sceneToLocal(e.getX(), e.getY());
            Point2D delta = after.subtract(before);
            viewTx.setX(viewTx.getX() + delta.getX());
            viewTx.setY(viewTx.getY() + delta.getY());
        }
    };

    ComponentListener componentListener = new ComponentListener() {
        @Override
        public void componentShown(ComponentEvent e) {
            zoomToFit();
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
 
                // When a new job is loaded we zoomToFit() so the user sees the entire job. This
                // method of doing this is not ideal - I would rather have not exposed the job
                // property. Ideally we could just have a listener for job loaded, but since
                // Guava doesn't guarantee listener order we can't be sure the JobView will be
                // finished adding components.
                machineView.getJobView().jobProperty().addListener((observable, oldValue, newValue) -> {
                    zoomToFit();
                });
                
                zoomToFit();
            });
        }
    };
}
