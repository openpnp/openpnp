package org.openpnp.machine.index.sheets.gui;

import org.openpnp.machine.index.IndexFeederSlots.Slot;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;

import java.util.Locale;

public class SlotProxy extends AbstractModelObject {
    private Slot slot;

    public void setSlot(Slot slot) {
        Slot oldSlot = this.slot;
        boolean oldIsEnabled = isEnabled();
        String oldSlotAddress = getSlotAddress();
        Location oldLocation = getLocation();

        this.slot = slot;

        firePropertyChange("slot", oldSlot, slot);
        firePropertyChange("enabled", oldIsEnabled, isEnabled());
        firePropertyChange("slotAddress", oldSlotAddress, getSlotAddress());
        firePropertyChange("location", oldLocation, getLocation());
    }

    public boolean isEnabled() {
        return slot != null;
    }

    public String getSlotAddress() {
        if(slot == null) {
            return "None";
        } else {
            return String.format(Locale.US, "%d", slot.getAddress());
        }
    }

    public Location getLocation() {
        if(slot == null) {
            return new Location(LengthUnit.Millimeters);
        } else {
            Location location = slot.getLocation();

            return location;
        }
    }

    public void setLocation(Location location) {
        Location oldLocation = getLocation();

        if(slot != null) {
            slot.setLocation(location);
        }

        firePropertyChange("location", oldLocation, getLocation());
    }
}
