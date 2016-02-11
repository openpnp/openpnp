package org.openpnp.spi.base;

import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Job;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.PropertySheetHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractJobProcessor implements JobProcessor, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AbstractJobProcessor.class);

    protected Job job;
    protected Set<JobProcessorListener> listeners = new HashSet<>();
    protected JobProcessorDelegate delegate = new DefaultJobProcessorDelegate();
    protected JobState state;
    protected Thread thread;
    protected Object runLock = new Object();
    private boolean pauseAtNextStep;

    @Override
    public void setDelegate(JobProcessorDelegate delegate) {
        if (delegate == null) {
            this.delegate = new DefaultJobProcessorDelegate();
        }
        else {
            this.delegate = delegate;
        }
    }
    
    @Override
    public void addListener(JobProcessorListener listener) {
        listeners.add(listener);
    }
    
    @Override
    public void removeListener(JobProcessorListener listener) {
        listeners.remove(listener);
    }
    
    @Override
    public Job getJob() {
        return job;
    }
    
    @Override
    public JobState getState() {
        return state;
    }
    
    // TODO: Change this, and most of the other properties on here to bound
    // properties.
    @Override
    public void load(Job job) {
        stop();
        this.job = job;
        
        fireJobLoaded();
    }

    @Override
    public void start() throws Exception {
        logger.debug("start()");
        if (state != JobState.Stopped) {
            throw new Exception("Invalid state. Cannot start new job while state is " + state);
        }
        if (thread != null && thread.isAlive()) {
            throw new Exception("Previous Job has not yet finished.");
        }
        thread = new Thread(this);
        thread.start();
    }
    
    @Override
    public void pause() {
        logger.debug("pause()");
        state = JobState.Paused;
        fireJobStateChanged();
    }
    
    @Override
    public void step() throws Exception {
        logger.debug("step()");
        if (state == JobState.Stopped) {
            pauseAtNextStep = true;
            start();
        }
        else {
            pauseAtNextStep = true;
            resume();
        }
    }
    
    @Override
    public void resume() {
        logger.debug("resume()");
        state = JobState.Running;
        fireJobStateChanged();
        synchronized (runLock) {
            runLock.notifyAll();
        }
    }
    
    @Override
    public void stop() {
        logger.debug("stop()");
        state = JobState.Stopped;
        fireJobStateChanged();
        synchronized (runLock) {
            runLock.notifyAll();
        }
    }

    /**
     * Checks if the Job has been Paused or Stopped. If it has been Paused this method
     * blocks until the Job is Resumed. If the Job has been Stopped it returns false and
     * the loop should break.
     */
    protected boolean shouldJobProcessingContinue() {
        if (pauseAtNextStep) {
            pauseAtNextStep = false;
            pause();
        }
        while (true) {
            if (state == JobState.Stopped) {
                return false;
            }
            else if (state == JobState.Paused) {
                synchronized (runLock) {
                    try {
                        runLock.wait();
                    }
                    catch (InterruptedException ie) {
                        throw new Error(ie);
                    }
                }
            }
            else {
                break;
            }
        }
        return true;
    }
    
    protected void fireJobEncounteredError(JobError error, String description) {
        logger.debug("fireJobEncounteredError({}, {})", error, description);
        for (JobProcessorListener listener : listeners) {
            listener.jobEncounteredError(error, description);
        }
    }
    
    private void fireJobLoaded() {
        logger.debug("fireJobLoaded()");
        for (JobProcessorListener listener : listeners) {
            listener.jobLoaded(job);
        }
    }
    
    protected void fireJobStateChanged() {
        logger.debug("fireJobStateChanged({})", state);
        for (JobProcessorListener listener : listeners) {
            listener.jobStateChanged(state);
        }
    }
    
    protected void firePartProcessingStarted(BoardLocation board, Placement placement) {
        logger.debug("firePartProcessingStarted({}, {})", board, placement);
        for (JobProcessorListener listener : listeners) {
            listener.partProcessingStarted(board, placement);
        }
    }
    
    protected void firePartPicked(BoardLocation board, Placement placement) {
        logger.debug("firePartPicked({}, {})", board, placement);
        for (JobProcessorListener listener : listeners) {
            listener.partPicked(board, placement);
        }
    }
    
    protected void firePartPlaced(BoardLocation board, Placement placement) {
        logger.debug("firePartPlaced({}, {})", board, placement);
        for (JobProcessorListener listener : listeners) {
            listener.partPlaced(board, placement);
        }
    }
    
    protected void firePartProcessingComplete(BoardLocation board, Placement placement) {
        logger.debug("firePartProcessingComplete({}, {})", board, placement);
        for (JobProcessorListener listener : listeners) {
            listener.partProcessingCompleted(board, placement);
        }
    }
    
    protected void fireDetailedStatusUpdated(String status) {
        logger.debug("fireDetailedStatusUpdated({})", status);
        for (JobProcessorListener listener : listeners) {
            listener.detailedStatusUpdated(status);
        }
    }
    
    
    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(getConfigurationWizard())
        };
    }
    
    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public Icon getPropertySheetHolderIcon() {
        // TODO Auto-generated method stub
        return null;
    }



    class DefaultJobProcessorDelegate implements JobProcessorDelegate {
        @Override
        public PickRetryAction partPickFailed(BoardLocation board, Part part,
                Feeder feeder) {
            return PickRetryAction.SkipAndContinue;
        }
    }
}
