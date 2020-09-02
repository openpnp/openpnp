package org.openpnp.tcpServer;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import org.openpnp.model.Configuration;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Machine;
import org.openpnp.gui.JobPanel;
import org.openpnp.gui.JogControlsPanel;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.driver.GcodeDriver;

public class ClientProcessor implements Runnable {

	private Socket sock;
	private PrintWriter writer = null;
	private InputStream reader = null;
	private JobPanel jobPanel;
	private JogControlsPanel jogControlsPanel;

	public ClientProcessor(Socket pSock, JobPanel jobPanel, JogControlsPanel jogControlsPanel) {
		sock = pSock;
		this.jobPanel = jobPanel;
		this.jogControlsPanel = jogControlsPanel;
	}

	// Le traitement lancé dans un thread séparé
	public void run() {
		Thread.currentThread().setName("TCPClient");
		System.out.println("new TCP client ");

		try {
			writer = new PrintWriter(sock.getOutputStream());
			reader = sock.getInputStream();

			int readen = reader.read();
			while (readen != -1) {

				MessageType messageType = MessageType.values()[readen];
				switch (messageType) {
				case HOME: {
					Machine machine = Configuration.get().getMachine();
					try {
						machine.home();
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
				case M999: {
					ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();
					GcodeDriver driver = (GcodeDriver) machine.getDriver();
					try {
						driver.sendCommand("M999", 500);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
				case START:
					jobPanel.startPauseResumeJobAction.actionPerformed(null);
					break;
				case STOP:
					jobPanel.stopJobAction.actionPerformed(null);
					break;
				case PARK_XY:
					jogControlsPanel.xyParkAction.actionPerformed(null);
					break;
				case PARK_Z:
					jogControlsPanel.zParkAction.actionPerformed(null);
					break;
				case MOVE_XP_01_MIL:
					jogControlsPanel.jogValue(0.1, 0, 0, 0);
					break;
				case MOVE_XP_1_MIL:
					jogControlsPanel.jogValue(1, 0, 0, 0);
					break;
				case MOVE_XP_10_MIL:
					jogControlsPanel.jogValue(10, 0, 0, 0);
					break;
				case MOVE_XM_01_MIL:
					jogControlsPanel.jogValue(-0.1, 0, 0, 0);
					break;
				case MOVE_XM_1_MIL:
					jogControlsPanel.jogValue(-1, 0, 0, 0);
					break;
				case MOVE_XM_10_MIL:
					jogControlsPanel.jogValue(-10, 0, 0, 0);
					break;
				case MOVE_YP_01_MIL:
					jogControlsPanel.jogValue(0.1, 0, 0, 0);
					break;
				case MOVE_YP_1_MIL:
					jogControlsPanel.jogValue(1, 0, 0, 0);
					break;
				case MOVE_YP_10_MIL:
					jogControlsPanel.jogValue(10, 0, 0, 0);
					break;
				case MOVE_YM_01_MIL:
					jogControlsPanel.jogValue(-0.1, 0, 0, 0);
					break;
				case MOVE_YM_1_MIL:
					jogControlsPanel.jogValue(-1, 0, 0, 0);
					break;
				case MOVE_YM_10_MIL:
					jogControlsPanel.jogValue(-10, 0, 0, 0);
					break;
				case MOVE_ZP_01_MIL:
					jogControlsPanel.jogValue(0.1, 0, 0, 0);
					break;
				case MOVE_ZP_1_MIL:
					jogControlsPanel.jogValue(1, 0, 0, 0);
					break;
				case MOVE_ZP_10_MIL:
					jogControlsPanel.jogValue(10, 0, 0, 0);
					break;
				case MOVE_ZM_01_MIL:
					jogControlsPanel.jogValue(-0.1, 0, 0, 0);
					break;
				case MOVE_ZM_1_MIL:
					jogControlsPanel.jogValue(-1, 0, 0, 0);
					break;
				case MOVE_ZM_10_MIL:
					jogControlsPanel.jogValue(-10, 0, 0, 0);
					break;

				default:
					break;
				}

//					writer.write(toSend);
				// writer.flush();
				readen = reader.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.err.println("\nSocket closed");

	}

}
