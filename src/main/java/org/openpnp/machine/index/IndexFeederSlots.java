package org.openpnp.machine.index;

import org.openpnp.model.Identifiable;
import org.openpnp.model.Location;
import org.openpnp.util.IdentifiableList;
import org.simpleframework.xml.ElementList;

public class IndexFeederSlots {
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
        private final int address;

        public Slot(int address) {
            this.address = address;
        }

        static String getIdentifierFromAddress(int address) {
            return "INDEX-SLOT-" + address;
        }

        @Override
        public String getId() {
            return getIdentifierFromAddress(address);
        }

        public int getAddress() {
            return address;
        }

        public Location getLocation() {
            return null;
        }
    }
}
