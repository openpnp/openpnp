package org.openpnp.machine.reference.vision;

import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class OpenCvVisionProviderDebugger extends JFrame {
	private JLabel image4;
	private JLabel image1;
	private JLabel image2;
	private JLabel image3;
	
	public OpenCvVisionProviderDebugger() {
		getContentPane().setLayout(new GridLayout(2, 2, 0, 0));
		
		image1 = new JLabel("");
		getContentPane().add(image1);
		
		image2 = new JLabel("");
		getContentPane().add(image2);
		
		image3 = new JLabel("");
		getContentPane().add(image3);
		
		image4 = new JLabel("");
		getContentPane().add(image4);
		
		setSize(1024, 768);
		setVisible(true);
	}

	public JLabel getImage4() {
		return image4;
	}

	public JLabel getImage1() {
		return image1;
	}

	public JLabel getImage2() {
		return image2;
	}

	public JLabel getImage3() {
		return image3;
	}
}
