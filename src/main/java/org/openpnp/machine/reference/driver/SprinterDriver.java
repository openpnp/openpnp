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

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.driver.wizards.AbstractSerialPortDriverConfigurationWizard;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Consider adding some type of heartbeat to the firmware.  
 */

//Implemented Codes
//-------------------
//G0  -> G1
//G1  - Coordinated Movement X Y Z E
//G2  - CW ARC
//G3  - CCW ARC
//G4  - Dwell S<seconds> or P<milliseconds>
//G28 - Home all Axis
//G90 - Use Absolute Coordinates
//G91 - Use Relative Coordinates
//G92 - Set current position to cordinates given

//RepRap M Codes
//M104 - Set extruder target temp
//M105 - Read current temp
//M106 - Fan on
//M107 - Fan off
//M109 - Wait for extruder current temp to reach target temp.
//M114 - Display current position

//Custom M Codes
//M20  - List SD card
//M21  - Init SD card
//M22  - Release SD card
//M23  - Select SD file (M23 filename.g)
//M24  - Start/resume SD print
//M25  - Pause SD print
//M26  - Set SD position in bytes (M26 S12345)
//M27  - Report SD print status
//M28  - Start SD write (M28 filename.g)
//M29  - Stop SD write
// -  <filename> - Delete file on sd card
//M42  - Set output on free pins, on a non pwm pin (over pin 13 on an arduino mega) use S255 to turn it on and S0 to turn it off. Use P to decide the pin (M42 P23 S255) would turn pin 23 on
//M80  - Turn on Power Supply
//M81  - Turn off Power Supply
//M82  - Set E codes absolute (default)
//M83  - Set E codes relative while in Absolute Coordinates (G90) mode
//M84  - Disable steppers until next move, 
//      or use S<seconds> to specify an inactivity timeout, after which the steppers will be disabled.  S0 to disable the timeout.
//M85  - Set inactivity shutdown timer with parameter S<seconds>. To disable set zero (default)
//M92  - Set axis_steps_per_unit - same syntax as G92
//M93  - Send axis_steps_per_unit
//M115	- Capabilities string
//M119 - Show Endstopper State 
//M140 - Set bed target temp
//M190 - Wait for bed current temp to reach target temp.
//M201 - Set maximum acceleration in units/s^2 for print moves (M201 X1000 Y1000)
//M202 - Set maximum feedrate that your machine can sustain (M203 X200 Y200 Z300 E10000) in mm/sec
//M203 - Set temperture monitor to Sx
//M204 - Set default acceleration: S normal moves T filament only moves (M204 S3000 T7000) in mm/sec^2
//M205 - advanced settings:  minimum travel speed S=while printing T=travel only,  X=maximum xy jerk, Z=maximum Z jerk
//M206 - set additional homing offset

//M220 - set speed factor override percentage S=factor in percent 
//M221 - set extruder multiply factor S100 --> original Extrude Speed 

//M301 - Set PID parameters P I and D
//M303 - PID relay autotune S<temperature> sets the target temperature. (default target temperature = 150C)

//M400 - Finish all moves

//M500 - stores paramters in EEPROM
//M501 - reads parameters from EEPROM (if you need to reset them after you changed them temporarily).
//M502 - reverts to the default "factory settings". You still need to store them in EEPROM afterwards if you want to.
//M503 - Print settings

//Debug feature / Testing the PID for Hotend
//M601 - Show Temp jitter from Extruder (min / max value from Hotend Temperature while printing)
//M602 - Reset Temp jitter from Extruder (min / max val) --> Don't use it while Printing
//M603 - Show Free Ram

public class SprinterDriver extends AbstractSerialPortDriver implements Runnable {

/*	@Attribute(required=false) 
    private int vacpumpPin;
 
    @Attribute(required=false) 
    private boolean invertVacpump;

*/	
	private static final Logger logger = LoggerFactory.getLogger(SprinterDriver.class);
//	private static final double minimumRequiredVersion = 0.75;
	
	@Attribute(required=false)
	private int vacuumPin = 31;
	
	@Attribute(required=false)
	private boolean invertVacuum;
	
    @Attribute(required=false)
	private int actuatorPin = 33;
	
	@Attribute(required=false)
	private boolean invertActuator;
	
	@Attribute(required=false)
	private boolean homeX;
	
	@Attribute(required=false)
	private boolean homeY;
	
	@Attribute(required=false)
	private boolean homeZ;
	
	@Attribute(required=false)
	private boolean homeC;
	
	@Attribute(required=false)
    private double feedRateMmPerMinute = 5000;
	
	private double x, y, z, c;
	private Thread readerThread;
	private boolean disconnectRequested;
	private Object commandLock = new Object();
	private boolean connected;
//	private double connectedVersion;
	private Queue<String> responseQueue = new ConcurrentLinkedQueue<>();
	
	public SprinterDriver() {
	}
	
    @Override
    public void home(ReferenceHead head) throws Exception {
        if (homeX || homeY || homeZ || homeC) {
            sendCommand(String.format("G28 %s %s %s %s", homeX ? "X" : "", homeY ? "Y" : "", homeZ ? "Z" : "", homeC ? "E" : ""));
            dwell();
        }
        else {
            throw new Exception("No homing axes defined. See the homeX, homeY, homeZ and homeC parameters.");
        }
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
        sendCommand(String.format("M42 P%d S%d", vacuumPin, invertVacuum ? 0 : 255));
        dwell();
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        sendCommand(String.format("M42 P%d S%d", vacuumPin, invertVacuum ? 255 : 0));
        dwell();
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on)
            throws Exception {
        if (actuator == null || actuator.getIndex() == 0) {
            sendCommand(String.format("M42 P%d S%d", actuatorPin, on ^ invertActuator ? 255 : 0));
            dwell();
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value)
            throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
	public void setEnabled(boolean enabled) throws Exception {
        if (enabled && !connected) {
            connect();
        }
        if (connected) {
            sendCommand(String.format("M84 %s", enabled ? "T" : ""));
            place(null);
            actuate(null, false);
        }
	}

	public synchronized void connect() throws Exception {
		super.connect();

		/**
		 * Connection process notes:
		 * 
		 * On some platforms, as soon as we open the serial port it will reset
		 * Sprinter and we'll start getting some data. On others, Sprinter may
		 * already be running and we will get nothing on connect.
		 */
		
		List<String> responses;
		synchronized (commandLock) {
			// Start the reader thread with the commandLock held. This will
			// keep the thread from quickly parsing any responses messages
			// and notifying before we get a chance to wait.
			readerThread = new Thread(this);
			readerThread.start();
			// Wait up to 3 seconds for Sprinter to say Hi
			// If we get anything at this point it will have been the settings
			// dump that is sent after reset.
			responses = sendCommand(null, 3000);
		}

		processConnectionResponses(responses);

		for (int i = 0; i < 5 && !connected; i++) {
			responses = sendCommand("M115", 5000);
			processConnectionResponses(responses);
		}
		
		if (!connected)  {
			throw new Exception(
//				String.format("Unable to receive connection response from Sprinter. Check your port and baud rate, and that you are running at least version %f of Sprinter", 
//						minimumRequiredVersion));
				String.format("Unable to receive connection response from Sprinter. Check your port and baud rate, and that you are running the latest version of Sprinter."));
		}
		
		// TODO: Version Info
//		if (connectedVersion < minimumRequiredVersion) {
//			throw new Error(String.format("This driver requires Sprinter version %.2f or higher. You are running version %.2f", minimumRequiredVersion, connectedVersion));
//		}
		
		// We are connected to at least the minimum required version now
		// So perform some setup
		
		// Turn off the stepper drivers
		setEnabled(false);
		
		// Reset all axes to 0, in case the firmware was not reset on
		// connect.
		sendCommand("G92 X0 Y0 Z0 E0");
	}
	
	private void processConnectionResponses(List<String> responses) {
		for (String response : responses) {
			if (response.startsWith("FIRMWARE_NAME:") || response.equals("Sprinter")) {
//				String[] versionComponents = response.split(" ");
//				connectedVersion = Double.parseDouble(versionComponents[2]);
				connected = true;
//				logger.debug(String.format("Connected to Sprinter Version: %.2f", connectedVersion));
				logger.debug(String.format("Connected to Sprinter."));
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
			// connected because Sprinter does not send an "ok" when it starts
			// up.
			if (line.equals("ok") || line.startsWith("error: ") || (!connected && line.equals("start"))) {
				// This is the end of processing for a command
				synchronized (commandLock) {
					commandLock.notify();
				}
			}
		}
	}

	/**
	 * Causes Sprinter to block until all commands are complete.
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
    public Wizard getConfigurationWizard() {
        return new AbstractSerialPortDriverConfigurationWizard(this);
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
