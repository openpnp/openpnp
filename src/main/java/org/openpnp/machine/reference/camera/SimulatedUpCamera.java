package org.openpnp.machine.reference.camera;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.openpnp.CameraListener;
import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.wizards.ReferenceCameraConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Head;
import org.openpnp.spi.Machine;
import org.openpnp.spi.MachineListener;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Root;

@Root
public class SimulatedUpCamera extends ReferenceCamera implements Runnable {
    protected int width = 640;

    protected int height = 480;

    protected int fps = 10;

    private Thread thread;

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

    
    // TODO STOPSHIP this is all getting close, and there is good code here, but way too much
    // mixing of units. Figure out how to do a single scale to UPP at the end (or beginning)
    // in the transform and then just work in real units.
    // TODO STOPSHIP Shit. That won't work. All the graphics primitives take ints, not doubles.
    private void drawNozzle(Graphics2D g, Nozzle nozzle) {
        AffineTransform tx = g.getTransform();
        
        g.setStroke(new BasicStroke(1f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        LengthUnit units = LengthUnit.Millimeters;
        Location mmPerPixels = getUnitsPerPixel().convertToUnits(units);
        Location pixelsPerMm = new Location(units, 1D / mmPerPixels.getX(), 1D / mmPerPixels.getY(), 1, 1);
        
        // Set a scale on the transform to convert mm to pixels. Everything below will be drawn
        // in mm and automatically scaled to pixels by this transform.
        tx.scale(pixelsPerMm.getX(), pixelsPerMm.getY());
        
        // Draw a 1mm circle representation of the nozzle
        g.setColor(new Color(0, 150, 0));
        Location nozzleLocation = nozzle
                .getLocation()
                .convertToUnits(units)
                .subtract(getLocation());
        g.fillOval((int) (nozzleLocation.getX() - 0.5), (int) (nozzleLocation.getY() - 0.5), 1, 1);
        
        Part part = nozzle.getPart();
        if (part == null) {
            return;
        }

        org.openpnp.model.Package pkg = part.getPackage();
        Footprint footprint = pkg.getFootprint();
        if (footprint == null) {
            return;
        }

        Shape shape = footprint.getShape();
        if (shape == null) {
            return;
        }
        
//        // Determine the scaling factor to go from Outline units to
//        // Camera units (mm)
//        Location footprintScale = pixelsPerMm.multiply(new Location(footprint.getUnits(), 1, 1, 1, 1));
//
//        // Create a transform that will be applied to the footprint.
//        AffineTransform tx = new AffineTransform();
//
//        Location offsets = new Location(LengthUnit.Millimeters, 2, 2, 0, 10);
//
////        // Rotate the footprint by the nozzle rotation.
////        tx.rotate(Math.toRadians(nozzle.getLocation().getRotation()));
////        // And by the offset error.
////        tx.rotate(Math.toRadians(offsets.getRotation()));
//
//        // Translate the footprint so that it is at the same point as the nozzle.
//        tx.translate(nozzleOffset.getX(), nozzleOffset.getY());
//        // And by the offset error.
//        tx.translate(offsets.getX(), offsets.getY());
//
//        // Scale the footprint to pixels.
//        tx.scale(footprintScale.getX(), footprintScale.getY());
//        
//        // Transform the Shape and draw it out.
//        shape = tx.createTransformedShape(shape);
//        g.setColor(Color.white);
//        g.fill(shape);
        
        g.setTransform(tx);
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
        return new ReferenceCameraConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }
}
