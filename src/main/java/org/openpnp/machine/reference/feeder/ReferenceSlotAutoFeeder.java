package org.openpnp.machine.reference.feeder;

import java.util.List;

import org.openpnp.ConfigurationListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.feeder.wizards.ReferenceSlotAutoFeederConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Configuration;
import org.openpnp.model.Identifiable;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Named;
import org.openpnp.model.Part;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class ReferenceSlotAutoFeeder extends ReferenceAutoFeeder {
    @Attribute(required = false)
    private String bankId;

    @Attribute(required = false)
    private String feederId;

    private Bank bank;
    
    public ReferenceSlotAutoFeeder() {
        this.id = Configuration.createId("SLOT-");
        this.name = this.id;
        // partId is required in AbstractFeeder to save the config. We don't use it so we just
        // set it to an empty string to make the serializer happy.
        partId = "";
    }
    
    @Commit
    public void commit() {
        Configuration.get().addListener(new ConfigurationListener() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
            }
            
            @Override
            public void configurationComplete(Configuration configuration) throws Exception {
                setBank(getBanks().get(bankId));
                setFeeder(getBank().getFeeder(feederId));
            }
        });
    }

    @Persist
    public void persist() {
        bankId = getBank().getId();
        feederId = getFeeder() == null ? null : getFeeder().getId();
    }

    @Override
    public Location getPickLocation() throws Exception {
        if (getFeeder() == null) {
            return location;
        }
        
        // Start with the slot's location.
        Location pickLocation = getLocation();
        // Rotate the feeder's offsets by the slot's rotation making the offsets
        // normal to the slot's orientation.
        Location offsets = getFeeder().getOffsets().rotateXy(pickLocation.getRotation());
        // Add the rotated offsets to the pickLocation to get the final pickLocation. We add
        // with rotation since we need to pick with the combined rotation of the slot and the
        // offsets.
        pickLocation = pickLocation.addWithRotation(offsets);
        return pickLocation;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        if (getFeeder() == null) {
            throw new Exception("No feeder loaded in slot.");
        }
        super.feed(nozzle);
    }

    @Override
    public void postPick(Nozzle nozzle) throws Exception {
        if (getFeeder() == null) {
            throw new Exception("No feeder loaded in slot.");
        }
        super.postPick(nozzle);
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && getFeeder() != null && getFeeder().getPart() != null;
    }

    @Override
    public void setPart(Part part) {
        if (getFeeder() == null) {
            return;
        }
        getFeeder().setPart(part);
    }

    @Override
    public Part getPart() {
        if (getFeeder() == null) {
            return null;
        }
        return getFeeder().getPart();
    }

    public Bank getBank() {
        if (bank == null) {
            bank = getBanks().get(getBanks().size() - 1);
        }
        return bank;
    }

    public void setBank(Bank bank) throws Exception {
        if (bank == null) {
            throw new Exception("Bank is required.");
        }
        this.bank = bank;
    }

    public Feeder getFeeder() {
        return getBank().getFeeder(this);
    }

    public void setFeeder(Feeder feeder) throws Exception {
        if (feeder != null) {
          // Make sure the feeder is in our bank.
          if (getBank().getFeeder(feeder.getId()) == null) {
              throw new Exception("Can't set feeder from another bank.");
          }
        }
        getBank().setFeeder(this, feeder);
    }
    
    public static synchronized IdentifiableList<Bank> getBanks() {
        BanksProperty bp = (BanksProperty) Configuration.get().getMachine().getProperty("ReferenceAutoFeederSlot.banks");
        if (bp == null) {
            bp = new BanksProperty();
            Bank bank = new Bank();
            bank.setName("Default");
            bp.banks.add(bank);
            Configuration.get().getMachine().setProperty("ReferenceAutoFeederSlot.banks", bp);
        }
        return bp.banks;
    }
    
    @Override
    public String getName() {
        return String.format("%s (%s)", name, getFeeder() == null ? "None" : getFeeder().getName());
    }
    
    @Override
    public void setName(String name) {
        /**
         * This is kind of an ugly hack to avoid the bug in:
         * https://github.com/openpnp/openpnp/issues/427.
         * It removes any instance of the additional info we show in the name when a feeder is
         * attached.
         */
        name = name.replace(String.format("(%s)", getFeeder() == null ? "None" : getFeeder().getName()), "").trim();
        super.setName(name);
    }
    
    @Override
    public Wizard getConfigurationWizard() {
        return new ReferenceSlotAutoFeederConfigurationWizard(this);
    }

    @Root
    public static class Bank extends AbstractModelObject implements Identifiable, Named {
        @ElementList
        private IdentifiableList<ReferenceSlotAutoFeeder.Feeder> feeders = new IdentifiableList<>();

        @Attribute(name = "id")
        final private String id;

        @Attribute
        private String name;
        
        BiMap<Feeder, ReferenceSlotAutoFeeder> assignments = HashBiMap.create();
        
        public Bank() {
            this(Configuration.createId("BANK-"));
        }

        public Bank(@Attribute(name = "id") String id) {
            if (id == null) {
                throw new Error("Id is required.");
            }
            this.id = id;
            this.name = id;
        }

        public String getId() {
            return id;
        }

        Feeder getFeeder(String id) {
            return feeders.get(id);
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            Object oldValue = this.name;
            this.name = name;
            firePropertyChange("name", oldValue, name);
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        public List<Feeder> getFeeders() {
            return feeders;
        }
        
        public void setFeeder(ReferenceSlotAutoFeeder slot, Feeder feeder) throws Exception {
            assignments.forcePut(feeder, slot);
        }
        
        public Feeder getFeeder(ReferenceSlotAutoFeeder slot) {
            return assignments.inverse().get(slot);
        }
    }
    
    /**
     * This class is just a delegate wrapper around a list. 
     */
    @Root
    public static class BanksProperty {
        @ElementList
        IdentifiableList<Bank> banks = new IdentifiableList<>();
    }

    @Root
    public static class Feeder extends AbstractModelObject implements Identifiable, Named {
        @Attribute(name = "id")
        final private String id;

        @Attribute
        private String name;

        @Attribute(required = false)
        private String partId;

        @Element
        private Location offsets = new Location(LengthUnit.Millimeters);

        private Part part;
        
        private ReferenceSlotAutoFeeder owner = null;
        
        public Feeder() {
            this(Configuration.createId("SLOTFDR-"));
        }

        public Feeder(@Attribute(name = "id") String id) {
            this.id = id;
            this.name = id;
            Configuration.get().addListener(new ConfigurationListener.Adapter() {
                @Override
                public void configurationLoaded(Configuration configuration) throws Exception {
                    part = configuration.getPart(partId);
                }
            });
        }
        
        public void setOwner(ReferenceSlotAutoFeeder owner) throws Exception {
            if (this.owner == owner) {
                return;
            }
            if (this.owner != null) {
                System.out.println(String.format("Remove owner of %s from %s", this, owner));
                this.owner.setFeeder(null);
            }
            System.out.println(String.format("Set owner of %s to %s", this, owner));
            this.owner = owner;
        }

        @Persist
        public void persist() {
            partId = part == null ? null : part.getId();
        }

        public String getId() {
            return id;
        }

        public void setPart(Part part) {
            Object oldValue = this.part;
            this.part = part;
            firePropertyChange("part", oldValue, part);
        }

        public Part getPart() {
            return part;
        }

        public void setOffsets(Location offsets) {
            Object oldValue = this.offsets;
            this.offsets = offsets;
            firePropertyChange("offsets", oldValue, offsets);
        }

        public Location getOffsets() {
            return offsets;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            Object oldValue = this.name;
            this.name = name;
            firePropertyChange("name", oldValue, name);
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
}
