package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.BoxLayout;

import org.openpnp.Main;

import java.awt.Component;

@SuppressWarnings("serial")
public class AboutDialog extends JDialog {

	private final JPanel contentPanel = new JPanel();

	public AboutDialog(Frame frame) {
		super(frame, true);
		setTitle("About OpenPnP");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 347, 360);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		{
			JLabel lblOpenpnp = new JLabel("OpenPnP");
			lblOpenpnp.setAlignmentX(Component.CENTER_ALIGNMENT);
			lblOpenpnp.setFont(new Font("Lucida Grande", Font.BOLD, 32));
			contentPanel.add(lblOpenpnp);
		}
		{
			JLabel lblCopyright = new JLabel("Copyright © 2011, 2012 Jason von Nieda");
			lblCopyright.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
			lblCopyright.setAlignmentX(Component.CENTER_ALIGNMENT);
			contentPanel.add(lblCopyright);
		}
		{
			JLabel lblVersion = new JLabel("Version: " + Main.getVersion());
			lblVersion.setFont(new Font("Lucida Grande", Font.PLAIN, 10));
			lblVersion.setAlignmentX(Component.CENTER_ALIGNMENT);
			contentPanel.add(lblVersion);
		}
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						setVisible(false);
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
	}

}
