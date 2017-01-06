package org.openpnp.machine.reference.feeder;

import org.openpnp.ConfigurationListener;
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

public class ReferenceAutoFeederSlot extends ReferenceAutoFeeder {
    @Attribute(required = false)
    private String bankId;

    @Attribute(required = false)
    private String feederId;

    private Bank bank;
    private Feeder feeder;
    
    // TODO: Stuck on race condition in getBanks(). When loading a previously serialized feeder
    // we call Configuration.get().getMachine() before it's been set, giving an NPE. 
    // Guess we could fix with a config listener instead of doing it in commit();

    public ReferenceAutoFeederSlot() {
        // partId is required in AbstractFeeder to save the config. We don't use it so we just
        // set it to an empty string to make the serializer happy.
        partId = "";
    }
    
    static synchronized IdentifiableList<Bank> getBanks() {
        BanksProperty bp = (BanksProperty) Configuration.get().getMachine().getProperty("ReferenceAutoFeederSlot.banks");
        if (bp == null) {
            bp = new BanksProperty();
            bp.banks.add(new Bank());
            Configuration.get().getMachine().setProperty("ReferenceAutoFeederSlot.banks", bp);
        }
        return bp.banks;
    }

    @Commit
    public void commit() {
//        setBank(banks.get(bankId));
        feeder = getBank().getFeeder(feederId);
    }

    @Persist
    public void persist() {
        bankId = getBank().getId();
        feederId = feeder == null ? null : feeder.getId();
    }

    @Override
    public Location getPickLocation() throws Exception {
        return location;
    }

    @Override
    public void feed(Nozzle nozzle) throws Exception {
        if (feeder == null) {
            throw new Exception("No feeder loaded in slot.");
        }
        super.feed(nozzle);
    }

    @Override
    public void postPick(Nozzle nozzle) throws Exception {
        if (feeder == null) {
            throw new Exception("No feeder loaded in slot.");
        }
        super.postPick(nozzle);
    }

    @Override
    public boolean isEnabled() {
        return super.isEnabled() && feeder != null;
    }

    @Override
    public void setPart(Part part) {
        if (feeder == null) {
            return;
        }
        feeder.setPart(part);
    }

    @Override
    public Part getPart() {
        if (feeder == null) {
            return null;
        }
        return feeder.getPart();
    }

    public Bank getBank() {
        if (bank == null) {
            bank = getBanks().get(getBanks().size() - 1);
        }
        return bank;
    }

    public void setBank(Bank bank) {
        this.bank = bank;
    }

    public Feeder getFeeder() {
        return feeder;
    }

    public void setFeeder(Feeder feeder) throws Exception {
        if (getBank().getFeeder(feeder.getId()) == null) {
            throw new Exception("Can't set feeder from another bank.");
        }
        this.feeder = feeder;
    }

    @Root
    public static class Bank implements Identifiable, Named {
        @ElementList
        private IdentifiableList<ReferenceAutoFeederSlot.Feeder> feeders = new IdentifiableList<>();

        @Attribute(name = "id")
        final private String id;

        @Attribute
        private String name;

        public Bank() {
            this(Configuration.createId("BNK"));
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
            this.name = name;
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
    public static class Feeder implements Identifiable, Named {
        @Attribute(name = "id")
        final private String id;

        @Attribute
        private String name;

        @Attribute(required = false)
        private String partId;

        @Element
        private Location offsets = new Location(LengthUnit.Millimeters);

        private Part part;

        public Feeder() {
            this(Configuration.createId("FDR"));
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

        @Persist
        public void persist() {
            partId = part == null ? null : part.getId();
        }

        public String getId() {
            return id;
        }

        public void setPart(Part part) {
            this.part = part;
        }

        public Part getPart() {
            return part;
        }

        public void setOffsets(Location offsets) {
            this.offsets = offsets;
        }

        public Location getOffsets() {
            return offsets;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
