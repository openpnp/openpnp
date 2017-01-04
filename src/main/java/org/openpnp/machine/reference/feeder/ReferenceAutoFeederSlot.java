package org.openpnp.machine.reference.feeder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.spi.Nozzle;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;

public class ReferenceAutoFeederSlot extends ReferenceAutoFeeder {
    // Note: By using a static here we can skip the external configuration file, for now. It's
    // a cheap hack that duplicates all the config, but it should actually be pretty safe and it's
    // very easy to get things rolling.
    @ElementList(required=false)
    private static List<Bank> banks = new ArrayList<>();
    
    @Attribute
    private String bankId;
    
    @Attribute(required=false)
    private String feederId;
    
    private Bank bank; 
    private Feeder feeder;
    
    public ReferenceAutoFeederSlot() {
    }
    
    @Commit
    public void commit() {
        bank = getBank(bankId);
        if (feederId != null) {
            feeder = bank.getFeeder(feederId);
        }
    }
    
    Bank getBank(String id) {
        for (Bank bank : banks) {
            if (bank.getId().equals(id)) {
                return bank;
            }
        }
        return null;
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
    public Part getPart() {
        if (feeder == null) {
            return null;
        }
        return feeder.getPart();
    }
    
    public void setFeeder(Feeder feeder) {
        this.feeder = feeder;
        this.feederId = feeder.getId();
    }
    
    public void setBank(Bank bank) {
        this.bank = bank;
        this.bankId = bank.getId();
    }

    @Root
    public static class Bank {
        @ElementList
        private List<ReferenceAutoFeederSlot.Feeder> feeders = new ArrayList<>();
        
        @Attribute
        private String id;
        
        
        public Bank() {
            id = UUID.randomUUID().toString();
        }
        
        public String getId() {
            return id;
        }
        
        Feeder getFeeder(String id) {
            for (Feeder feeder : feeders) {
                if (feeder.getId().equals(id)) {
                    return feeder;
                }
            }
            return null;
        }
    }
    
    @Root
    public static class Feeder {
        @Attribute
        private String id;
        
        @Attribute(required=false)
        private String partId;
        
        private Part part;
        
        public Feeder() {
            id = UUID.randomUUID().toString();
        }
        
        public String getId() {
            return id;
        }
        
        public Part getPart() {
            return part;
        }
    }
}
