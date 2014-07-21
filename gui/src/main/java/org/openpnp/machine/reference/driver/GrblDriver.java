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

import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetConfigurable;
import org.simpleframework.xml.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Consider adding some type of heartbeat to the firmware.  
 */
public class GrblDriver implements ReferenceDriver, Runnable {
	private static final Logger logger = LoggerFactory.getLogger(GrblDriver.class);
	private static final double minimumRequiredVersion = 0.81;
	
	@Attribute
	private String portName;
	@Attribute
	private int baud;
	@Attribute
	private double feedRateMmPerMinute;
	
	
	private double x, y, z, c;
	private SerialPort serialPort;
	private InputStream input;
	private OutputStream output;
	private Thread readerThread;
	private boolean disconnectRequested;
	private Object commandLock = new Object();
	private boolean connected;
	private double connectedVersion;
	private Queue<String> responseQueue = new ConcurrentLinkedQueue<String>();
	
	public GrblDriver() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationComplete(Configuration configuration)
                    throws Exception {
                connect(portName, baud);
            }
        });
	}
	
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
		sendCommand("G28");
		x = y = z= c = 0;
	}
	
	@Override
    public Location getLocation(ReferenceHeadMountable hm) {
        return new Location(LengthUnit.Millimeters, x, y, z, c).add(hm
                .getHeadOffsets());
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value)
            throws Exception {
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
        
		// TODO: Due to a bug (of my creating) in Grbl, C movements are
		// included in the linear movements, and since they are much slower
		// than X, Y movements they end up slowing the whole thing down.
		// So, as a temporary hack, if there is a C move to be made we'll
		// make it first. 
		// Also, since C is so slow in comparison, we just increase it
		// by a factor of 10.
		if (c != this.c && (x != this.x || y != this.y || z != this.z)) {
			moveTo(hm, location.derive(Double.NaN, Double.NaN, Double.NaN, null), speed);
		}
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
			// TODO see above bug note, and remove this when fixed.
			feedRateMmPerMinute *= 10;
			sb.append(String.format(Locale.US, "C%2.2f ", c));
		}
		if (sb.length() > 0) {
			sb.append(String.format(Locale.US, "F%2.2f", feedRateMmPerMinute));
			sendCommand("G1 " + sb.toString());
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
	public void setEnabled(boolean enabled) throws Exception {
		sendCommand("$1000=" + (enabled ? "1" : "0"));
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

	public synchronized void connect(String portName, int baud)
			throws Exception {
		connect(CommPortIdentifier.getPortIdentifier(portName), baud);
	}

	public synchronized void connect(CommPortIdentifier commPortId, int baud)
			throws Exception {
		disconnect();

		if (commPortId.isCurrentlyOwned()) {
			throw new Exception("Port is in use.");
		}
		this.baud = baud;
		serialPort = (SerialPort) commPortId.open(this.getClass().getName(),
				2000);
		serialPort.setSerialPortParams(baud, SerialPort.DATABITS_8,
				SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
		serialPort.enableReceiveTimeout(100);
		if (!serialPort.isReceiveTimeoutEnabled()) {
			throw new Exception("Unable to enable receive timeout.");
		}
		input = serialPort.getInputStream();
		output = serialPort.getOutputStream();

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
			// Wait up to 3 seconds for Grbl to say Hi
			// If we get anything at this point it will have been the settings
			// dump that is sent after reset.
			responses = sendCommand(null, 3000);
		}

		processConnectionResponses(responses);

		for (int i = 0; i < 5 && !connected; i++) {
			responses = sendCommand("$", 5000);
			processConnectionResponses(responses);
		}
		
		if (!connected)  {
			throw new Error(
				String.format("Unable to receive connection response from Grbl. Check your port and baud rate, and that you are running at least version %f of Grbl", 
						minimumRequiredVersion));
		}
		
		if (connectedVersion < minimumRequiredVersion) {
			throw new Error(String.format("This driver requires Grbl version %.2f or higher. You are running version %.2f", minimumRequiredVersion, connectedVersion));
		}
		
		// We are connected to at least the minimum required version now
		// So perform some setup
		
		// Turn off the stepper drivers
		setEnabled(false);
		
		// Reset all axes to 0, in case the firmware was not reset on
		// connect.
		sendCommand("G92 X0 Y0 Z0 C0");
	}
	
	private void processConnectionResponses(List<String> responses) {
		for (String response : responses) {
			if (response.startsWith("$VERSION = ")) {
				String[] versionComponents = response.split(" ");
				connectedVersion = Double.parseDouble(versionComponents[2]);
				connected = true;
				logger.debug(String.format("Connected to Grbl Version: %.2f", connectedVersion));
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
		if (serialPort != null) {
			serialPort.close();
		}
		disconnectRequested = false;
	}

	private List<String> sendCommand(String command) throws Exception {
		return sendCommand(command, -1);
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
	
	public void run() {
		while (!disconnectRequested) {
			String line = readLine().trim();
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
	 * Causes Grbl to block until all commands are complete.
	 * @throws Exception
	 */
	private void dwell() throws Exception {
		sendCommand("G4 P0");
	}

	private List<String> drainResponseQueue() {
		List<String> responses = new ArrayList<String>();
		String response;
		while ((response = responseQueue.poll()) != null) {
			responses.add(response);
		}
		return responses;
	}
	
	private String readLine() {
		StringBuffer line = new StringBuffer();
		try {
			while (true) {
				int ch = readChar();
				if (ch == -1) {
					return null;
				}
				else if (ch == '\n' || ch == '\r') {
					if (line.length() > 0) {
						return line.toString();
					}
				}
				else {
					line.append((char) ch);
				}
			}
		}
		catch (Exception e) {
			logger.error("readLine()", e);
		}
		return null;
	}

	private int readChar() {
		try {
			int ch = -1;
			while (ch == -1 && !disconnectRequested) {
				ch = input.read();
			}
			return ch;
		}
		catch (Exception e) {
			logger.error("readChar()", e);
			return -1;
		}
	}

    @Override
    public Wizard getConfigurationWizard() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPropertySheetConfigurableTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public PropertySheetConfigurable[] getPropertySheetConfigurableChildren() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        // TODO Auto-generated method stub
        return null;
    }
}
