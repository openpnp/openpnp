package org.openpnp.machine.neoden4;

import javax.swing.SwingUtilities;

import org.openpnp.Translations;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.neoden4.wizards.Neoden4SignalerConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Driver;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.spi.base.AbstractSignaler;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

public class Neoden4Signaler extends AbstractSignaler implements Runnable {

    @Attribute
    protected boolean enableErrorSound;

    @Attribute
    protected boolean enableFinishedSound;

	private boolean playError = false;
	private boolean playSuccess = false;
	private boolean lastPlayError = false;
	private boolean lastPlaySuccess = false;
    
	public Neoden4Signaler() {
		Thread thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}

	@Override
	public void run() {
		// Starting interval between signals
		int sleepTime = 10000;
		
		while (!Thread.interrupted()) {
			try {	
				if (playError) {
					if (lastPlayError != playError) {
						// If triggered show dialog
						lastPlayError = playError;
						sleepTime = 10000;

						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								MessageBoxes.infoBox(Translations.getString("CommonPhrases.clickOkToConfirm"), //$NON-NLS-1$
										Translations.getString("CommonPhrases.jobError")); //$NON-NLS-1$
								playError = false;
								lastPlayError = false;
							}
						});
					}
					// Decrease interval between signals
					sleepTime /= 1.2;
					this.playSound(2, 250);
					Thread.sleep(sleepTime);
				} 
				else if (playSuccess) {
					if (lastPlaySuccess != playSuccess) {
						lastPlaySuccess = playSuccess;
						sleepTime = 10000;

						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								MessageBoxes.infoBox(Translations.getString("CommonPhrases.clickOkToConfirm"), //$NON-NLS-1$
										Translations.getString("CommonPhrases.jobSuccess")); //$NON-NLS-1$
								playSuccess = false;
								lastPlaySuccess = false;
							}
						});
					}
					sleepTime /= 1.2;
					this.playSound(3, 30);
					Thread.sleep(sleepTime);
				}
			} 
			catch (Exception e) {
				e.printStackTrace();
			}
		}

		Logger.debug("Signaler " + getName() + " thread " + Thread.currentThread().getId() + " bye-bye.");
	}
	
	private NeoDen4Driver getNeoden4Driver() {
		NeoDen4Driver driver = null;

		for (Driver d : Configuration.get().getMachine().getDrivers()) {
			if (d instanceof NeoDen4Driver) {
				driver = (NeoDen4Driver) d;
				break;
			}
		}
		return driver;
	}
	
	private void playSound(int times, int delay) throws Exception {
		NeoDen4Driver neoden4Driver = getNeoden4Driver();

		for (int i = 0; i < times; i++) {
			neoden4Driver.setBuzzer(true);
			Thread.sleep(delay);
			neoden4Driver.setBuzzer(false);
			Thread.sleep(delay);
		}
	}

    @SuppressWarnings("incomplete-switch")
    @Override
    public void signalJobProcessorState(AbstractJobProcessor.State state) {
        switch (state) {
            case ERROR: {
                playError = true;
                break;
            }

            case FINISHED: {
                playSuccess = true;
                break;
            }
        }
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new Neoden4SignalerConfigurationWizard(this);
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