package org.firepick;

import org.openpnp.machine.reference.driver.wizards.AbstractSerialPortDriverConfigurationWizard;

public class MarlinDriverWizard  extends AbstractSerialPortDriverConfigurationWizard {
    private final MarlinDriver driver;
    
    public MarlinDriverWizard(MarlinDriver driver) {
        super(driver);
        this.driver = driver;
    }

}
