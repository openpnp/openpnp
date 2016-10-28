package org.openpnp.machine.reference.signaler;

import java.io.IOException;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.spi.base.AbstractMachine;
import org.openpnp.spi.base.AbstractSignaler;
import org.simpleframework.xml.Attribute;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

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
            AudioStream audioStream = new AudioStream(classLoader.getResourceAsStream(filename));
            AudioPlayer.player.start(audioStream);
        } catch (IOException e) {
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
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return new PropertySheetHolder[0];
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[0];
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[0];
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }
}
