package org.openpnp.machine.reference.driver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import org.openpnp.machine.reference.driver.ReferenceDriverCommunications.LineEndingType;
import org.openpnp.spi.Driver;
import org.openpnp.util.GcodeServer;
import org.simpleframework.xml.Attribute;

/**
 * A base class for basic TCP based Drivers. Includes functions for connecting,
 * disconnecting, reading and sending lines.
 */
public class SimulatedCommunications extends ReferenceDriverCommunications {
    protected Socket clientSocket;
    protected BufferedReader input;
    protected DataOutputStream output;

    protected GcodeServer gcodeServer;
    private Driver driver;

    public synchronized void connect() throws Exception {
        disconnect();
        clientSocket = new Socket("localhost", getGcodeServer().getListenerPort());
        input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        output = new DataOutputStream(clientSocket.getOutputStream());
    }

    public synchronized void disconnect() throws Exception {
        if (clientSocket != null && clientSocket.isBound()) {
            clientSocket.close();
            input = null;
            output = null;
            clientSocket = null;
            gcodeServer = null;
        }
    }

    public String getConnectionName(){
        return "simulated: "+(gcodeServer == null ? "off" : "port "+gcodeServer.getListenerPort());
    }

    public GcodeServer getGcodeServer() throws Exception {
        if (gcodeServer == null) {
            gcodeServer = new GcodeServer();
        }
        gcodeServer.setDriver(driver);
        return gcodeServer;
    }

    /**
     * Read a line from the socket. Blocks for the default timeout. If the read times out a
     * TimeoutException is thrown. Any other failure to read results in an IOExeption;
     * 
     * @return
     * @throws TimeoutException
     * @throws IOException
     */
    public String readLine() throws TimeoutException, IOException {
        StringBuffer line = new StringBuffer();
        while (true) {
            try {
                int ch = input.read();
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
            catch (IOException ex) {
                if (ex.getCause() instanceof SocketTimeoutException) {
                    throw new TimeoutException(ex.getMessage());
                }
                throw ex;
            }
        }
    }

    public void writeLine(String data) throws IOException
    {
        try {
            output.write(data.getBytes());
            output.write(getLineEndingType().getLineEnding().getBytes());
        }
        catch (IOException ex) {
            throw ex;
        }
    }
    
    public int read() throws TimeoutException, IOException {
        try {
            return input.read();
        }
        catch (IOException ex) {
            if (ex.getCause() instanceof SocketTimeoutException) {
                throw new TimeoutException(ex.getMessage());
            }
            throw ex;
        }
    }
    
    public void write(int d) throws IOException {
        output.write(d);
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }
}

