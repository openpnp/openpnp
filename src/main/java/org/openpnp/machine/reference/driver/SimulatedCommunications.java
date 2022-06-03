package org.openpnp.machine.reference.driver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import org.openpnp.spi.Driver;
import org.openpnp.util.GcodeServer;
import org.pmw.tinylog.Logger;

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

    @Override
    public synchronized void connect() throws Exception {
        disconnect();
        if (gcodeServer == null) {
            try {
                gcodeServer = new GcodeServer();
            }
            catch (Exception e) {
                Logger.warn(e);
            }
        }
        if (gcodeServer != null) {
            gcodeServer.setDriver(driver);
        }
        clientSocket = new Socket("localhost", getGcodeServer().getListenerPort());
        input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        output = new DataOutputStream(clientSocket.getOutputStream());
    }

    @Override
    public synchronized void disconnect() throws Exception {
        if (clientSocket != null && clientSocket.isBound()) {
            clientSocket.close();
            input.close();
            output.close();
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
        GcodeServer server = gcodeServer; // prevent race by taking a copy
        return "simulated: "+(server == null ? "off" : "port "+server.getListenerPort());
    }

    @Override
    public GcodeServer getGcodeServer() {
        return gcodeServer;
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

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    @Override
    public void writeBytes(byte[] data) throws IOException {
        output.write(data, 0, data.length);
    }
}

