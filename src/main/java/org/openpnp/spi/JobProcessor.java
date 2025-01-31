package org.openpnp.spi;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.openpnp.model.Job;

public interface JobProcessor extends PropertySheetHolder, WizardConfigurable {
    public interface TextStatusListener {
        public void textStatus(String text);
    }

    public void initialize(Job job) throws Exception;

    public boolean next() throws JobProcessorException;

    boolean isSteppingToNextMotion();

    public void abort() throws JobProcessorException;    

    public void addTextStatusListener(TextStatusListener listener);

    public void removeTextStatusListener(TextStatusListener listener);
    
    public class JobProcessorException extends Exception {
        private static final long serialVersionUID = 1L;

        private final Object source;
        private Object secondarySource = null;
        private boolean interrupting = false;

        private static String getThrowableMessage(Throwable throwable) {
            if (throwable.getMessage() != null) {
                return throwable.getMessage();
            }
            // If a message is missing, use the stack trace as the message
            // (same behavior as MessageBoxes.errorBox() when given an exception directly).
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            return sw.toString();
        }

        public JobProcessorException(Object source, Throwable throwable) {
            super(getThrowableMessage(throwable), throwable);
            this.source = source;
            if (throwable instanceof JobProcessorException) {
                JobProcessorException e = ((JobProcessorException) throwable);
                this.interrupting = e.isInterrupting();
                if (e.secondarySource !=null) {
                    this.secondarySource = e.secondarySource;
                }
            }
        }

        public JobProcessorException(Object source, Object secondarySource, Throwable throwable) {
            super(getThrowableMessage(throwable), throwable);
            this.source = source;
            if (throwable instanceof JobProcessorException) {
                JobProcessorException e = ((JobProcessorException) throwable);
                this.interrupting = e.isInterrupting();
            }
            this.secondarySource = secondarySource;
        }

        public JobProcessorException(Object source, String message) {
            super(message);
            this.source = source;
        }

        public JobProcessorException(Object source, Object secondarySource, String message) {
            super(message);
            this.source = source;
            this.secondarySource = source;
        }

        public JobProcessorException(Object source, String message, boolean interrupting) {
            super(message);
            this.source = source;
            this.interrupting = interrupting;
        }

        public JobProcessorException(Object source, Throwable throwable, boolean interrupting) {
            super(getThrowableMessage(throwable), throwable);
            this.source = source;
            this.interrupting = interrupting;
        }

        public Object getSource() {
            return source;
        }

        public Object getSecondarySource() {
            return secondarySource;
        }

        public boolean isInterrupting() {
            return interrupting;
        }
    }
}
