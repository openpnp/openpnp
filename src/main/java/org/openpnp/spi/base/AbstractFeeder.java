package org.openpnp.spi.base;

import javax.swing.Icon;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.WizardContainer;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;
import org.openpnp.machine.reference.feeder.ReferenceFeederGroup;

public abstract class AbstractFeeder extends AbstractModelObject implements Feeder {
    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String owner;

    @Attribute(required = false)
    protected String name;

    @Attribute
    protected boolean enabled;

    @Attribute
    protected String partId;
    
    /**
     * Note: This is feedRetryCount in reality. It was left as retryCount for backwards
     * compatibility when pickRetryCount was added. 
     */
    @Attribute(required=false)
    protected int retryCount = 3;
    
    @Attribute(required = false)
    protected int pickRetryCount = 3;

    protected Part part;
    
    protected WizardContainer wizardContainer;

    public AbstractFeeder() {
        this.id = Configuration.createId("FDR");
        this.name = getClass().getSimpleName();
        this.owner = "Machine";
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                part = configuration.getPart(partId);
            }
        });
    }


    @Commit
    public void commit() {
        //This method gets called by the deserializer when configuration .xml files are loading.
        if (owner == null) {
            Logger.trace( "Old feeder format found in .xml file, converting to new feeder format..." );
            owner = "Machine";
        }
    }
    
    
    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isEnabled() {
        if (owner.equals("Machine")) {
            return enabled;
        } else {
            return enabled && Configuration.get().getMachine().getFeederByName(owner).isEnabled();
        }
    }

    @Override
    public boolean isLocallyEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        Object oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
    }

    @Override
    public void setWizardContainer(WizardContainer wizardContainer) {
        this.wizardContainer = wizardContainer;
    }

    @Override
    public void setOwner(String owner) {
        if (!this.owner.equals("Machine")) {
            ((ReferenceFeederGroup) Configuration.get().getMachine().getFeederByName(this.owner)).removeChild(this);
        }
        this.owner = owner;
        if (!owner.equals("Machine")) {
            ((ReferenceFeederGroup) Configuration.get().getMachine().getFeederByName(owner)).addChild(this);
        }
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public void setPart(Part part) {
        this.part = part;
        this.partId = part.getId();
    }

    @Override
    public Part getPart() {
        return part;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return Icons.feeder;
    }

    public int getFeedRetryCount() {
        return retryCount;
    }

    public void setFeedRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
    
    public int getPickRetryCount() {
        return pickRetryCount;
    }

    public void setPickRetryCount(int pickRetryCount) {
        this.pickRetryCount = pickRetryCount;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard(), "Configuration", wizardContainer)};
    }
    
    public void postPick(Nozzle nozzle) throws Exception { }
}
