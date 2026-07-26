/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.support;

import org.openpnp.Translations;
import org.openpnp.spi.Nozzle;

public class NozzleItem extends HeadMountableItem {

    public NozzleItem(Nozzle nozzle) {
        super(nozzle);
    }

    public Nozzle getNozzle() {
        return (Nozzle)hm;
    }

    @Override
    public String toString() {
        Nozzle nozzle = (Nozzle)hm;

        return String.format(Translations.getString("HeadMountableItem.Format.Nozzle"), nozzle.getName(), //$NON-NLS-1$
                nozzle.getNozzleTip() != null ? nozzle.getNozzleTip().getName()
                        : Translations.getString("HeadMountableItem.NoNozzleTip"), //$NON-NLS-1$
                nozzle.getPart() != null ? String.format(" - %s", nozzle.getPart().getId()) : "", 
                nozzle.getHead() != null ? String.format(
                        Translations.getString("HeadMountableItem.Format.Head"), //$NON-NLS-1$
                        nozzle.getHead().getName()) : "");
    }
}
