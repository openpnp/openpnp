package org.openpnp.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class GcodeServer extends Thread {
    final Map<String, String> commandResponses = new HashMap<>();
    final ServerSocket serverSocket;
    
    
    /**
     * Create a GcodeServer listening on the given port.
     * @param port
     * @throws Exception
     */
    public GcodeServer(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        Thread thread = new Thread(this);
        thread.setDaemon(false);
        thread.start();
    }
    
    /**
     * Create a GcodeServer listening on a random port. The chosen port can be
     * retrived by calling GcodeServer.getListenerPort().
     * @throws Exception
     */
    public GcodeServer() throws Exception {
        this(0);
    }
    
    public int getListenerPort() {
        return serverSocket.getLocalPort();
    }
    
    public void addCommandResponse(String command, String response) {
        commandResponses.put(command, response);
    }
    
    public void shutdown() {
        try {
            serverSocket.close();
        }
        catch (Exception e) {
            
        }
    }
    
    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                new Worker(socket).start();
            }
            catch (Exception e) {
            }
        }
    }
    
    class Worker extends Thread {
        final Socket socket;
        final InputStream input;
        final OutputStream output;
        
        public Worker(Socket socket) throws Exception {
            this.socket = socket;
            input = socket.getInputStream();
            output = socket.getOutputStream();
        }
        
        String read() throws Exception {
            StringBuffer line = new StringBuffer();
            while (true) {
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
        }
        
        
        void write(String s) throws Exception {
            output.write((s + "\n").getBytes("UTF8"));
        }
        
        public void run() {
            while (true) {
                try {
                    String input = read().trim();
                    String response = commandResponses.get(input);
                    if (response != null) {
                        write(response);
                    }
                    else {
                        write("error:unknown command");
                    }
                }
                catch (Exception e) {
                    break;
                }
            }
            try {
                input.close();
            }
            catch (Exception e) {
            }
            try {
                output.close();
            }
            catch (Exception e) {
            }
            try {
                socket.close();
            }
            catch (Exception e) {
            }
        }
    }
    
    
    public static void main(String[] args) throws Exception {
        GcodeServer server = new GcodeServer();
    }
}
