package org.openpnp.machine.reference.feeder;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.feeder.wizards.ReferenceFeederSlotConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.base.AbstractFeederSlot;
import org.openpnp.spi.SlottedFeeder;

/**
 * Created by matt on 05/09/2016.
 */
public class ReferenceFeederSlot extends AbstractFeederSlot {

    public ReferenceFeederSlot() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                setFeeder((SlottedFeeder) configuration.get().getMachine().getFeeder(feederID));
            }
        });
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceFeederSlotConfigurationWizard(this);
    }

}
