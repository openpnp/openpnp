package org.openpnp.machine.photon.sheets.gui;

import java.awt.*;

import javax.swing.*;
import java.awt.event.ActionEvent;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.FormSpecs;

public class ProgramFeederSlotWizard extends JDialog {

	private final JPanel wizardSteps;
	private static final String REMOVE_FEEDERS_PANEL = "Remove Feeders";
	private static final String UPDATE_FLOORS_PANEL = "Update Floors";
	private final RemoveAllFeedersStep removeAllFeedersStep;
	private final FeederSlotUpdateStep feederSlotUpdateStep;
	private final JButton wizardButton;

	public ProgramFeederSlotWizard() {
		setTitle("Program Feeder Slot Wizard");
		setBounds(100, 100, 450, 300);
		
		JPanel buttonsPanel = new JPanel();
		getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
		buttonsPanel.setLayout(new FormLayout(new ColumnSpec[] {
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("4dlu:grow"),
				FormSpecs.RELATED_GAP_COLSPEC,
				FormSpecs.DEFAULT_COLSPEC,
				FormSpecs.RELATED_GAP_COLSPEC,},
			new RowSpec[] {
				FormSpecs.RELATED_GAP_ROWSPEC,
				RowSpec.decode("29px"),
				FormSpecs.RELATED_GAP_ROWSPEC,}));

		wizardButton = new JButton("Next");
		buttonsPanel.add(wizardButton, "4, 2, left, top");
		wizardButton.addActionListener(wizardButtonAction);

		wizardSteps = new JPanel();
		getContentPane().add(wizardSteps, BorderLayout.CENTER);
		wizardSteps.setLayout(new CardLayout(0, 0));

		removeAllFeedersStep = new RemoveAllFeedersStep();
		feederSlotUpdateStep = new FeederSlotUpdateStep();

		wizardSteps.add(removeAllFeedersStep, REMOVE_FEEDERS_PANEL);
		wizardSteps.add(feederSlotUpdateStep, UPDATE_FLOORS_PANEL);
	}

	@Override
	public void dispose() {
		super.dispose();
		feederSlotUpdateStep.stopThread();
	}

	private final Action wizardButtonAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			ProgramFeederSlotWizard wizard = ProgramFeederSlotWizard.this;

			CardLayout wizardStepsLayout = (CardLayout) wizardSteps.getLayout();
			if(removeAllFeedersStep.isVisible()) {
				wizardStepsLayout.show(wizardSteps, UPDATE_FLOORS_PANEL);
				wizardButton.setText("Finish");
				feederSlotUpdateStep.startThread();
			} else if(feederSlotUpdateStep.isVisible()) {
				wizard.setVisible(false);
				wizard.dispose();
			}
		}
	};

}
