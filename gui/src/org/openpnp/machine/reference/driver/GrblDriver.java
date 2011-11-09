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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.Part;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
<pre>
{@code
<Configuration>
	<Port name="/dev/tty.usbserial-A9007LmZ" baud="38400" />
	<Settings>
		<Setting name="$0" value="56.338" />
		<Setting name="$1" value="56.338" />
		<Setting name="$2" value="56.338" />
		<Setting name="$3" value="10" />
	</Settings>
</Configuration>
}
</pre>
	TODO Consider adding some type of heartbeat to the firmware.  
 */
public class GrblDriver implements ReferenceDriver, Runnable {
	private static final double minimumRequiredVersion = 0.75;
	
	private double x, y, z, c;

	private CommPortIdentifier commPortId;
	private SerialPort serialPort;
	private int baud;
	private InputStream input;
	private OutputStream output;
	private Thread readerThread;
	private boolean disconnectRequested;
	private Object commandLock = new Object();
	
	private boolean connected;
	private double connectedVersion;
	
	private Queue<String> responseQueue = new ConcurrentLinkedQueue<String>();
	

	@Override
	public void configure(Node n) throws Exception {
		XPath xpath = XPathFactory.newInstance().newXPath();

		Node portNode = (Node) xpath.evaluate("Port", n, XPathConstants.NODE);
		
		String portName = Configuration.getAttribute(portNode, "name");
		int portBaud = Integer.parseInt(Configuration.getAttribute(portNode, "baud"));
		
		NodeList nodes = (NodeList) xpath.evaluate("Settings/Setting", n, XPathConstants.NODESET);

		for (int i = 0; i < nodes.getLength(); i++) {
			Node settingNode = nodes.item(i);
			System.out.println(Configuration.getAttribute(settingNode, "name"));
		}
		
		connect(portName, portBaud);
	}

	@Override
	public void prepareJob(Configuration configuration, Job job)
			throws Exception {
	}

	@Override
	public void actuate(ReferenceHead head, int index, boolean on)
			throws Exception {
		if (index == 0) {
			sendCommand(on ? "M8" : "M9");
			dwell();
		}
	}
	
	@Override
	public void home(ReferenceHead head, double feedRateMmPerMinute) throws Exception {
		moveTo(head, 0, 0, 0, 0, feedRateMmPerMinute);
	}
	
	@Override
	public void moveTo(ReferenceHead head, double x, double y, double z, double c, double feedRateMmPerMinute)
			throws Exception {
		StringBuffer sb = new StringBuffer();
		if (x != this.x) {
			sb.append(String.format("X%2.2f", x));
		}
		if (y != this.y) {
			sb.append(String.format("Y%2.2f", y));
		}
		if (z != this.z) {
			sb.append(String.format("Z%2.2f", z));
		}
		if (c != this.c) {
			sb.append(String.format("C%2.2f", c));
		}
		if (sb.length() > 0) {
			sb.append(String.format("F%2.2f", feedRateMmPerMinute));
			sendCommand("G1" + sb.toString());
			dwell();
		}
		this.x = x;
		this.y = y;
		this.z = z;
		this.c = c;
	}
	
	@Override
	public void setEnabled(boolean enabled) throws Exception {
		sendCommand("!1=" + (enabled ? "1" : "0"));
	}

	@Override
	public void pick(ReferenceHead head, Part part) throws Exception {
		sendCommand("M4");
		dwell();
	}

	@Override
	public void place(ReferenceHead head) throws Exception {
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
		this.commPortId = commPortId;
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
			if (response.startsWith("!0 = ")) {
				String[] versionComponents = response.split(" ");
				connectedVersion = Double.parseDouble(versionComponents[2]);
				connected = true;
				System.out.println(String.format("Connected to Grbl Version: %.2f", connectedVersion));
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
			e.printStackTrace();
		}
		if (serialPort != null) {
			serialPort.close();
		}
		disconnectRequested = false;
	}

	public void run() {
		while (!disconnectRequested) {
			String line = readLine().trim();
			System.out.println(line);
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

	private List<String> sendCommand(String command) throws Exception {
		return sendCommand(command, -1);
	}
	
	private List<String> sendCommand(String command, long timeout) throws Exception {
		if (command != null) {
			System.out.println(command);
			output.write(command.getBytes());
			output.write("\n".getBytes());
		}
		synchronized (commandLock) {
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
			e.printStackTrace();
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
			e.printStackTrace();
			return -1;
		}
	}
}
