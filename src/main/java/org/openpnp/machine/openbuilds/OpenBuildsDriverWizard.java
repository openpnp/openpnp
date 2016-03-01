package org.openpnp.machine.openbuilds;

import org.openpnp.machine.reference.driver.wizards.AbstractSerialPortDriverConfigurationWizard;

public class OpenBuildsDriverWizard extends AbstractSerialPortDriverConfigurationWizard {
    private final OpenBuildsDriver driver;

    public OpenBuildsDriverWizard(OpenBuildsDriver driver) {
        super(driver);
        this.driver = driver;
    }
}
