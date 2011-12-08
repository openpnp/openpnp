package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import org.openpnp.BoardLocation;
import org.openpnp.Configuration;

@SuppressWarnings("serial")
public class OrientBoardWizard extends JPanel implements Wizard {
	private WizardContainer wizardContainer;
	
	public OrientBoardWizard(BoardLocation board, Configuration configuration) {
		setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		FlowLayout flowLayout = (FlowLayout) panel.getLayout();
		flowLayout.setAlignment(FlowLayout.RIGHT);
		add(panel, BorderLayout.SOUTH);
		
		JButton btnNewButton_2 = new JButton("Previous");
		panel.add(btnNewButton_2);
		
		JButton btnNewButton_1 = new JButton("Next");
		panel.add(btnNewButton_1);
		
		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		panel.add(separator);
		
		JButton btnNewButton = new JButton("Cancel");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				wizardContainer.wizardCompleted(OrientBoardWizard.this);
			}
		});
		panel.add(btnNewButton);
		
		JPanel panel_1 = new JPanel();
		add(panel_1, BorderLayout.CENTER);
		panel_1.setLayout(new BorderLayout(0, 0));
		
		JTextPane txtpnThisWizardWill = new JTextPane();
		txtpnThisWizardWill.setOpaque(false);
		txtpnThisWizardWill.setEditable(false);
		txtpnThisWizardWill.setText("This wizard will allow you to set the position and orientation of a board by precisely finding the positions of two or more elements on the board. \n\nIdeally, you should find the positions of two elements that are on opposite corners of the board. If that is not possible, try to use two elements that are as far apart from each other as possible.");
		panel_1.add(txtpnThisWizardWill);
	}
	
	@Override
	public void setWizardContainer(WizardContainer wizardContainer) {
		this.wizardContainer = wizardContainer;
	}
	
	@Override
	public JPanel getWizardPanel() {
		return this;
	}
	
	@Override
	public String getWizardName() {
		return "Set Board Location";
	}
}
