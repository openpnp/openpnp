package org.openpnp.machine.reference.camera;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.ConcurrentModificationException;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.machine.reference.camera.wizards.SimulatedUpCameraConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;


@Root
public class SimulatedUpCamera extends ReferenceCamera {
    @Attribute(required=false)
    protected int width = 640;

    @Attribute(required=false)
    protected int height = 480;
    
    @Attribute(required=false)
    private boolean simulateFocalBlur;

    @Element(required=false)
    private Location errorOffsets = new Location(LengthUnit.Millimeters);

    @Element(required=false)
    private Location simulatedLocation;

    @Element(required=false)
    private Location simulatedUnitsPerPixel;

    @Attribute(required=false)
    private boolean simulatedFlipped;

    public SimulatedUpCamera() {
        setUnitsPerPixel(new Location(LengthUnit.Millimeters, 0.0234375D, 0.0234375D, 0, 0));
        setLooking(Looking.Up);
    }

    @Override
    public BufferedImage internalCapture() {
        if (!ensureOpen()) {
            return null;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setColor(Color.black);
        g.fillRect(0, 0, width, height);
        AffineTransform tx = g.getTransform();
        // invert the image in Y so that Y+ is up
        g.translate(0, height);
        g.scale(1, -1);
        g.translate(width / 2, height / 2);
        g.rotate(Math.toRadians(getSimulatedLocation().getRotation()));
        if (isSimulatedFlipped()) {
            g.scale(-1.0, 1.0);
        }

        // figure out our physical viewport size
        Location phySize = getSimulatedUnitsPerPixel().convertToUnits(LengthUnit.Millimeters)
                .multiply(width, height, 0, 0);
        double phyWidth = phySize.getX();
        double phyHeight = phySize.getY();

        // and bounds
        Location location = getSimulatedLocation().convertToUnits(LengthUnit.Millimeters);
        Rectangle2D.Double phyBounds = new Rectangle2D.Double(location.getX() - phyWidth / 2,
                location.getY() - phyHeight / 2, phyWidth, phyHeight);

        // determine if there are any nozzles within our bounds and if so render them
        Machine machine = Configuration.get()
                .getMachine();
        if (machine != null) {
            try {
                for (Head head :  machine.getHeads()) {
                    for (Nozzle nozzle : head.getNozzles()) {
                        Location l = SimulationModeMachine.getSimulatedPhysicalLocation(nozzle, getLooking());
                        if (phyBounds.contains(l.getX(), l.getY())) {
                            drawNozzle(g, nozzle, l);
                        }
                    }
                }
            }
            catch (ConcurrentModificationException e) {
                // If nozzles are added/removed while enumerating them here, a ConcurrentModificationExceptions 
                // is thrown. This is not so unlikely when this camera has high fps. 
            }
        }

        g.setTransform(tx);

        SimulationModeMachine.simulateCameraExposure(this, g, width, height);

        g.dispose();

        return image;
    }

    private void drawNozzle(Graphics2D gView, Nozzle nozzle, Location l) {
        BufferedImage frame;
        Graphics2D g; 
        if (isSimulateFocalBlur()) {
            frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            g = frame.createGraphics();
            g.setTransform(gView.getTransform());
            // Clear with transparent background
            g.setBackground(new Color(0, 0, 0, 0));
            g.clearRect(-width/2, -height/2, width, height);
        }
        else {
           frame = null;
           g = gView;
        }

        g.setStroke(new BasicStroke(2f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        LengthUnit units = LengthUnit.Millimeters;
        Location unitsPerPixel = getSimulatedUnitsPerPixel().convertToUnits(units);

        // Draw the nozzle
        // Get nozzle offsets from camera
        Location offsets = l.subtractWithRotation(getSimulatedLocation());

        // Create a nozzle shape
        fillShape(g, new Ellipse2D.Double(-0.5, -0.5, 1, 1), new Color(0, 220, 0), unitsPerPixel, offsets, false);

        if (frame != null) {
            blurObjectIntoView(gView, frame, nozzle, l);

            // Clear with transparent background
            g.setBackground(new Color(0, 0, 0, 0));
            g.clearRect(-width/2, -height/2, width, height);
        }

        // Draw the part
        Part part = nozzle.getPart();
        if (part == null) {
            return;
        }

        org.openpnp.model.Package pkg = part.getPackage();
        Footprint footprint = pkg.getFootprint();
        if (footprint == null) {
            return;
        }

        if (footprint.getUnits() != units) {
            throw new Error("Not yet supported.");
        }

        // First draw the body in dark grey.
        fillShape(g, footprint.getBodyShape(), new Color(60, 60, 60), unitsPerPixel, offsets, true);

        // Then draw the pads in white
        fillShape(g, footprint.getPadsShape(), Color.white, unitsPerPixel, offsets, true);

        if (frame != null) {
            blurObjectIntoView(gView, frame, nozzle, 
                l.subtract(new Location(part.getHeight().getUnits(), 0, 0, Math.abs(part.getHeight().getValue()), 0)));

            g.dispose();
        }
    }

    protected void blurObjectIntoView(Graphics2D gView, BufferedImage frame, Nozzle nozzle, Location l) {
        // Blur according to Z coordinate
        AffineTransform tx = gView.getTransform();
        gView.setTransform(new AffineTransform());
        double distanceMm = Math.abs(l.subtract(getSimulatedLocation()).convertToUnits(LengthUnit.Millimeters).getZ());
        final double bokeh = 0.01/getSimulatedUnitsPerPixel().convertToUnits(LengthUnit.Millimeters).getX();
        double radius = distanceMm*bokeh;
        ConvolveOp op = null;
        if (radius > 0.01) {
            int size = (int)Math.ceil(radius) * 2 + 1;
            float[] data = new float[size * size];
            double sum = 0;
            int num = 0;
            for (int i = 0; i < data.length; i++) {
                double x = i/size - size/2.0 + 0.5;
                double y = i%size - size/2.0 + 0.5;
                double r = Math.sqrt(x*x+y*y);
                // rough approximation
                float weight = (float) Math.max(0, Math.min(1, radius + 1 - r));
                data[i] = weight;
                sum += weight;
                if (weight > 0) {
                    num++;
                }
            }
            if (num > 1) {
                for (int i = 0; i < data.length; i++) {
                    data[i] /= sum;
                }

                Kernel kernel = new Kernel(size, size, data);
                op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
            }
        }
        gView.drawImage(frame, op, 0, 0);
        gView.setTransform(tx);
    }

    private void fillShape(Graphics2D g, Shape shape, Color color, Location unitsPerPixel, Location offsets, boolean addError) {
        AffineTransform tx = new AffineTransform();
        // Scale to pixels
        tx.scale(1.0 / unitsPerPixel.getX(), 1.0 / unitsPerPixel.getY());
        // Translate and rotate to offsets
        tx.translate(offsets.getX(), offsets.getY());
        tx.rotate(Math.toRadians(Utils2D.normalizeAngle(offsets.getRotation())));
        if (addError) {
            // Translate and rotate to error offsets
            tx.translate(errorOffsets.getX(), errorOffsets.getY());
            tx.rotate(Math.toRadians(Utils2D.normalizeAngle(errorOffsets.getRotation())));
        }
        // Transform
        shape = tx.createTransformedShape(shape);
        // Draw
        g.setColor(color);
        g.fill(shape);
    }

    public int getViewWidth() {
        return width;
    }

    public void setViewWidth(int width) {
        this.width = width;
    }

    public int getViewHeight() {
        return height;
    }

    public void setViewHeight(int height) {
        this.height = height;
    }

    public Location getSimulatedLocation() {
        if (simulatedLocation == null) {
            simulatedLocation = this.getLocation();
        }
        return simulatedLocation;
    }

    public void setSimulatedLocation(Location simulatedLocation) {
        this.simulatedLocation = simulatedLocation;
    }

    public Location getSimulatedUnitsPerPixel() {
        if (simulatedUnitsPerPixel == null) {
            simulatedUnitsPerPixel = getUnitsPerPixel();
        }
        return simulatedUnitsPerPixel;
    }

    public void setSimulatedUnitsPerPixel(Location simulatedUnitsPerPixel) {
        this.simulatedUnitsPerPixel = simulatedUnitsPerPixel;
    }

    public boolean isSimulatedFlipped() {
        return simulatedFlipped;
    }

    public void setSimulatedFlipped(boolean simulatedFlipped) {
        this.simulatedFlipped = simulatedFlipped;
    }

    public boolean isSimulateFocalBlur() {
        return simulateFocalBlur;
    }

    public void setSimulateFocalBlur(boolean simulateFocalBlur) {
        this.simulateFocalBlur = simulateFocalBlur;
    }

    public Location getErrorOffsets() {
        return errorOffsets;
    }

    public void setErrorOffsets(Location errorOffsets) {
        this.errorOffsets = errorOffsets;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new SimulatedUpCameraConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }


    @Override
    public void findIssues(Solutions solutions) {
        super.findIssues(solutions);
        if (solutions.isTargeting(Milestone.Connect)) {
            solutions.add(new Solutions.Issue(
                    this, 
                    "The SimulatedUpCamera can be replaced with a OpenPnpCaptureCamera to connect to a real USB camera.", 
                    "Replace with OpenPnpCaptureCamera.", 
                    Severity.Fundamental,
                    "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera") {

                @Override
                public void setState(Solutions.State state) throws Exception {
                    if (state == Solutions.State.Solved) {
                        OpenPnpCaptureCamera camera = createReplacementCamera();
                        replaceCamera(camera);
                    }
                    else if (getState() == Solutions.State.Solved) {
                        // Place the old one back (from the captured SimulatedUpCamera.this).
                        replaceCamera(SimulatedUpCamera.this);
                    }
                    super.setState(state);
                }
            });
        }
    }
}
