package org.openpnp.spi;

import org.openpnp.model.Job;

public interface JobProcessor extends PropertySheetHolder, WizardConfigurable {
    public interface TextStatusListener {
        public void textStatus(String text);
    }

    public void initialize(Job job) throws Exception;
    
    public boolean next() throws JobProcessorException;
    
    public void abort() throws JobProcessorException;    

    public void addTextStatusListener(TextStatusListener listener);

    public void removeTextStatusListener(TextStatusListener listener);
    
    public class JobProcessorException extends Exception {
        private static final long serialVersionUID = 1L;
        
        private final Object source;
        
        public JobProcessorException(Object source, Throwable throwable) {
            super(throwable.getMessage(), throwable);
            this.source = source;
        }
        
        public JobProcessorException(Object source, String message) {
            super(message);
            this.source = source;
        }
        
        public Object getSource() {
            return source;
        }
    }
}
