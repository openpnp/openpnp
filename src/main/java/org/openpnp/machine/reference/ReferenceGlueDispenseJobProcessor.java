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

package org.openpnp.machine.reference;

import org.openpnp.model.Job;
import org.openpnp.spi.base.AbstractPasteDispenseJobProcessor;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 * The Glue feature has been removed. This is left here so that existing configs don't break.
 * TODO: Remove after July 1, 2017.
 */
@Deprecated
@Root
public class ReferenceGlueDispenseJobProcessor extends AbstractPasteDispenseJobProcessor {
    @Attribute(required = false)
    protected boolean parkWhenComplete = false;

    public ReferenceGlueDispenseJobProcessor() {
    }

    @Override
    public void initialize(Job job) throws Exception {
    }

    @Override
    public boolean next() throws Exception {
        return false;
    }

    @Override
    public void abort() throws Exception {
    }

    @Override
    public void skip() throws Exception {
    }

    @Override
    public boolean canSkip() {
        return false;
    }
}
