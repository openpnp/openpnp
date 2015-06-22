/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

import javax.swing.Action;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Consider adding some type of heartbeat to the firmware.  
 */

public class GrblDriver extends AbstractSerialPortDriver implements Runnable {

/*	@Attribute(required=false) 
    private int vacpumpPin;
 
    @Attribute(required=false) 
    private boolean invertVacpump;

*/	
	private static final Logger logger = LoggerFactory.getLogger(GrblDriver.class);
//	private static final double minimumRequiredVersion = 0.75;
	
	@Attribute
    private double feedRateMmPerMinute;
	
	private double x, y, z, c;
	private Thread readerThread;
	private boolean disconnectRequested;
	private Object commandLock = new Object();
	private boolean connected;
//	private double connectedVersion;
	private Queue<String> responseQueue = new ConcurrentLinkedQueue<String>();
	
	public GrblDriver() {
	    Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration)
                    throws Exception {
                connect();
            }
	    });
	}
	
    @Override
    public void home(ReferenceHead head) throws Exception {
	  sendCommand("M2",400); // deactivate pin Actor^M
          sendCommand("$H");   // home^M
          sendCommand("G21"); // units = mm^M
          sendCommand("G92X0Y0Z0"); // ^M
          sendCommand("G92C0"); // for C axis if exist^M
        // Reset all axes to 0. This is required so that the Head and Driver
        // stay in sync.
        sendCommand("G92 X0 Y0 Z0 E0");
        x = y = z = c = 0;
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location,
            double speed) throws Exception {
        location = location.subtract(hm.getHeadOffsets());

        location = location.convertToUnits(LengthUnit.Millimeters);

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        double c = location.getRotation();

        StringBuffer sb = new StringBuffer();
        if (!Double.isNaN(x) && x != this.x) {
            sb.append(String.format(Locale.US, "X%2.4f ", x));
        }
        if (!Double.isNaN(y) && y != this.y) {
            sb.append(String.format(Locale.US, "Y%2.4f ", y));
        }
        if (!Double.isNaN(z) && z != this.z) {
            sb.append(String.format(Locale.US, "Z%2.4f ", z));
        }
        if (!Double.isNaN(c) && c != this.c) {
            sb.append(String.format(Locale.US, "E%2.4f ", c));
        }
        if (sb.length() > 0) {
            sb.append(String.format(Locale.US, "F%2.4f ", feedRateMmPerMinute
                    * speed));
            sendCommand("G1" + sb.toString());
            dwell();
        }
        if (!Double.isNaN(x)) {
            this.x = x;
        }
        if (!Double.isNaN(y)) {
            this.y = y;
        }
        if (!Double.isNaN(z)) {
            this.z = z;
        }
        if (!Double.isNaN(c)) {
            this.c = c;
        }
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        return new Location(LengthUnit.Millimeters, x, y, z, c).add(hm.getHeadOffsets());
    }

    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        sendCommand(String.format("M4"));
        dwell();
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        sendCommand(String.format("M5"));
        dwell();
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on)
            throws Exception {
        if (actuator == null || actuator.getIndex() == 0) { 
	    if(on)
            	sendCommand("M8"); 
	    else
            	sendCommand("M9");
        }
        if (actuator.getIndex() == 1) { 
	    if(on)
            	sendCommand("M7"); 
	    else
            	sendCommand("M9");
	}
            dwell();
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value)
            throws Exception { 
        // TODO Auto-generated method stub
        
    }

    @Override
	public void setEnabled(boolean enabled) throws Exception {
                sendCommand("\030\030\030",700);
                sendCommand("M2",300);
                sendCommand("G21"); // units = mm
                sendCommand("G90"); // units = abs
                sendCommand("G92X0Y0Z0"); // 
                sendCommand("G92C0"); // for C axis if exist
	}

	public synchronized void connect() throws Exception {
		super.connect();

		/**
		 * Connection process notes:
		 * 
		 * On some platforms, as soon as we open the serial port it will reset
		 * Grbl and we'll start getting some data. On others, Grbl may
		 * already be running and we will get nothing on connect.
		 */
		
		List<String> responses;
		synchronized (commandLock) {
			// Start the reader thread with the commandLock held. This will
			// keep the thread from quickly parsing any responses messages
			// and notifying before we get a chance to wait.
			readerThread = new Thread(this);
			readerThread.start();
			// Wait up to 3 seconds for Grbl to say Hi
			// If we get anything at this point it will have been the settings
			// dump that is sent after reset.
			responses = sendCommand("\030", 3000);
		}

		processConnectionResponses(responses);

		for (int i = 0; i < 5 && !connected; i++) {
			responses = sendCommand("\030", 5000);
			processConnectionResponses(responses);
		}
		
		if (!connected)  {
			throw new Error(
//				String.format("Unable to receive connection response from Grbl. Check your port and baud rate, and that you are running at least version %f of Grbl", 
//						minimumRequiredVersion));
				String.format("Unable to receive connection response from Grbl. Check your port and baud rate, and that you are running the latest version of Grbl."));
		}
		
		// TODO: Version Info
//		if (connectedVersion < minimumRequiredVersion) {
//			throw new Error(String.format("This driver requires Grbl version %.2f or higher. You are running version %.2f", minimumRequiredVersion, connectedVersion));
//		}
		
		// We are connected to at least the minimum required version now
		// So perform some setup
		
		// Turn off the stepper drivers
		setEnabled(false);
		
		// Reset all axes to 0, in case the firmware was not reset on
		// connect.
		sendCommand("G92 X0 Y0 Z0");
		sendCommand("G92 C0");
	}
	
	private void processConnectionResponses(List<String> responses) {
		for (String response : responses) 
 if (response.startsWith("Grbl ")||response.startsWith("$VERSION = ")) {
                String[] versionComponents = response.split(" ");
                if(response.startsWith("$"))
                  versionComponents[1]=versionComponents[2];
//              connectedVersion = Double.parseDouble(versionComponents[1]);
                connected = true;
                logger.debug(String.format("Connected to Grbl Version: %s", versionComponents[1]));
		logger.debug(String.format("Connected to Grbl."));
         }       }

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
		return sendCommand(command, -1);
	}
	
	private List<String> sendCommand(String command, long timeout) throws Exception {
		synchronized (commandLock) {
			if (command != null) {
				logger.debug("> " + command);
				output.write(command.getBytes());
				output.write("\n".getBytes());
			}
			long t = System.currentTimeMillis();
			if (timeout == -1) {
				commandLock.wait();
			}
			else {
				commandLock.wait(timeout);
			}
			logger.debug("Waited {} ms for command to return.", (System.currentTimeMillis() - t));
		}
		List<String> responses = drainResponseQueue();
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
			logger.debug("< " + line);
			responseQueue.offer(line);
			// We have a special case of accepting "start" when we are not
			// connected because Grbl does not send an "ok" when it starts
			// up.
  if (line.equals("ok")  || line.startsWith("error: ") || line.startsWith("Grbl ")) {
				// This is the end of processing for a command
				synchronized (commandLock) {
					commandLock.notify();
				}
			}
		}
	}

	/**
	 * Causes Grbl to block until all commands are complete.
	 * @throws Exception
	 */
	protected void dwell() throws Exception {
		sendCommand("G4P0");
	}

	private List<String> drainResponseQueue() {
		List<String> responses = new ArrayList<String>();
		String response;
		while ((response = responseQueue.poll()) != null) {
			responses.add(response);
		}
		return responses;
	}
	
    @Override
    public Wizard getConfigurationWizard() {
        return null;
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
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard())
        };
    }	
    
    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }    
}
