package org.openpnp.spi;

import org.openpnp.gui.support.Wizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Location;
import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.SlottedFeeder;
import org.openpnp.spi.base.AbstractFeederSlot;

/**
 * Created by matt on 05/09/2016.
 */
public interface FeederSlot extends Named,Identifiable
{
    public Location getPickLocation();
    public Feeder getFeeder();
    public void setFeeder(SlottedFeeder feeder);
    public void setEnabled(Boolean bFlag);
    public Boolean getEnabled();
    public Wizard getConfigurationWizard();



}