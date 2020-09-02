package org.openpnp.tcpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.openpnp.gui.JobPanel;
import org.openpnp.gui.JogControlsPanel;

public class TcpServer {
	  private ServerSocket server = null;
	  
	public boolean isRunning = true;

	public TcpServer(JobPanel jobPanel, JogControlsPanel jogControlsPanel) {

		int port = 42;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			System.err.println("Le port " + port + " est déjà utilisé ! ");
		}
		
		// Toujours dans un thread à part vu qu'il est dans une boucle infinie
		Thread t = new Thread(new Runnable() {
			public void run() {
				while (isRunning == true) {

					try {
						// On attend une connexion d'un client
						Socket client = server.accept();

						// Une fois reçue, on la traite dans un thread séparé
						System.out.println("Connexion cliente reçue.");
						ClientProcessor clientProcessor = new ClientProcessor(client, jobPanel, jogControlsPanel);
						Thread t = new Thread(clientProcessor);
						t.start();

					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				try {
					server.close();
				} catch (IOException e) {
					e.printStackTrace();
					server = null;
				}
			}
		});

		t.start();
	}
}
