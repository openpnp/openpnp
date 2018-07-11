package org.openpnp.machine.reference.driver;

import java.io.Closeable;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.DataOutputStream;
import java.util.concurrent.TimeoutException;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCommunications;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.machine.reference.driver.wizards.AbstractTcpDriverConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * A base class for basic TCP based Drivers. Includes functions for connecting,
 * disconnecting, reading and sending lines.
 */
public class TcpCommunications implements Closeable, ReferenceCommunications {
    @Attribute(required = false)
    protected String ipAddress = "127.0.0.1";

    @Attribute(required = false)
    protected int port = 23;

    @Attribute(required = false)
    protected String name = "TcpCommunications";


    protected Socket clientSocket;
    protected BufferedReader input;
    protected DataOutputStream output;

    public synchronized void connect() throws Exception {
        disconnect();
        clientSocket = new Socket(ipAddress,port);
        input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        output = new DataOutputStream(clientSocket.getOutputStream());
    }

    public synchronized void disconnect() throws Exception {
        if (clientSocket != null && clientSocket.isBound()) {
            clientSocket.close();
            input = null;
            output = null;
            clientSocket = null;
        }
    }

    public String getConnectionName(){
        return "tcp://" + ipAddress + ":" + port;
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
            output.write(lineEnding.getBytes());
        }
        catch (IOException ex) {
            throw ex;
        }
    }

    public void close() throws IOException {
        try {
            disconnect();
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ip) {
        this.ipAddress = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
}

