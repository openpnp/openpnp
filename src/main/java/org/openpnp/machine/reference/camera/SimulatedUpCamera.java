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

import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.SimulatedUpCameraConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;


@Root
public class SimulatedUpCamera extends ReferenceCamera implements Runnable {
    protected int width = 1280;

    protected int height = 1280;

    protected int fps = 10;

    private Thread thread;
    
    @Element(required=false)
    private Location errorOffsets = new Location(LengthUnit.Millimeters);

    public SimulatedUpCamera() {
        setUnitsPerPixel(new Location(LengthUnit.Millimeters, 0.0234375D, 0.0234375D, 0, 0));
        setLooking(Looking.Up);
    }

    @Override
    public BufferedImage internalCapture() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        AffineTransform tx = g.getTransform();
        // invert the image in Y so that Y+ is up
        g.translate(0, height);
        g.scale(1, -1);
        g.translate(width / 2, height / 2);

        g.setColor(Color.black);
        g.fillRect(0, 0, width, height);

        // figure out our physical viewport size
        Location phySize = getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters)
                                             .multiply(width, height, 0, 0);
        double phyWidth = phySize.getX();
        double phyHeight = phySize.getY();

        // and bounds
        Location location = getLocation().convertToUnits(LengthUnit.Millimeters);
        Rectangle2D.Double phyBounds = new Rectangle2D.Double(location.getX() - phyWidth / 2,
                location.getY() - phyHeight / 2, phyWidth, phyHeight);

        // determine if there are any nozzles within our bounds and if so render them
        for (Head head : Configuration.get()
                                      .getMachine()
                                      .getHeads()) {
            for (Nozzle nozzle : head.getNozzles()) {
                Location l = nozzle.getLocation()
                                   .convertToUnits(LengthUnit.Millimeters);
                if (phyBounds.contains(l.getX(), l.getY())) {
                    drawNozzle(g, nozzle);
                }
            }
        }

        g.setTransform(tx);
        g.dispose();
        return image;
    }


    private void drawNozzle(Graphics2D g, Nozzle nozzle) {
        g.setStroke(new BasicStroke(2f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        LengthUnit units = LengthUnit.Millimeters;
        Location unitsPerPixel = getUnitsPerPixel().convertToUnits(units);
        
        // Draw the nozzle
        // Get nozzle offsets from camera
        Location offsets = nozzle.getLocation()
                .convertToUnits(units)
                .subtractWithRotation(getLocation());
        
        // Create a nozzle shape
        fillShape(g, new Ellipse2D.Double(-0.5, -0.5, 1, 1), Color.green, unitsPerPixel, offsets, false);

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
    
    public Location getErrorOffsets() {
        return errorOffsets;
    }

    public void setErrorOffsets(Location errorOffsets) {
        this.errorOffsets = errorOffsets;
    }

    @Override
    public synchronized void startContinuousCapture(CameraListener listener, int maximumFps) {
        start();
        super.startContinuousCapture(listener, maximumFps);
    }

    @Override
    public synchronized void stopContinuousCapture(CameraListener listener) {
        super.stopContinuousCapture(listener);
        if (listeners.size() == 0) {
            stop();
        }
    }

    private synchronized void stop() {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            try {
                thread.join(3000);
            }
            catch (Exception e) {

            }
            thread = null;
        }
    }

    private synchronized void start() {
        if (thread == null) {
            thread = new Thread(this);
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void run() {
        while (!Thread.interrupted()) {
            BufferedImage frame = internalCapture();
            broadcastCapture(frame);
            try {
                Thread.sleep(1000 / fps);
            }
            catch (InterruptedException e) {
                return;
            }
        }
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
}
