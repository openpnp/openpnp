/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openpnp.machine.reference.simulator;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import org.openpnp.machine.reference.simulator.Machine.Movable;

/**
 *
 * @author jason
 */
public class Server implements Runnable {
    private final Machine machine;
    private ServerSocket socket;
    
    public Server(Machine machine) throws Exception {
        this.machine = machine;
        
        socket = new ServerSocket(9037);
        
        Thread t = new Thread(this);
        t.setDaemon(true);
        t.start();
    }
    
    public void run() {
        while (!Thread.interrupted()) {
            try {
                Socket clientSocket = socket.accept();
                System.out.println("Connection from " + clientSocket.getInetAddress());
                try {
                    DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                    PrintStream out = new PrintStream(clientSocket.getOutputStream());
                    String line;
                    while ((line = in.readLine()) != null) {
                        line = line.trim();
                        if (line.length() == 0) {
                            continue;
                        }
                        String[] parts = line.split(",");
                        System.out.println(line);
                        if (parts[0].equals("m")) {
                            // m,Camera|Actuator|Nozzle1|Nozzle2,x.xx,y.yy,z.zz,c.cc
                            Movable movable = Movable.valueOf(parts[1]);
                            double x = getDouble(parts[2]);
                            double y = getDouble(parts[3]);
                            double z = getDouble(parts[4]);
                            double c = getDouble(parts[5]);
                            machine.moveTo(movable, x, y, z, c);
                            out.print("ok\n");
                        }
                        else if (parts[0].equals("a")) {
                            // a,true|false
                            boolean on = parts[1].equals("true");
                            machine.actuate(on);
                            out.print("ok\n");
                        }
                        else if (parts[0].equals("p")) {
                            // m,Nozzle1|Nozzle2,true|false
                            Movable movable = Movable.valueOf(parts[1]);
                            boolean pick = parts[2].equals("true");
                            if (pick) {
                                machine.pick(movable);
                            }
                            else {
                                machine.place(movable);
                            }
                            out.print("ok\n");
                        }
                        else if (parts[0].equals("h")) {
                            // h
                            machine.home();
                            out.print("ok\n");
                        }
                    }
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private double getDouble(String s) {
        if (s.equals("NaN")) {
            return Double.NaN;
        }
        return Double.valueOf(s);
    }
}
