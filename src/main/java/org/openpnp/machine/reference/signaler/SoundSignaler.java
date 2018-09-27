package org.openpnp.machine.reference.signaler;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.model.Configuration;
import org.openpnp.spi.PropertySheetHolder;
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
            InputStream soundStream;

            // Check if there is a file in the configuration directory under sounds overriding the resource files
            File overrideFile = new File(Configuration.get().getConfigurationDirectory(), filename);

            if(overrideFile.exists() && !overrideFile.isDirectory()) {
                soundStream = new FileInputStream(overrideFile);
            } else {
                soundStream = classLoader.getResourceAsStream(filename);
            }

            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(soundStream);
            clip.open(inputStream);
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
}
