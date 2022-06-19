package org.openpnp.machine.reference.driver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import org.openpnp.util.GcodeServer;
import org.simpleframework.xml.Attribute;

/**
 * A base class for basic TCP based Drivers. Includes functions for connecting,
 * disconnecting, reading and sending lines.
 */
public class TcpCommunications extends ReferenceDriverCommunications {
    @Attribute(required = false)
    protected String ipAddress = "127.0.0.1";

    @Attribute(required = false)
    protected int port = 23;

    @Attribute(required = false)
    protected String name = "TcpCommunications";


    protected Socket clientSocket;
    protected BufferedReader input;
    protected DataOutputStream output;
    protected GcodeServer gcodeServer;
    protected AbstractReferenceDriver driver;

    @Override
    public synchronized void connect() throws Exception {
        disconnect();
        if (ipAddress.equals("GcodeServer")) {
            gcodeServer = new GcodeServer();
            gcodeServer.setDriver(driver);
            port = gcodeServer.getListenerPort();
            clientSocket = new Socket("localhost", port);
        }
        else {
            clientSocket = new Socket(ipAddress,port);
        }
        input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        output = new DataOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public synchronized void disconnect() throws Exception {
        if (clientSocket != null && clientSocket.isBound()) {
            clientSocket.close();
            input = null;
            output = null;
            clientSocket = null;
        }
        if (gcodeServer != null) {
            gcodeServer.shutdown();
            gcodeServer = null;
        }
    }

    @Override
    public String getConnectionName(){
        return "tcp://" + ipAddress + ":" + port;
    }

    @Override
    public int read() throws TimeoutException, IOException {
        try {
            return input.read();
        }
        catch (NullPointerException ex) {
            throw new IOException("Trying to read from a unconnected socket.");
        }
        catch (IOException ex) {
            if (ex.getCause() instanceof SocketTimeoutException) {
                throw new TimeoutException(ex.getMessage());
            }
            throw ex;
        }
    }

    @Override
    public void write(int d) throws IOException {
        output.write(d);
    }

    @Override
    public void writeBytes(byte[] data) throws IOException {
        output.write(data);
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

    public void setDriver(AbstractReferenceDriver driver) {
        this.driver = driver;
    }

    @Override
    public GcodeServer getGcodeServer() {
        return gcodeServer;
    }
}

