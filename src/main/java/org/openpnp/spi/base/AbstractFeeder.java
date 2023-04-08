package org.openpnp.spi.base;

import javax.swing.Icon;

import org.openpnp.ConfigurationListener;
import org.openpnp.Translations;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.simpleframework.xml.Attribute;

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

    public AbstractFeeder() {
        this.id = Configuration.createId("FDR");
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
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        Object oldValue = this.enabled;
        this.enabled = enabled;
        firePropertyChange("enabled", oldValue, enabled);
    }

    @Override
    public void setPart(Part part) {
        Part oldValue = this.part;
        this.part = part;
        firePropertyChange("part", oldValue, part);
        if (part != null) {
            this.partId = part.getId();
        }
        else {
            this.partId = "";
        }
        // Also notify the old/new part that the feeder count has changed.
        if (oldValue != null) {
            oldValue.setAssignedFeeders(-1);
        }
        if (part != null) {
            part.setAssignedFeeders(+1);
        }
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
        return new PropertySheet[] {new PropertySheetWizardAdapter(getConfigurationWizard(),
                Translations.getString("AbstractFeeder.ConfigurationWizard.title"))}; //$NON-NLS-1$
    }
    
    public void postPick(Nozzle nozzle) throws Exception { }
    
    @Override
    public boolean canTakeBackPart() {
        return false;   // default feeder does not take back parts
    }

    @Override
    public void takeBackPart(Nozzle nozzle) throws Exception {
        throw new UnsupportedOperationException("Not supported on this Feeder");
    }
}
