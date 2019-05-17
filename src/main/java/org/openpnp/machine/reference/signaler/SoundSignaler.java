package org.openpnp.machine.reference.signaler;

import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.signaler.wizards.SoundSignalerConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractSignaler;
import org.simpleframework.xml.Attribute;

/**
 * The SoundSignaler can acoustically indicate certain states of the machine or a job processor like errors or
 * notifications to the user
 */
public class SoundSignaler extends AbstractSignaler {

    @Attribute
    protected boolean enableErrorSound;

    @Attribute
    protected boolean enableFinishedSound;

    private ClassLoader classLoader = getClass().getClassLoader();

    private void playSound(String filename) {
        try {
            AudioInputStream audioInputStream;

            // Check if there is a file in the configuration directory under sounds overriding the resource files
            File overrideFile = new File(Configuration.get().getConfigurationDirectory(), filename);

            if(overrideFile.exists() && !overrideFile.isDirectory()) {
                audioInputStream = AudioSystem.getAudioInputStream(overrideFile);
            } 
            else {
                audioInputStream = AudioSystem.getAudioInputStream(classLoader.getResourceAsStream(filename));
            }

            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start(); 
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void playErrorSound() {
        // "A generic "wrong" sound or an error." by Aucustic Lucario, CC
        // https://www.freesound.org/people/Autistic%20Lucario/sounds/142608/
        if(enableErrorSound) {
            playSound("sounds/error.wav");
        }
    }

    public void playSuccessSound() {
        // "A good result sound effect. Made with FL Studio." by unadamlar, CC
        // https://www.freesound.org/people/unadamlar/sounds/341985/
        if(enableFinishedSound) {
            playSound("sounds/success.wav");
        }
    }

    @Override
    public void signalMachineState(AbstractMachine.State state) {
        switch (state) {
            case ERROR: {
                playErrorSound();
                break;
            }
        }
    }

    @Override
    public void signalJobProcessorState(AbstractJobProcessor.State state) {
        switch (state) {
            case ERROR: {
                playErrorSound();
                break;
            }

            case FINISHED: {
                playSuccessSound();
                break;
            }
        }
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new SoundSignalerConfigurationWizard(this);
    }

    public boolean isEnableErrorSound() {
        return enableErrorSound;
    }

    public void setEnableErrorSound(boolean enableErrorSound) {
        this.enableErrorSound = enableErrorSound;
    }

    public boolean isEnableFinishedSound() {
        return enableFinishedSound;
    }

    public void setEnableFinishedSound(boolean enableFinishedSound) {
        this.enableFinishedSound = enableFinishedSound;
    }
}
