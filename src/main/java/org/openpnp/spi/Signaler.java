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

import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;
import org.openpnp.spi.base.AbstractJobProcessor;
import org.openpnp.spi.base.AbstractMachine;

/**
 * A Signaler is capable of signaling certain machine or job states like errors or warnings.
 */
public interface Signaler extends Identifiable, Named, PropertySheetHolder {

    public void signalMachineState(AbstractMachine.State state);

    public void signalJobProcessorState(AbstractJobProcessor.State state);
}