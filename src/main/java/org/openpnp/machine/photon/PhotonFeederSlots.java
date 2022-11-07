package org.openpnp.machine.photon;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Location;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

public class PhotonFeederSlots {
    @ElementList
    IdentifiableList<Slot> slots = new IdentifiableList<>();

    public Slot getSlot(int address) {
        String slotIdentifier = Slot.getIdentifierFromAddress(address);
        Slot slot = slots.get(slotIdentifier);

        if(slot == null) {
            slot = new Slot(address);
            slots.add(slot);
        }

        return slot;
    }

    public static class Slot implements Identifiable {
        @Attribute
        private int address;

        @Element(required = false)
        private Location location;

        private Slot() {}

        public Slot(int address) {
            this.address = address;
        }

        static String getIdentifierFromAddress(int address) {
            return "PHOTON-SLOT-" + address;
        }

        @Override
        public String getId() {
            return getIdentifierFromAddress(address);
        }

        public int getAddress() {
            return address;
        }

        public Location getLocation() {
            return location;
        }

        public void setLocation(Location location) {
            this.location = location;
        }
    }
}
