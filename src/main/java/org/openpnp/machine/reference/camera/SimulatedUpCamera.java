package org.openpnp.machine.reference.camera;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

    private Map<Nozzle, Part> nozzleParts = new HashMap<>();

    private Location offsets = new Location(LengthUnit.Millimeters);

    public SimulatedUpCamera() {
        setUnitsPerPixel(new Location(LengthUnit.Millimeters, 0.0234375D, 0.0234375D, 0, 0));
        setLooking(Looking.Up);
        Configuration.get()
                     .addListener(new ConfigurationListener.Adapter() {
                         @Override
                         public void configurationComplete(Configuration configuration)
                                 throws Exception {
                             configuration.getMachine()
                                          .addListener(machineListener);
                         }
                     });
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
        // g.setColor(Color.white);
        // Location l = nozzle.getLocation().convertToUnits(LengthUnit.Millimeters);
        //
        // Location upp = getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);
        // Location scale =
        // new Location(LengthUnit.Millimeters, 1D / upp.getX(), 1D / upp.getY(), 0, 0);
        // l = l.multiply(scale);
        //
        // g.fillOval((int) (l.getX() - 20), (int) (l.getY() - 20), 40, 40);

        g.setStroke(new BasicStroke(1f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.white);

        Part part = nozzleParts.get(nozzle);
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

        Location upp = getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);

        // Determine the scaling factor to go from Outline units to
        // Camera units.
        Length l = new Length(1, footprint.getUnits());
        l = l.convertToUnits(upp.getUnits());
        double unitScale = l.getValue();

        // Create a transform to scale the Shape by
        AffineTransform tx = new AffineTransform();

        // First we scale by units to convert the units and then we scale
        // by the camera X and Y units per pixels to get pixel locations.
        tx.scale(unitScale, unitScale);
        tx.scale(1.0 / upp.getX(), 1.0 / upp.getY());

        tx.translate(offsets.getX(), offsets.getY());
        // AffineTransform rotates positive clockwise, so we invert the value.
        tx.rotate(Math.toRadians(offsets.getRotation()));


        // Transform the Shape and draw it out.
        shape = tx.createTransformedShape(shape);
        g.fill(shape);
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

    private MachineListener machineListener = new MachineListener.Adapter() {
        @Override
        public void machineHeadActivity(Machine machine, Head head) {
            if (head == null) {
                return;
            }
            for (Nozzle nozzle : head.getNozzles()) {
                Part part = nozzleParts.get(nozzle);
                if (part == null || part != nozzle.getPart()) {
                    nozzleParts.put(nozzle, nozzle.getPart());
                    Random r = new Random();
                    offsets = new Location(LengthUnit.Millimeters, Math.random() * 2 - 1,
                            Math.random() * 2 - 1, 0, Math.random() * 30 - 15);
                }
            }
        }
    };
}
