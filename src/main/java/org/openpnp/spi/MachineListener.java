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

package org.openpnp.spi;

/**
 * Provides a set of callbacks called by a Machine to notify listeners of asynchronous state changes
 * in the Machine.
 * 
 * @author jason
 */
public interface MachineListener {
    void machineHeadActivity(Machine machine, Head head);

    void machineEnabled(Machine machine);

    void machineEnableFailed(Machine machine, String reason);

    void machineDisabled(Machine machine, String reason);

    void machineDisableFailed(Machine machine, String reason);

    void machineBusy(Machine machine, boolean busy);

    static public class Adapter implements MachineListener {

        @Override
        public void machineHeadActivity(Machine machine, Head head) {}

        @Override
        public void machineEnabled(Machine machine) {}

        @Override
        public void machineEnableFailed(Machine machine, String reason) {}

        @Override
        public void machineDisabled(Machine machine, String reason) {}

        @Override
        public void machineDisableFailed(Machine machine, String reason) {}

        @Override
        public void machineBusy(Machine machine, boolean busy) {}
    }
}
