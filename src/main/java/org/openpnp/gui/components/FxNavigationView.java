package org.openpnp.gui.components;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.JobProcessorListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.util.UiUtils;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

@SuppressWarnings("serial")
public class FxNavigationView extends JFXPanel {
    Location machineExtentsBottomLeft = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
    Location machineExtentsTopRight = new Location(LengthUnit.Millimeters, 300, 300, 0, 0);

    Map<Camera, CameraImageView> cameraImageViews = new HashMap<>();

    Scene scene;
    Pane root;
    Group machine;
    Group bed;
    Group boards;
    Line jogTargetLine;

    Scale zoomTx = new Scale(1, 1, 0, 0);
    Translate viewTx = new Translate(100, 100);

    public FxNavigationView() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                setScene(createScene());
                Configuration.get().addListener(configurationListener);
            }
        });
    }

    private Scene createScene() {
        root = new Pane();
        // Flip Y so the coordinate system is that of OpenPnP
        root.setScaleY(-1);
        scene = new Scene(root, Color.BLACK);

        machine = new Group();
        machine.getTransforms().add(zoomTx);
        machine.getTransforms().add(viewTx);
        root.getChildren().add(machine);

        bed = new Group();
        Rectangle bedRect =
                new Rectangle(machineExtentsBottomLeft.getX(), machineExtentsBottomLeft.getY(),
                        machineExtentsTopRight.getX(), machineExtentsTopRight.getY());
        bedRect.setFill(Color.rgb(97, 98, 100));
        bed.getChildren().add(bedRect);
        machine.getChildren().add(bed);

        boards = new Group();
        bed.getChildren().add(boards);

        scene.setOnScroll(zoomHandler);
        scene.setOnDragDetected(jogDragStartHandler);
        scene.setOnMouseDragged(jogDragHandler);
        scene.setOnMouseDragReleased(jogDragEndHandler);
        return scene;
    }

    Camera getCamera() {
        return Configuration.get().getMachine().getHeads().get(0).getCameras().get(0);
    }

    EventHandler<MouseEvent> jogDragStartHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent e) {
            scene.startFullDrag();
            Camera camera = getCamera();
            Location location = camera.getLocation().convertToUnits(LengthUnit.Millimeters);
            Point2D start = machine.localToScene(location.getX(), location.getY());
            start = root.sceneToLocal(start);
            Point2D end = root.sceneToLocal(e.getX(), e.getY());
            jogTargetLine = new Line(start.getX(), start.getY(), end.getX(), end.getY());
            jogTargetLine.setStroke(Color.WHITE);
            root.getChildren().add(jogTargetLine);
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
            root.getChildren().remove(jogTargetLine);
            final Camera camera = getCamera();
            Point2D point = machine.sceneToLocal(e.getX(), e.getY());
            final Location location =
                    camera.getLocation().derive(point.getX(), point.getY(), null, null);
            UiUtils.submitUiMachineTask(() -> {
                camera.moveTo(location, 1.0);
            });
        }
    };

    EventHandler<ScrollEvent> zoomHandler = new EventHandler<ScrollEvent>() {
        @Override
        public void handle(final ScrollEvent e) {
            e.consume();
            Point2D before = machine.sceneToLocal(e.getX(), e.getY());
            double scale = zoomTx.getX();
            scale += (e.getDeltaY() * scale * 0.001);
            scale = Math.max(scale, 0.1);
            zoomTx.setX(scale);
            zoomTx.setY(scale);
            Point2D after = machine.sceneToLocal(e.getX(), e.getY());
            Point2D delta = after.subtract(before);
            viewTx.setX(viewTx.getX() + delta.getX());
            viewTx.setY(viewTx.getY() + delta.getY());
        }
    };

    JobProcessorListener jobProcessorListener = new JobProcessorListener.Adapter() {
        @Override
        public void jobLoaded(final Job job) {
            Platform.runLater(new Runnable() {
                public void run() {
                    boards.getChildren().clear();
                    for (BoardLocation boardLocation : job.getBoardLocations()) {
                        Location location =
                                boardLocation.getLocation().convertToUnits(LengthUnit.Millimeters);
                        Group board = new Group();
                        board.getChildren().add(new Rectangle(80, 50, Color.GREEN));
                        board.setTranslateX(location.getX());
                        board.setTranslateY(location.getY());
                        boards.getChildren().add(board);
                    }
                }
            });
        }
    };

    ConfigurationListener configurationListener = new ConfigurationListener.Adapter() {
        @Override
        public void configurationComplete(Configuration configuration) throws Exception {
            final Machine machine = configuration.getMachine();
            machine.addListener(machineListener);
            // TODO: This doesn't really work in the new JobProcessor world
            // because the JobProcessor gets swapped out when changing tabs.
            // Need to figure out how to reference the current one and
            // maintain listeners across switches.
            for (JobProcessor jobProcessor : machine.getJobProcessors().values()) {
                jobProcessor.addListener(jobProcessorListener);
            }
            Platform.runLater(new Runnable() {
                public void run() {
                    for (Camera camera : machine.getCameras()) {
                        CameraImageView view = new CameraImageView(camera);
                        cameraImageViews.put(camera, view);
                        FxNavigationView.this.machine.getChildren().add(view);
                        updateCameraLocation(camera);
                    }
                    for (Head head : machine.getHeads()) {
                        for (Camera camera : head.getCameras()) {
                            CameraImageView view = new CameraImageView(camera);
                            cameraImageViews.put(camera, view);
                            FxNavigationView.this.machine.getChildren().add(view);
                            updateCameraLocation(camera);
                        }
                    }
                }
            });
        }
    };

    MachineListener machineListener = new MachineListener.Adapter() {
        @Override
        public void machineHeadActivity(Machine machine, Head head) {
            // Reposition anything that might have moved.
            Platform.runLater(() -> {
                for (Camera camera : head.getCameras()) {
                    updateCameraLocation(camera);
                }
            });
        }

        @Override
        public void machineEnabled(Machine machine) {
            Platform.runLater(() -> {
                for (Camera camera : machine.getCameras()) {
                    updateCameraLocation(camera);
                }
                for (Head head : machine.getHeads()) {
                    for (Camera camera : head.getCameras()) {
                        updateCameraLocation(camera);
                    }
                }
            });
        }
    };
    
    private void updateCameraLocation(Camera camera) {
        Location location =
                camera.getLocation().convertToUnits(LengthUnit.Millimeters);
        CameraImageView view = cameraImageViews.get(camera);
        view.setX(location.getX());
        view.setY(location.getY());
    }

    class CameraImageView extends ImageView implements CameraListener {
        final Camera camera;

        public CameraImageView(Camera camera) {
            this.camera = camera;
            Location unitsPerPixel =
                    camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
            double width = unitsPerPixel.getX() * camera.getWidth();
            double height = unitsPerPixel.getY() * camera.getHeight();
            setFitWidth(width);
            setFitHeight(height);
            // Images are flipped with respect to display coordinates, so
            // flip em back.
            setScaleY(-1);
            setTranslateX(-width / 2);
            setTranslateY(-height / 2);
            setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    setOpacity(getOpacity() == 1 ? 0.25 : 1);
                }
            });
            camera.startContinuousCapture(this, 10);
        }

        public void frameReceived(BufferedImage img) {
            setImage(SwingFXUtils.toFXImage(img, null));
        }
    }
}
