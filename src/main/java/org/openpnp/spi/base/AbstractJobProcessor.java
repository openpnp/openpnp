package org.openpnp.spi.base;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.Signaler;

public abstract class AbstractJobProcessor implements JobProcessor {

    public enum State {
        STOPPED,
        RUNNING,
        ERROR,
        FINISHED
    }

    public static interface Retryable {
        void retry() throws Exception;
    }

    protected List<TextStatusListener> textStatusListeners = new ArrayList<>();

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return null;
    }

    @Override
    public void addTextStatusListener(TextStatusListener listener) {
        if (!this.textStatusListeners.contains(listener)) {
            this.textStatusListeners.add(listener);
        }
    }

    @Override
    public void removeTextStatusListener(TextStatusListener listener) {
        this.textStatusListeners.remove(listener);
    }

    protected void fireTextStatus(String format, Object... params) {
        for (TextStatusListener listener : this.textStatusListeners) {
            try {
                listener.textStatus(String.format(format, params));
            }
            catch (Exception e) {

            }
        }
    }

    protected void fireJobState(List<Signaler> signalers, State state) {
        signalers.forEach(signaler -> signaler.signalJobProcessorState(state));
    }

    /**
     * Call the Retryable's action method until it either does not throw an Exception or it is
     * called maxTries number of times. If the method throws an Exception each time then this method
     * will throw the final Exception.
     * 
     * @param maxTries
     * @param r
     * @throws Exception
     */
    public static void retry(int maxTries, Retryable r) throws Exception {
        for (int i = 0; i < maxTries; i++) {
            try {
                r.retry();
                break;
            }
            catch (Exception e) {
                if (i == maxTries - 1) {
                    throw e;
                }
            }
        }
    }
}
