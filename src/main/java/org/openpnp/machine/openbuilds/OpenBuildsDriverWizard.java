package org.openpnp.machine.openbuilds;

import org.openpnp.machine.reference.driver.wizards.AbstractCommunicationsConfigurationWizard;

public class OpenBuildsDriverWizard extends AbstractCommunicationsConfigurationWizard {
    private final OpenBuildsDriver driver;

    public OpenBuildsDriverWizard(OpenBuildsDriver driver) {
        super(driver);
        this.driver = driver;
    }
}
