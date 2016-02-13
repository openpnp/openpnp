package org.openpnp.machine.openbuilds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.Action;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.driver.AbstractSerialPortDriver;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenBuildsDriver extends AbstractSerialPortDriver implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(OpenBuildsDriver.class);

    @Attribute(required=false)
    protected double feedRateMmPerMinute = 5000;
    
    @Attribute(required=false)
    private double zCamRadius = 24;
    
    @Attribute(required=false)
    private double zCamWheelRadius = 9.5;
    
    @Attribute(required=false)
    private double zGap = 2;
    
    @Attribute(required=false)
    private boolean homeZ = false;
    
    protected double x, y, zA, c, c2;
    private Thread readerThread;
    private boolean disconnectRequested;
    private Object commandLock = new Object();
    private boolean connected;
    private LinkedBlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private boolean n1Picked, n2Picked;
    
    @Override
    public void setEnabled(boolean enabled) throws Exception {
        if (enabled && !connected) {
            connect();
        }
        if (connected) {
            if (enabled) {
                n1Vacuum(false);
                n1Exhaust(false);
                n2Vacuum(false);
                n2Exhaust(false);
                led(true);
            }
            else {
                sendCommand("M84");
                n1Vacuum(false);
                n1Exhaust(false);
                n2Vacuum(false);
                n2Exhaust(false);
                led(false);
                pump(false);
                
            }
        }
    }
    
    @Override
    public void home(ReferenceHead head) throws Exception {
        if (homeZ) {
            // Home Z
            sendCommand("G28 Z0", 10 * 1000);
            // Move Z to 0
            sendCommand("G0 Z0");
        }
        else {
            // We "home" Z by turning off the steppers, allowing the
            // spring to pull the nozzle back up to home.
            sendCommand("M84");
            // And call that zero
            sendCommand("G92 Z0");
            // And wait a tick just to let things settle down
            Thread.sleep(250);
        }
        // Home X and Y
        sendCommand("G28 X0 Y0", 60 * 1000);
        // Zero out the two "extruders"
        sendCommand("T1");
        sendCommand("G92 E0");
        sendCommand("T0");
        sendCommand("G92 E0");
        // Update position
        getCurrentPosition();
    }
    
    
    @Override
    public void actuate(ReferenceActuator actuator, boolean on)
            throws Exception {
//        if (actuator.getIndex() == 0) {
//            sendCommand(on ? actuatorOnGcode : actuatorOffGcode);
//            dwell();
//        }
    }
    
    
    
    @Override
    public void actuate(ReferenceActuator actuator, double value)
            throws Exception {
    }
    

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        if (hm instanceof ReferenceNozzle) {
        	ReferenceNozzle nozzle = (ReferenceNozzle) hm;
            double z = Math.sin(Math.toRadians(this.zA)) * zCamRadius;
            if (((ReferenceNozzle) hm).getName().equals("N2")) {
                z = -z;
            }
            z += zCamWheelRadius + zGap;                
            int tool = (nozzle == null || nozzle.getName().equals("N1")) ? 0 : 1;
            return new Location(LengthUnit.Millimeters, x, y, z, tool == 0 ? c : c2).add(hm
                    .getHeadOffsets());
        }
        else {
            return new Location(LengthUnit.Millimeters, x, y, zA, c).add(hm
                    .getHeadOffsets());
        }
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed)
            throws Exception {
        location = location.subtract(hm.getHeadOffsets());

        location = location.convertToUnits(LengthUnit.Millimeters);
        
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        double c = location.getRotation();
        
        ReferenceNozzle nozzle = null;
        if (hm instanceof ReferenceNozzle) {
            nozzle = (ReferenceNozzle) hm;
        }
        
        /*
         * Only move Z if it's a Nozzle.
         */
        if (nozzle == null) {
            z = Double.NaN;
        }
        
        StringBuffer sb = new StringBuffer();
        if (!Double.isNaN(x) && x != this.x) {
            sb.append(String.format(Locale.US, "X%2.2f ", x));
            this.x = x;
        }
        if (!Double.isNaN(y) && y != this.y) {
            sb.append(String.format(Locale.US, "Y%2.2f ", y));
            this.y = y;
        }
        int tool = (nozzle == null || nozzle.getName().equals("N1")) ? 0 : 1;
        if (!Double.isNaN(c) && c != (tool == 0 ? this.c : this.c2)) {
        	// If there is an E move we need to set the tool before
        	// performing any commands otherwise we may move the wrong tool.
        	sendCommand(String.format(Locale.US, "T%d", tool));
            if (sb.length() == 0) {
                // If the move won't contain an X or Y component but will
                // have an E component we need to send the E component as a
                // solo move because Smoothie won't move only E and Z at
                // the same time.
                sendCommand(String.format(Locale.US, "G0 E%2.2f F%2.2f", c, feedRateMmPerMinute));
                dwell();
            }
            else {
                sb.append(String.format(Locale.US, "E%2.2f ", c));
            }
            if (tool == 0) {
            	this.c = c;
            }
            else {
            	this.c2 = c;
            }
        }
        
        if (!Double.isNaN(z)) {
            double a = Math.toDegrees(Math.asin((z - zCamWheelRadius - zGap) / zCamRadius));
            logger.debug("nozzle {} {} {}", new Object[] { z, zCamRadius, a });
            if (nozzle.getName().equals("N2")) {
                a = -a;
            }
            if (a != this.zA) {
                sb.append(String.format(Locale.US, "Z%2.2f ", a));
                this.zA = a;
            }
        }
        
        if (sb.length() > 0) {
            sb.append(String.format(Locale.US, "F%2.2f", feedRateMmPerMinute));
            sendCommand("G0 " + sb.toString());
            dwell();
        }
    }
    
    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        if (((ReferenceNozzle) nozzle).getName().equals("N1")) {
            pump(true);
            n1Exhaust(false);
            n1Vacuum(true);
            n1Picked = true;
        }
        else {
            pump(true);
            n2Exhaust(false);
            n2Vacuum(true);
            n2Picked = true;
        }
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        if (((ReferenceNozzle) nozzle).getName().equals("N1")) {
            n1Picked = false;
            if (!n1Picked && !n2Picked) {
                pump(false);
            }
            n1Vacuum(false);
            n1Exhaust(true);
            Thread.sleep(500);
            n1Exhaust(false);
        }
        else {
            n2Picked = false;
            if (!n1Picked && !n2Picked) {
                pump(false);
            }
            n2Vacuum(false);
            n2Exhaust(true);
            Thread.sleep(500);
            n2Exhaust(false);
        }
    }
    
    public synchronized void connect()
            throws Exception {
        super.connect();
        
        /**
         * Connection process notes:
         * 
         * On some platforms, as soon as we open the serial port it will reset
         * the controller and we'll start getting some data. On others, it may
         * already be running and we will get nothing on connect.
         */

        connected = false;
        List<String> responses;
        readerThread = new Thread(this);
        readerThread.start();
            
        try {
            do {
                // Consume any buffered incoming data, including startup messages
                responses = sendCommand(null, 200);
            } while (!responses.isEmpty());
        }
        catch (Exception e) {
            // ignore timeouts
        }
            
        
    	// Send a request to force Smoothie to respond and clear any buffers.
        // On my machine, at least, this causes Smoothie to re-send it's
        // startup message and I can't figure out why, but this works
        // around it.
    	responses = sendCommand("M114", 5000);
    	// Continue to read responses until we get the one that is the
    	// result of the M114 command. When we see that we're connected.
    	long t = System.currentTimeMillis();
    	while (System.currentTimeMillis() - t < 5000) {
            for (String response : responses) {
            	if (response.contains("X:")) {
            		connected = true;
            		break;
            	}
            }
            if (connected) {
            	break;
            }
            responses = sendCommand(null, 200);
    	}

        if (!connected)  {
            throw new Exception(
                String.format("Unable to receive connection response. Check your port and baud rate"));
        }
        
        // We are connected to at least the minimum required version now
        // So perform some setup
        
        // Turn off the stepper drivers
        setEnabled(false);
        
        // Set mm coordinate mode
        sendCommand("G21");
        // Set absolute positioning mode
        sendCommand("G90");
        // Set absolute mode for extruder
        sendCommand("M82");
        getCurrentPosition();
    }
    
    protected void getCurrentPosition() throws Exception {
        List<String> responses;
    	sendCommand("T0");
        responses = sendCommand("M114");
        for (String response : responses) {
            if (response.contains("X:")) {
                String[] comps = response.split(" ");
                for (String comp : comps) {
                    if (comp.startsWith("X:")) {
                        x = Double.parseDouble(comp.split(":")[1]);
                    }
                    else if (comp.startsWith("Y:")) {
                        y = Double.parseDouble(comp.split(":")[1]);
                    }
                    else if (comp.startsWith("Z:")) {
                        zA = Double.parseDouble(comp.split(":")[1]);
                    }
                    else if (comp.startsWith("E:")) {
                        c = Double.parseDouble(comp.split(":")[1]);
                    }
                }
            }
        }
    	sendCommand("T1");
        responses = sendCommand("M114");
        for (String response : responses) {
            if (response.contains("X:")) {
                String[] comps = response.split(" ");
                for (String comp : comps) {
                    if (comp.startsWith("E:")) {
                        c2 = Double.parseDouble(comp.split(":")[1]);
                    }
                }
            }
        }
        sendCommand("T0");
        logger.debug("Current Position is {}, {}, {}, {}, {}", new Object[] { x, y, zA, c, c2 });
    }
    
    public synchronized void disconnect() {
        disconnectRequested = true;
        connected = false;
        
        try {
            if (readerThread != null && readerThread.isAlive()) {
                readerThread.join();
            }
        }
        catch (Exception e) {
            logger.error("disconnect()", e);
        }
        
        try {
            super.disconnect();
        }
        catch (Exception e) {
            logger.error("disconnect()", e);
        }
        disconnectRequested = false;
    }

    protected List<String> sendCommand(String command) throws Exception {
        return sendCommand(command, 5000);
    }
    
    protected List<String> sendCommand(String command, long timeout) throws Exception {
        List<String> responses = new ArrayList<>();
        
        // Read any responses that might be queued up so that when we wait
        // for a response to a command we actually wait for the one we expect.
        responseQueue.drainTo(responses);

        // Send the command, if one was specified
        if (command != null) {
            logger.debug("sendCommand({}, {})", command, timeout);
            logger.debug(">> " + command);
            output.write(command.getBytes());
            output.write("\n".getBytes());
        }

        String response = null;
        if (timeout == -1) {
            // Wait forever for a response to return from the reader.
            response = responseQueue.take();
        }
        else {
            // Wait up to timeout milliseconds for a response to return from
            // the reader.
            response = responseQueue.poll(timeout, TimeUnit.MILLISECONDS);
            if (response == null) {
                throw new Exception("Timeout waiting for response to " + command);
            }
        }
        // And if we got one, add it to the list of responses we'll return.
        responses.add(response);
        
        // Read any additional responses that came in after the initial one.
        responseQueue.drainTo(responses);

        logger.debug("{} => {}", command, responses);
        return responses;
    }
    
    public void run() {
        while (!disconnectRequested) {
            String line;
            try {
                line = readLine().trim();
            }
            catch (TimeoutException ex) {
                continue;
            }
            catch (IOException e) {
                logger.error("Read error", e);
                return;
            }
            line = line.trim();
            logger.debug("<< " + line);
            responseQueue.offer(line);
            if (line.startsWith("ok") || line.startsWith("error: ")) {
                // This is the end of processing for a command
                synchronized (commandLock) {
                    commandLock.notify();
                }
            }
        }
    }

    /**
     * Block until all movement is complete.
     * @throws Exception
     */
    protected void dwell() throws Exception {
        sendCommand("M400");
    }

    private List<String> drainResponseQueue() {
        List<String> responses = new ArrayList<>();
        String response;
        while ((response = responseQueue.poll()) != null) {
            responses.add(response);
        }
        return responses;
    }
    
    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard())
        };
    }
    
    @Override
    public Wizard getConfigurationWizard() {
        return new OpenBuildsDriverWizard(this);
    }
    
    private void n1Vacuum(boolean on) throws Exception {
        sendCommand(on ? "M800" : "M801");
    }
    
    private void n1Exhaust(boolean on) throws Exception {
        sendCommand(on ? "M802" : "M803");
    }
    
    private void n2Vacuum(boolean on) throws Exception {
        sendCommand(on ? "M804" : "M805");
    }
    
    private void n2Exhaust(boolean on) throws Exception {
        sendCommand(on ? "M806" : "M807");
    }
    
    private void pump(boolean on) throws Exception {
        sendCommand(on ? "M808" : "M809");
    }
    
    private void led(boolean on) throws Exception {
        sendCommand(on ? "M810" : "M811");
    }
}
