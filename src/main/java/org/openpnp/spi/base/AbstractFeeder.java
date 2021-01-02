package org.openpnp.spi.base;

import javax.swing.Icon;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.core.Commit;

public abstract class AbstractFeeder extends AbstractModelObject implements Feeder {
    /**
     * History:
     * 
     * Note: Can't actually use the @Version annotation because of a bug in SimpleXML. See
     * http://sourceforge.net/p/simple/mailman/message/27887562/
     * 
     * 1.0: Initial revision. 
     * 1.1: Migrate retryCount to feedRetryCount and zero out pickRetryCount for initial release
     *      of feature.
     */
    @Attribute(required=false)
    private double version = 1.0;
    
    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String parentId;

    @Attribute(required = false)
    protected String name;

    @Attribute
    protected boolean enabled;

    @Attribute
    protected String partId;
    
    /**
     * Note: This is feedRetryCount in reality. It was left as retryCount for backwards
     * compatibility when pickRetryCount was added. 
     * 
     * TODO Migration has been added and this can be removed after 2021-12-29.  
     */
    @Attribute(required=false)
    protected Integer retryCount = 3;
    
    @Attribute(required=false)
    protected int feedRetryCount = 3;
    
    @Attribute(required = false)
    protected int pickRetryCount = 3;

    protected Part part;
    
    //protected WizardContainer wizardContainer;
    
    public AbstractFeeder() {
        this.id = Configuration.createId("FDR");
        this.parentId = ROOT_FEEDER_ID;
        this.name = getClass().getSimpleName();
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                part = configuration.getPart(partId);
                
                if (version == 1.0) {
                    feedRetryCount = retryCount;
                    retryCount = null;
                    pickRetryCount = 0;
                    version = 1.1;
                }
            }
        });
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isEnabled() {
        if (parentId.equals(ROOT_FEEDER_ID)) {
            return enabled;
        } else {
            return enabled && Configuration.get().getMachine().getFeeder(parentId).isEnabled();
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        Object oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
    }
    
    public boolean isLocallyEnabled() {
        return enabled;
    }

    @Override
    public String getParentId() {
        return parentId;
    }

    /*
     * Any class extending AbstractFeeder that intends to support feeder nesting, needs to override
     * setParentId, addChild, removeChild, removeAllChildern, and isPotentialParentOf to provide the
     * required functionality.  Any class, that desires to only have the machine as its parent, should
     * override isParentIdChangable to return false.
     */
    @Override
    public void setParentId(String parentId) {
        if (isParentIdChangable()) {
            if (!this.parentId.equals(ROOT_FEEDER_ID)) {
                Configuration.get().getMachine().getFeeder(this.parentId).removeChild(getId());
            }
            this.parentId = parentId;
            if (!parentId.equals(ROOT_FEEDER_ID)) {
                Configuration.get().getMachine().getFeeder(parentId).addChild(getId());
            }
        }
    }

    @Override
    public boolean isParentIdChangable() {
        return true;
    }
    
    @Override
    public void addChild(String childId) {
    }
    
    @Override
    public void removeChild(String childId) {
    }
    
    @Override
    public void removeAllChildren() {
    }
    
    @Override
    public boolean isPotentialParentOf(Feeder feeder) {
        return false;
    }

    @Override
    public void preDeleteCleanUp() {
        if (!parentId.equals(ROOT_FEEDER_ID)) {
            Configuration.get().getMachine().getFeeder(parentId).removeChild(getId());
        }
    }
    
    
    @Override
    public void setPart(Part part) {
        Object oldValue = this.part;
        this.part = part;
        firePropertyChange("part", oldValue, part);
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
        firePropertyChange("name", null, name);
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return Icons.feeder;
    }

    public int getFeedRetryCount() {
        return feedRetryCount;
    }

    public void setFeedRetryCount(int feedRetryCount) {
        this.feedRetryCount = feedRetryCount;
        firePropertyChange("feedRetryCount", null, feedRetryCount);
    }
    
    public int getPickRetryCount() {
        return pickRetryCount;
    }

    public void setPickRetryCount(int pickRetryCount) {
        this.pickRetryCount = pickRetryCount;
        firePropertyChange("pickRetryCount", null, pickRetryCount);
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard(), "Configuration")};
    }
    
    public void postPick(Nozzle nozzle) throws Exception { }
}
