package org.openpnp.machine.neoden4.wizards;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;

import org.openpnp.Translations;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.neoden4.Neoden4Signaler;
import org.openpnp.spi.base.AbstractJobProcessor.State;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class Neoden4SignalerConfigurationWizard  extends AbstractConfigurationWizard {
    private final Neoden4Signaler signaler;
    private JCheckBox chckbxError;
    private JCheckBox chckbxSuccess;

    public Neoden4SignalerConfigurationWizard(Neoden4Signaler actuator) {
        this.signaler = actuator;
        createUi();
    }
    private void createUi() {
        contentPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,},
            new RowSpec[] {
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,
                FormSpecs.RELATED_GAP_ROWSPEC,
                FormSpecs.DEFAULT_ROWSPEC,}));
        
        chckbxError = new JCheckBox(Translations.getStringOrDefault(
                "Neoden4SignalerConfigurationWizard.PlaySoundOnErrorChkBox.text", "Play sound on error?"));
        contentPanel.add(chckbxError, "2, 2");
        
        JButton testError = new JButton();
        testError.setAction(testErrorAction);
        contentPanel.add(testError, "4, 2");
        
        chckbxSuccess = new JCheckBox(Translations.getStringOrDefault(
                "Neoden4SignalerConfigurationWizard.PlaySoundOnCompletionChkBox.text",
                "Play sound on completion?"));
        contentPanel.add(chckbxSuccess, "2, 4");

        JButton testFinished = new JButton();
        testFinished.setAction(testFinishedAction);
        contentPanel.add(testFinished, "4, 4");
    }
    
    private Action testErrorAction = new AbstractAction(Translations.getStringOrDefault(
            "Neoden4SignalerConfigurationWizard.Action.TestErrorSound",
            "Test error sound")) {
		@Override
		public void actionPerformed(ActionEvent arg0) {
        	signaler.signalJobProcessorState(State.ERROR);
        }
    };

    
    private Action testFinishedAction = new AbstractAction(Translations.getStringOrDefault(
            "Neoden4SignalerConfigurationWizard.Action.TestFinishedSound",
            "Test finished sound")) {
		@Override
		public void actionPerformed(ActionEvent arg0) {
        	signaler.signalJobProcessorState(State.FINISHED);
        }
    };


    @Override
    public void createBindings() {
        addWrappedBinding(signaler, "enableErrorSound", chckbxError, "selected");
        addWrappedBinding(signaler, "enableFinishedSound", chckbxSuccess, "selected");
    }
}