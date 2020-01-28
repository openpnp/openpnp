package org.openpnp.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class GcodeServer extends Thread {
    final Socket socket;
    final InputStream input;
    final OutputStream output;
    
    boolean n1Vac = false;
    
    public GcodeServer(Socket socket) throws Exception {
        this.socket = socket;
        input = socket.getInputStream();
        output = socket.getOutputStream();
    }
    
    public String read() throws Exception {
        StringBuffer line = new StringBuffer();
        while (true) {
            int ch = input.read();
            if (ch == -1) {
                return null;
            }
            else if (ch == '\n' || ch == '\r') {
                if (line.length() > 0) {
                    System.out.println("<<< " + line.toString());
                    return line.toString();
                }
            }
            else {
                line.append((char) ch);
            }
        }
    }
    
    
    public void write(String s) throws Exception {
        output.write((s + "\n").getBytes("UTF8"));
        System.out.println(">>> " + s);
    }
    
    public void run() {
        while (true) {
            try {
                String input = read().trim();
                if (input.equals("VACVacN1ACTUATEtrue")) {
                    n1Vac = true;
                }
                else if (input.equals("VACVacN1ACTUATEfalse")) {
                    n1Vac = false;
                }
                else if (input.equals("VACVacN1READ")) {
                    write(n1Vac ? "vacuum:75" : "vacuum:0");                    
                }
                write("ok");
            }
            catch (Exception e) {
                e.printStackTrace();
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
    
    public static void main(String[] args) throws Exception {
        final int port = 19367;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("GcodeServer listening on port " + port);
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Accepted connection from " + socket.getRemoteSocketAddress());
            new GcodeServer(socket).start();
        }
    }
}
