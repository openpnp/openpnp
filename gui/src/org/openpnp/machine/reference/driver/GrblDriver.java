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
	<Configuration>
		<Port name="/dev/tty.usbserial-A9007LmZ" baud="38400" />
		<Settings>
			<Setting name="$0" value="56.338" />
			<Setting name="$1" value="56.338" />
			<Setting name="$2" value="56.338" />
			<Setting name="$3" value="10" />
		</Settings>
	</Configuration>
</pre>
 */
public class GrblDriver implements ReferenceDriver, Runnable {
	private double x, y, z, c;

	private CommPortIdentifier commPortId;
	private SerialPort serialPort;
	private int baud;
	private InputStream input;
	private OutputStream output;
	private Thread readerThread;
	private boolean disconnectRequested;
	private Object commandLock = new Object();
	private String lastResponse;
	private boolean connected;
	private boolean configured;

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
		}
	}

	@Override
	public void home(ReferenceHead head) throws Exception {
		moveTo(head, 0, 0, 0, 0);
	}

	@Override
	public void moveTo(ReferenceHead head, double x, double y, double z, double c)
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
			sendCommand("G1" + sb.toString());
		}
		this.x = x;
		this.y = y;
		this.z = z;
		this.c = c;
	}

	@Override
	public void pick(ReferenceHead head, Part part) throws Exception {
		sendCommand("M4");
	}

	@Override
	public void place(ReferenceHead head) throws Exception {
		sendCommand("M5");
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
		
		readerThread = new Thread(this);
		readerThread.start();
	}

	public synchronized void disconnect() {
		disconnectRequested = true;
		connected = false;
		configured = false;
		
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
			String line = readLine();
			System.out.println(line);
//			if (!connected) {
//				if (line.startsWith("Grbl")) {
//					connected = true;
//					System.out.println("Connect complete");
//				}
//			}
//			else {
				synchronized (commandLock) {
					lastResponse = line;
					commandLock.notify();
				}
//			}
		}
	}
	
	private void sendCommand(String command) throws Exception {
		System.out.println(command);
		output.write(command.getBytes());
		output.write("\r\n".getBytes());
		String response;
		synchronized (commandLock) {
			commandLock.wait();
			response = lastResponse;
		}
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
