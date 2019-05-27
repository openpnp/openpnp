package org.openpnp.spi;

import org.openpnp.model.Job;

public interface JobProcessor extends PropertySheetHolder, WizardConfigurable {
    public interface TextStatusListener {
        public void textStatus(String text);
    }

    public void initialize(Job job) throws Exception;
    
    public boolean next() throws Exception;
    
    public void abort() throws Exception;    

    public void addTextStatusListener(TextStatusListener listener);

    public void removeTextStatusListener(TextStatusListener listener);
}
