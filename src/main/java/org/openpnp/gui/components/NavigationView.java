package org.openpnp.gui.components;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JComponent;

import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.JobProcessorListener;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.BoardPad;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Pad;
import org.openpnp.model.Placement;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Head;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.JobProcessor.JobError;
import org.openpnp.spi.JobProcessor.JobState;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.HslColor;
import org.openpnp.util.ImageUtils;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;

/**
 * User interaction with this component: Single click interacts with the object under the cursor.
 * Camera dims. Drag jogs camera on release.
 * 
 */
public class NavigationView extends JComponent implements JobProcessorListener, MachineListener,
        MouseWheelListener, MouseListener, KeyListener, MouseMotionListener {
    private Location machineExtentsBottomLeft = new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
    private Location machineExtentsTopRight = new Location(LengthUnit.Millimeters, 400, 400, 0, 0);

    // MUST always be in mm, if something sets it it should be converted first.
    private Location lookingAt = new Location(LengthUnit.Millimeters, 0, 0, 1, 0);

    // Determine the base scale. This is the scaling factor needed to fit
    // the entire machine in the window.
    // TODO: It would simplify things if we just calculate this once and
    // set it as Z on lookingAt during startup. There's no reason to
    // recalculate it every time since we only actually set it once.
    // double bedWidth = machineExtents.getX();
    // double bedHeight = machineExtents.getY();
    // double xScale = width / bedWidth;
    // double yScale = height / bedHeight;
    // double baseScale = Math.min(xScale, yScale);
    // double scale = baseScale * lookingAt.getZ();

    private double cameraOpacity = 1;
    private Point dragStart = null;
    private Point dragEnd = null;
    private Paint backgroundPaint = new Color(97, 98, 100);
    private Paint bedPaint = createNoisyPaint(new Color(37, 37, 37));
    private Paint boardPaint = new Color(29, 115, 25);
    private Paint padPaint = new Color(168, 139, 9);
    static BufferedImage noiseImage;

    private static Paint createNoisyPaint(Color color) {
        if (noiseImage == null) {
            try {
                noiseImage = ImageIO.read(ClassLoader.getSystemResource("noise-texture.png"));
            }
            catch (Exception e) {
                return null;
            }
        }
        int width = noiseImage.getWidth();
        int height = noiseImage.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.drawImage(noiseImage, 0, 0, null);
        g.dispose();
        return new TexturePaint(image, new Rectangle2D.Double(0, 0, width, height));
    }


    /**
     * Contains the AffineTransform that was last used to render the component. This is used
     * elsewhere to convert component coordinates back to machine coordinates.
     */
    private AffineTransform transform;

    private HashMap<Camera, BufferedImage> cameraImages = new HashMap<>();

    public NavigationView() {
        addMouseWheelListener(this);
        addMouseListener(this);
        addKeyListener(this);
        addMouseMotionListener(this);
        Configuration.get().addListener(new ConfigurationListener() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {}

            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                Machine machine = configuration.getMachine();
                machine.addListener(NavigationView.this);
                // TODO: This doesn't really work in the new JobProcessor world
                // because the JobProcessor gets swapped out when changing tabs.
                // Need to figure out how to reference the current one and
                // maintain listeners across switches.
                for (JobProcessor jobProcessor : machine.getJobProcessors().values()) {
                    jobProcessor.addListener(NavigationView.this);
                }
                for (Camera camera : machine.getCameras()) {
                    camera.startContinuousCapture(new NavCameraListener(camera), 24);
                }
                for (Head head : machine.getHeads()) {
                    for (Camera camera : head.getCameras()) {
                        camera.startContinuousCapture(new NavCameraListener(camera), 24);
                    }
                }
            }
        });
    }

    private void updateTransform() {
        AffineTransform transform = new AffineTransform();

        int width = getWidth();
        int height = getHeight();

        // Center the drawing
        transform.translate(width / 2, height / 2);

        // Scale the drawing to the zoom level
        transform.scale(lookingAt.getZ(), lookingAt.getZ());

        // Move to the lookingAt position
        transform.translate(-lookingAt.getX(), lookingAt.getY());

        // Flip the drawing in Y so that our coordinate system matches that
        // of the machine.
        transform.scale(1, -1);

        this.transform = transform;
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Create a new Graphics so we don't break the original.
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Paint the background
        g2d.setPaint(backgroundPaint);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // All rendering is done in mm, where 1mm = 1px. Any Locations that
        // are used for rendering must first be converted to mm.

        updateTransform();
        g2d.transform(transform);

        // Draw the bed
        g2d.setPaint(bedPaint);
        g2d.fillRect((int) machineExtentsBottomLeft.getX(), (int) machineExtentsBottomLeft.getY(),
                (int) (machineExtentsTopRight.getX() - machineExtentsBottomLeft.getX()),
                (int) (machineExtentsTopRight.getY() - machineExtentsBottomLeft.getY()));
        g2d.setColor(Color.black);
        g2d.drawRect((int) machineExtentsBottomLeft.getX(), (int) machineExtentsBottomLeft.getY(),
                (int) (machineExtentsTopRight.getX() - machineExtentsBottomLeft.getX()),
                (int) (machineExtentsTopRight.getY() - machineExtentsBottomLeft.getY()));


        Machine machine = Configuration.get().getMachine();
        JobProcessor jobProcessor = MainFrame.jobPanel.getJobProcessor();
        Job job = jobProcessor.getJob();
        if (job != null) {
            // Draw the boards
            for (BoardLocation boardLocation : job.getBoardLocations()) {
                Location location =
                        boardLocation.getLocation().convertToUnits(LengthUnit.Millimeters);

                AffineTransform tx = g2d.getTransform();
                // g2d.translate(location.getX(), location.getY());
                // g2d.rotate(location.getRotation());
                Board board = boardLocation.getBoard();

                Shape outline = board.getOutline().getShape();
                // TODO: Generate bounds outline if null
                if (outline != null) {
                    g2d.setPaint(boardPaint);
                    g2d.fill(outline);
                }

                // Draw the pads on the boards
                g2d.setPaint(padPaint);
                for (BoardPad boardPad : board.getSolderPastePads()) {
                    Location padLocation =
                            boardPad.getLocation().convertToUnits(LengthUnit.Millimeters);
                    Pad pad = boardPad.getPad().convertToUnits(LengthUnit.Millimeters);
                    Shape shape = pad.getShape();
                    AffineTransform shapeTx = new AffineTransform();
                    shapeTx.translate(padLocation.getX(), padLocation.getY());
                    shape = shapeTx.createTransformedShape(shape);
                    g2d.fill(shape);
                }

                // Draw the placements on the boards
                for (Placement placement : boardLocation.getBoard().getPlacements()) {
                    if (placement.getSide() != boardLocation.getSide()) {
                        continue;
                    }
                    Location placementLocation =
                            Utils2D.calculateBoardPlacementLocation(boardLocation, placement.getLocation());
                    // paintCrosshair(g2d, placementLocation, Color.orange);
                }

                g2d.setTransform(tx);
            }
        }

        // Draw the feeders
        for (Feeder feeder : machine.getFeeders()) {
            try {
                Location location = feeder.getPickLocation();
                paintCrosshair(g2d, location, Color.white);
            }
            catch (Exception e) {

            }
        }

        // Draw fixed cameras
        for (Camera camera : machine.getCameras()) {
            paintCamera(g2d, camera);
        }

        // Draw the head
        for (Head head : machine.getHeads()) {
            for (Nozzle nozzle : head.getNozzles()) {
                Location location = nozzle.getLocation();
                paintCrosshair(g2d, location, Color.red);
            }

            for (Camera camera : head.getCameras()) {
                paintCamera(g2d, camera);
            }

            for (Actuator actuator : head.getActuators()) {
                Location location = actuator.getLocation();
                paintCrosshair(g2d, location, Color.yellow);
            }
        }

        paintDragVector(g2d);

        // Dispose of the Graphics we created.
        g2d.dispose();
    }

    private void paintDragVector(Graphics2D g2d) {
        if (dragStart == null) {
            return;
        }
        Camera camera = Configuration.get().getMachine().getHeads().get(0).getCameras().get(0);
        Location start = camera.getLocation().convertToUnits(LengthUnit.Millimeters);
        Location end = getPixelLocation(dragEnd.getX(), dragEnd.getY());
        g2d.setColor(Color.yellow);
        g2d.drawLine((int) start.getX(), (int) start.getY(), (int) end.getX(), (int) end.getY());
    }

    private void paintCamera(Graphics2D g2d, Camera camera) {
        Location location = camera.getLocation();
        location = location.convertToUnits(LengthUnit.Millimeters);
        BufferedImage img = cameraImages.get(camera);
        if (img == null) {
            return;
        }

        // we need to scale the image so that 1 pixel = 1mm
        // and it needs to be centered on the location
        double width = camera.getWidth();
        double height = camera.getHeight();
        Location upp = camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
        double scaledWidth = width * upp.getX();
        double scaledHeight = height * upp.getY();

        int dx1 = (int) (location.getX() - (scaledWidth / 2));
        int dy1 = (int) (location.getY() + (scaledHeight / 2));
        int dx2 = (int) (location.getX() + (scaledWidth / 2));
        int dy2 = (int) (location.getY() - (scaledHeight / 2));

        int sx1 = 0;
        int sy1 = 0;
        int sx2 = (int) width;
        int sy2 = (int) height;

        if (cameraOpacity != 1) {
            if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                img = ImageUtils.convertBufferedImage(img, BufferedImage.TYPE_INT_ARGB);
            }
            // We're going to mess with the composite, so we create a new
            // context to draw with and dispose it when we're done.
            Graphics2D g = (Graphics2D) g2d.create();
            g.setComposite(
                    AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) cameraOpacity));
            g.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
            g.dispose();
        }
        else {
            g2d.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
        }
    }

    private void paintCrosshair(Graphics2D g2d, Location location, Color color) {
        Color color2 = new HslColor(color).getComplementary();
        location = location.convertToUnits(LengthUnit.Millimeters);
        int x = (int) location.getX();
        int y = (int) location.getY();
        g2d.setColor(color);
        g2d.drawLine(x - 3, y, x + 3, y);
        g2d.drawLine(x, y - 3, x, y);
        g2d.setColor(color2);
        g2d.drawLine(x, y, x, y + 3);
    }

    private Location getPixelLocation(double x, double y) {
        Point2D point = new Point2D.Double(x, y);
        try {
            transform.inverseTransform(point, point);
        }
        catch (Exception e) {
        }
        return lookingAt.derive(point.getX(), point.getY(), null, null);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double minimumScale = 0.1;
        double scaleIncrement = 0.01;

        double scale = lookingAt.getZ();
        scale += -e.getWheelRotation() * scale * scaleIncrement;

        // limit the scale to 10% so that it doesn't just turn into a dot
        scale = Math.max(scale, minimumScale);

        // Get the offsets from lookingAt to where the mouse was when the
        // scroll event happened
        Location location1 = getPixelLocation(e.getX(), e.getY());

        // Update the scale
        lookingAt = lookingAt.derive(null, null, scale, null);

        // And the transform
        updateTransform();

        // Get the newly scaled location
        Location location2 = getPixelLocation(e.getX(), e.getY());

        // Get the delta between the two locations.
        Location delta = location2.subtract(location1);

        // Reset Z and C since we don't want to mess with them
        delta = delta.derive(null, null, 0.0, 0.0);

        // And offset lookingAt by the delta
        lookingAt = lookingAt.subtract(delta);

        // If the user hit the minimum scale, center the table.
        // This helps them find it if it gets lost.
        if (scale == minimumScale) {
            lookingAt = new Location(LengthUnit.Millimeters, 0, 0, minimumScale, 0);
        }

        // Repaint will update the transform and we're ready to go.
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // find component that was clicked
        // do something with it

        // for now just pretend they clicked a camera and toggle camera dim
        if (cameraOpacity == 1) {
            cameraOpacity = 0.25;
        }
        else {
            cameraOpacity = 1;
        }
        repaint();
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragStart != null) {
            // jog
            Camera camera = Configuration.get().getMachine().getHeads().get(0).getCameras().get(0);
            Location clickLocation = getPixelLocation(e.getX(), e.getY())
                    .convertToUnits(camera.getLocation().getUnits());
            Location location = camera.getLocation().derive(clickLocation.getX(),
                    clickLocation.getY(), null, null);
            UiUtils.submitUiMachineTask(() -> {
                MovableUtils.moveToLocationAtSafeZ(camera, location);
            });
        }
        dragStart = null;
        dragEnd = null;
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {
        dragStart = null;
        dragEnd = null;
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStart == null) {
            dragStart = e.getPoint();
        }
        dragEnd = e.getPoint();
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void jobLoaded(Job job) {
        repaint();
    }

    @Override
    public void jobStateChanged(JobState state) {}

    @Override
    public void jobEncounteredError(JobError error, String description) {}

    @Override
    public void partProcessingStarted(BoardLocation board, Placement placement) {}

    @Override
    public void partPicked(BoardLocation board, Placement placement) {}

    @Override
    public void partPlaced(BoardLocation board, Placement placement) {}

    @Override
    public void partProcessingCompleted(BoardLocation board, Placement placement) {}

    @Override
    public void detailedStatusUpdated(String status) {}

    @Override
    public void machineHeadActivity(Machine machine, Head head) {
        repaint();
    }

    @Override
    public void machineEnabled(Machine machine) {}

    @Override
    public void machineEnableFailed(Machine machine, String reason) {}

    @Override
    public void machineDisabled(Machine machine, String reason) {}

    @Override
    public void machineDisableFailed(Machine machine, String reason) {}


    @Override
    public void machineBusy(Machine machine, boolean busy) {
        // TODO Auto-generated method stub

    }


    class NavCameraListener implements CameraListener {
        private final Camera camera;

        public NavCameraListener(Camera camera) {
            this.camera = camera;
        }

        @Override
        public void frameReceived(BufferedImage img) {
            cameraImages.put(camera, img);
            repaint();
        }
    }
}
