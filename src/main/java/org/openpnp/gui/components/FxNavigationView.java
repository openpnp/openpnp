package org.openpnp.gui.components;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.UiUtils;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

@SuppressWarnings("serial")
// TODO: Click drag jog should take into account which head item you drag from so you can
// choose to use nozzle, camera, etc.
public class FxNavigationView extends JFXPanel {
    public static FxNavigationView instance;
    
    // TODO: Don't add anymore specifics here, make a Head Group instead.
    Map<Camera, CameraImageView> cameraImageViews = new HashMap<>();
    Map<Nozzle, Rectangle> nozzleRects = new HashMap<>();

    Scene scene;
    Pane root;
    Group machine;
    Group boards;
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
    }
    
    private void fitToViewPort() {
        double zoom = getMinimumZoom();
        zoomTx.setX(zoom);
        zoomTx.setY(zoom);
        viewTx.setX(0);
        viewTx.setY(0);
    }
    
    private double getMinimumZoom() {
        double viewWidth = getWidth() - getInsets().left - getInsets().right;
        double viewHeight = getHeight() - getInsets().top - getInsets().bottom;
        double width = machine.getBoundsInLocal().getWidth();
        double height = machine.getBoundsInLocal().getHeight();

        double heightRatio = height / viewHeight;
        double widthRatio = width / viewWidth;

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

        return scaledWidth / width;
    }
    
    private Scene createScene() {
        root = new Pane();
        // Flip Y so the coordinate system is that of OpenPnP
        root.setScaleY(-1);
        scene = new Scene(root, Color.ALICEBLUE);

        machine = new Group();
        machine.getTransforms().add(zoomTx);
        machine.getTransforms().add(viewTx);
        root.getChildren().add(machine);

        boards = new Group();
        
        machine.getChildren().add(boards);

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
                camera.moveTo(location);
            });
        }
    };

    EventHandler<ScrollEvent> zoomHandler = new EventHandler<ScrollEvent>() {
        @Override
        public void handle(final ScrollEvent e) {
            e.consume();
            Point2D before = machine.sceneToLocal(e.getX(), e.getY());
            double scale = zoomTx.getX();
            scale += (e.getDeltaY() * 0.01);
            scale = Math.max(scale, 0.1);
            if (scale <= getMinimumZoom()) {
                fitToViewPort();
            }
            else {
                zoomTx.setX(scale);
                zoomTx.setY(scale);
                Point2D after = machine.sceneToLocal(e.getX(), e.getY());
                Point2D delta = after.subtract(before);
                viewTx.setX(viewTx.getX() + delta.getX());
                viewTx.setY(viewTx.getY() + delta.getY());
                
            }
        }
    };

    public void jobLoaded(final Job job) {
        Platform.runLater(new Runnable() {
            public void run() {
                boards.getChildren().clear();
                for (BoardLocation boardLocation : job.getBoardLocations()) {
                    Group board = createBoardLocation(boardLocation);
                    boards.getChildren().add(board);
                }
                fitToViewPort();
            }
        });
    }
    
    private Group createBoardLocation(BoardLocation boardLocation) {
        Group board = new Group();

        // First populate all the placements so that we can determine the bounds of the
        // board if it's not specified.
        for (Placement placement : boardLocation.getBoard().getPlacements()) {
            if (placement.getSide() != boardLocation.getSide()) {
                continue;
            }
            Color fillColor = Color.GOLD;
            Color strokeColor = null;
            double strokeWidth = 0.2;
            if (placement.getType() == Type.Place) {
                strokeColor = Color.RED;
            }
            else if (placement.getType() == Type.Fiducial) {
                strokeColor = Color.AQUA;
            }
            
            Location l = placement.getLocation().convertToUnits(LengthUnit.Millimeters);
            Node footprint = createFootprint(placement.getPart().getPackage().getFootprint(), fillColor);
            Group group = new Group();
            group.getChildren().add(footprint);
            // add an outline around the footprint
            if (strokeColor != null) {
                Rectangle r = new Rectangle(group.getBoundsInLocal().getWidth(), group.getBoundsInLocal().getHeight());
                r.setFill(null);
                r.setStroke(strokeColor);
                r.setStrokeWidth(strokeWidth);
                r.setTranslateX(-r.getWidth() / 2);
                r.setTranslateY(-r.getHeight() / 2);
                group.getChildren().add(r);
            }
            
            group.setTranslateX(l.getX());
            group.setTranslateY(l.getY());
            group.setRotate(l.getRotation());
            board.getChildren().add(group);

            Text text = new Text(l.getX(), l.getY(), placement.getId());
            text.setFont(new Font("monospace", 1));
            text.setFill(Color.WHITE);
            text.setScaleY(-1);
            text.setTranslateX(footprint.getBoundsInLocal().getWidth() / 2 + 0.1);
            text.setTranslateY(text.getBoundsInLocal().getHeight() / 2);
            board.getChildren().add(text);
        }

        // Now create the board itself, using the calculated bounds if needed.
        Bounds bounds = board.getBoundsInLocal();
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
        Rectangle background = new Rectangle(width, height, Color.GREEN);
        background.setTranslateX(x);
        background.setTranslateY(y);
        board.getChildren().add(0, background);

        // TODO: Board bottom is wrong
        Location location =
                boardLocation.getLocation().convertToUnits(LengthUnit.Millimeters);
        board.getTransforms().add(new Translate(location.getX(), location.getY()));
        board.getTransforms().add(new Rotate(location.getRotation()));
        
        return board;
    }
    
    private Node createFootprint(Footprint footprint, Color fillColor) {
        if (footprint == null || (footprint.getPads().size() == 0 && (footprint.getBodyWidth() == 0 || footprint.getBodyHeight() == 0))) {
            Group group = new Group();
            Rectangle r = new Rectangle(2, 2, fillColor);
            r.setTranslateX(-r.getBoundsInLocal().getWidth() / 2);
            r.setTranslateY(-r.getBoundsInLocal().getHeight() / 2);
            group.getChildren().add(r);
            return group;
        }
        else {
            Group group = new Group();
            if (footprint.getBodyWidth() > 0 && footprint.getBodyHeight() > 0) {
                double bodyWidth = new Length(footprint.getBodyWidth(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters).getValue();
                double bodyHeight = new Length(footprint.getBodyHeight(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters).getValue();
                Rectangle r = new Rectangle(bodyWidth, bodyHeight);
                r.setFill(null);
                r.setStroke(Color.WHITE);
                r.setStrokeWidth(0.2);
                r.setTranslateX(-r.getWidth() / 2);
                r.setTranslateY(-r.getHeight() / 2);
                group.getChildren().add(r);
            }
            for (Footprint.Pad pad : footprint.getPads()) {
                Length width = new Length(pad.getWidth(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters);
                Length height = new Length(pad.getWidth(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters);
                Length x = new Length(pad.getX(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters);
                Length y = new Length(pad.getY(), footprint.getUnits()).convertToUnits(LengthUnit.Millimeters);
                Rectangle r = new Rectangle(width.getValue(), height.getValue());
                r.setFill(fillColor);
                r.setTranslateX(x.getValue() - r.getWidth() / 2);
                r.setTranslateY(y.getValue() - r.getHeight() / 2);
                r.setRotate(pad.getRotation());
                r.setArcWidth(pad.getRoundness() * width.getValue());
                r.setArcHeight(pad.getRoundness() * height.getValue());
                group.getChildren().add(r);
            }
            return group;
        }
    }
    
    ConfigurationListener configurationListener = new ConfigurationListener.Adapter() {
        @Override
        public void configurationComplete(Configuration configuration) throws Exception {
            final Machine machine = configuration.getMachine();
            machine.addListener(machineListener);
            Platform.runLater(new Runnable() {
                public void run() {
                    for (Feeder feeder : machine.getFeeders()) {
                        try {
                            Location l = feeder.getPickLocation().convertToUnits(LengthUnit.Millimeters);
                            
                            Rectangle view = new Rectangle(8, 8);
                            view.setFill(Color.SLATEGRAY);
                            view.setTranslateX(l.getX() - view.getWidth() / 2);
                            view.setTranslateY(l.getY() - view.getHeight() / 2);
                            view.setRotate(l.getRotation());
                            FxNavigationView.this.machine.getChildren().add(view);

                            Node footprint = createFootprint(feeder.getPart().getPackage().getFootprint(), Color.BLACK);
                            footprint.setTranslateX(l.getX());
                            footprint.setTranslateY(l.getY());
                            footprint.setRotate(l.getRotation());
                            FxNavigationView.this.machine.getChildren().add(footprint);
                        }
                        catch (Exception e) {
                            
                        }
                    }
                    for (Camera camera : machine.getCameras()) {
                        CameraImageView view = new CameraImageView(camera);
                        cameraImageViews.put(camera, view);
//                        FxNavigationView.this.machine.getChildren().add(view);
                        updateCameraLocation(camera);
                    }
                    for (Head head : machine.getHeads()) {
                        for (Camera camera : head.getCameras()) {
                            CameraImageView view = new CameraImageView(camera);
                            cameraImageViews.put(camera, view);
//                            FxNavigationView.this.machine.getChildren().add(view);
                            updateCameraLocation(camera);
                        }
                        for (Nozzle nozzle : head.getNozzles()) {
                            Rectangle view = new Rectangle(1, 1);
                            view.setFill(Color.RED);
                            nozzleRects.put(nozzle, view);
                            FxNavigationView.this.machine.getChildren().add(view);
                            updateNozzleLocation(nozzle);
                        }
                    }
                }
            });
        }
    };

    MachineListener machineListener = new MachineListener.Adapter() {
        @Override
        public void machineHeadActivity(Machine machine, Head head) {
            if (head == null) {
                return;
            }
            // Reposition anything that might have moved.
            Platform.runLater(() -> {
                for (Camera camera : head.getCameras()) {
                    updateCameraLocation(camera);
                }
                for (Nozzle nozzle : head.getNozzles()) {
                    updateNozzleLocation(nozzle);
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
                    for (Nozzle nozzle : head.getNozzles()) {
                        updateNozzleLocation(nozzle);
                    }
                }
            });
        }
    };

    private void updateCameraLocation(Camera camera) {
        Location location = camera.getLocation().convertToUnits(LengthUnit.Millimeters);
        CameraImageView view = cameraImageViews.get(camera);
        view.setX(location.getX());
        view.setY(location.getY());
    }

    private void updateNozzleLocation(Nozzle nozzle) {
        Location location = nozzle.getLocation().convertToUnits(LengthUnit.Millimeters);
        Rectangle view = nozzleRects.get(nozzle);
        view.setX(location.getX() - view.getWidth() / 2);
        view.setY(location.getY() - view.getHeight() / 2);
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
