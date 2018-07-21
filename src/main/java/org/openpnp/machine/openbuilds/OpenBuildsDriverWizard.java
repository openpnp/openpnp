package org.openpnp.machine.openbuilds;

import org.openpnp.machine.reference.driver.wizards.AbstractReferenceDriverConfigurationWizard;

public class OpenBuildsDriverWizard extends AbstractReferenceDriverConfigurationWizard {
    private final OpenBuildsDriver driver;

    public OpenBuildsDriverWizard(OpenBuildsDriver driver) {
        super(driver);
        this.driver = driver;
    }
}
