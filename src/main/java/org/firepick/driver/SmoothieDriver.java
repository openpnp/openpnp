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

// This file is intended to support the RepRap Marlin motion controller.
// It is a mashup of TinygDriver.java and GrblDriver.java.
// - Neil Jansen (njansen1@gmail.com) 6/30/2014
// - Douglas Pearless (Douglas.Pearless@gmail.com) This is based on Neil's Marlin driver
//   and is written for my fork of Smoothie, which adds the M115 command and other features required by FPD 2015-08-12
package org.firepick.driver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

import javax.swing.Action;

import org.firepick.driver.wizards.SmoothieDriverWizard;
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

/**
 * TODO: Consider adding some type of heartbeat to the firmware.  
 */
public class SmoothieDriver extends AbstractSerialPortDriver implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(SmoothieDriver.class);
	private static final double minimumRequiredVersion = 1.0;
	
	@Attribute(required=false)
	private double feedRateMmPerMinute = 5000;
	
	
	private double x, y, z, c;
	private Thread readerThread;
	private boolean disconnectRequested;
	private Object commandLock = new Object();
	private boolean connected;
	//private double connectedVersion;
	private Queue<String> responseQueue = new ConcurrentLinkedQueue<>();
	
//	public SmootiheDriver() {
//        Configuration.get().addListener(new ConfigurationListener.Adapter() {
//            @Override
//            public void configurationComplete(Configuration configuration)
//                    throws Exception {
//                connect();
//            }
//        });
//	}
	
	@Override
	public void actuate(ReferenceActuator actuator, boolean on)
			throws Exception {
		if (actuator.getIndex() == 0) {
			sendCommand(on ? "M8" : "M9");
			dwell();
		}
	}
	
	
	
	@Override
	public void home(ReferenceHead head) throws Exception {
		List<String> responses;
		sendCommand("M999");
		sendCommand("M82");
		sendCommand("G28");
	
		//For some machines, home is not 0,0,0.  Send an M114 command to get the current position, after homing.
		responses = sendCommand("M114");
	
		for (String response : responses) {
			if (response.toUpperCase().startsWith("OK ")) {
				logger.debug("echo: " + response);
				String[] coords = response.split(" ");
				x = Double.parseDouble(coords[2].substring(2));
				y = Double.parseDouble(coords[3].substring(2));
				z = Double.parseDouble(coords[4].substring(2));
				c = Double.parseDouble(coords[9].substring(2));
			}
		}

		//x = y = z= c = 0;
	}
	
	@Override
  public Location getLocation(ReferenceHeadMountable hm) {
    return new Location(LengthUnit.Millimeters, x, y, z, c).add(hm.getHeadOffsets());
  }

  @Override
  public void actuate(ReferenceActuator actuator, double value)
      throws Exception {
      	dwell();
        // TODO Auto-generated method stub
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
        
        StringBuffer sb = new StringBuffer();
        if (!Double.isNaN(x) && x != this.x) {
            sb.append(String.format(Locale.US, "X%2.2f ", x));
        }
        if (!Double.isNaN(y) && y != this.y) {
            sb.append(String.format(Locale.US, "Y%2.2f ", y));
        }
        if (!Double.isNaN(z) && z != this.z) {
            sb.append(String.format(Locale.US, "Z%2.2f ", z));
        }
        if (!Double.isNaN(c) && c != this.c) {
            sb.append(String.format(Locale.US, "E%2.2f ", c));
        }
        if (sb.length() > 0) {
            sb.append(String.format(Locale.US, "F%2.2f", feedRateMmPerMinute * speed));
            sendCommand("G1 " + sb.toString());
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
        
        dwell();
	}
	
	@Override
	public void setEnabled(boolean enabled) throws Exception {
        if (enabled) {
            if (!connected) {
                try {
                    connect();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                }
            }
            sendCommand("M5");       // Turn the vacuum pump OFF
    		sendCommand("M80");      // Turn power supply ON
    		Thread.sleep(500,0);     // Delay for a bit, wait for power supply to stabilize.
            sendCommand("M999");     // Clear errors
            sendCommand("M17");      // Enable power for all stepper motors
            sendCommand("M420R255"); // Turn on down-looking LED ring light
            sendCommand("M421R255"); // Turn on up-looking LED ring light
        } //if (enabled)
        else{
        	if (connected)
        	{
                sendCommand("M5");       // Turn the vacuum pump OFF
                sendCommand("M18");      // Disable all stepper motors.  Same as M84.
                sendCommand("M420R0");   // Turn off down-looking LED ring light
                sendCommand("M421R0");   // Turn off up-looking LED ring light
        		sendCommand("M81");      // Turn power supply OFF
        	}
        }
	}

	@Override
	public void pick(ReferenceNozzle nozzle) throws Exception {
		sendCommand("M4");
		dwell();
		
	}

	@Override
	public void place(ReferenceNozzle nozzle) throws Exception {
		sendCommand("M5");
		dwell();
	}

	public synchronized void connect()
			throws Exception {
	    super.connect();

		/**
		 * Connection process notes:
		 * 
		 * On some platforms, as soon as we open the serial port it will reset
		 * Grbl and we'll start getting some data. On others, Grbl may already
		 * be running and we will get nothing on connect.
		 */
		
		List<String> responses;
		synchronized (commandLock) {
			// Start the reader thread with the commandLock held. This will
			// keep the thread from quickly parsing any responses messages
			// and notifying before we get a change to wait.
			readerThread = new Thread(this);
			readerThread.start();
			// Wait up to 3 seconds for Smoothie to say Hi
			// If we get anything at this point it will have been the settings
			// dump that is sent after reset.
			responses = sendCommand(null, 3000);
		}

//		connectedVersion = 1.0;
		connected = false;
		processConnectionResponses(responses);  //Flush out any start up messages

		//Now try to determine the firmware level
		for (int i = 0; i < 5 && !connected; i++) {
			responses = sendCommand("M115", 5000);
			processConnectionResponses(responses);
		}
		
  if	(!connected)  {
			throw new Error(
				String.format("Unable to receive connection response from Smoothie. Check your port and baud rate, and that you are running the latest version of Smoothie from https://github.com/DouglasPearless/Smoothieware", 
						minimumRequiredVersion));
		}
		
		// Turn off the stepper drivers
		setEnabled(false);
		
		// Reset all axes to 0, in case the firmware was not reset on
		// connect.
		sendCommand("G92 X0 Y0 Z0 E0");
	}
	
	private void processConnectionResponses(List<String> responses) {
		for (String response : responses) {
			if (response.toUpperCase().startsWith("SMOOTHIE")) {
				logger.debug("echo: " + response);
				logger.debug(String.format("Connected to Smoothie"));
			} else if (response.toUpperCase().startsWith("PROTOCOL")) {
				logger.debug("echo: " + response);
				String[] versionComponents = response.split(" ");

				if (versionComponents[3].toUpperCase().startsWith("DOUGLASPEARLESS")) {
					connected = true;
					logger.debug(String.format("Connected to Smoothie with the correct firmware version"));
				} else
					logger.debug(String.format("Wrong version of Smoothie, please obtain the latest version from https://github.com/DouglasPearless/Smoothieware"));
			}
		}
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


	private List<String> sendCommand(String command) throws Exception {
		return sendCommand(command, 500); // changed from -1
	}
	
	private List<String> sendCommand(String command, long timeout) throws Exception {
		synchronized (commandLock) {
			if (command != null) {
				logger.debug("sendCommand({}, {})", command, timeout);
				output.write(command.getBytes());
				output.write("\n".getBytes());
			}
			if (timeout == -1) {
				commandLock.wait();
			}
			else {
				commandLock.wait(timeout);
			}
		}
		List<String> responses = drainResponseQueue();
		return responses;
	}
	
	//Serial receive thread
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
			logger.debug(line);
			responseQueue.offer(line);
			if (line.equals("ok") || line.startsWith("error: ")) {
				// This is the end of processing for a command
				synchronized (commandLock) {
					commandLock.notify();
				}
			}
		}
	}

	/**
	 * Causes Smoothie to block until all commands are complete.
	 * @throws Exception
	 */
	private void dwell() throws Exception {
		sendCommand("M400");
		//sendCommand("G4 P0");
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
    public Wizard getConfigurationWizard() {
        return new SmoothieDriverWizard(this);
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
}
