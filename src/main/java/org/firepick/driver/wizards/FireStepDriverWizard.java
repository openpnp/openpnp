package org.firepick.driver.wizards;

import org.firepick.driver.FireStepDriver;
import org.openpnp.machine.reference.driver.wizards.AbstractSerialPortDriverConfigurationWizard;

public class FireStepDriverWizard  extends AbstractSerialPortDriverConfigurationWizard {
    private final FireStepDriver driver;
    
    public FireStepDriverWizard(FireStepDriver driver) {
        super(driver);
        this.driver = driver;
    }

}
