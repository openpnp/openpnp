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

package org.openpnp;

import org.openpnp.model.Configuration;

public interface ConfigurationListener {
    /**
     * Called by Configuration when it has been loaded from disk. This method will also be called
     * when the ConfigurationListener is added to the Configuration if the Configuration has already
     * been loaded. When this method is called, the Configuration may not yet be complete. This
     * method is intended to function as a chance for listeners to resolve parts of their
     * Configuration that depend on other Configuration objects.
     * 
     * @param configuration
     */
    public void configurationLoaded(Configuration configuration) throws Exception;

    /**
     * Called by Configuration after all existing listeners have been notified with
     * configurationLoaded. This is a signal that the Configuration is complete and resolved and is
     * ready to be used for runtime tasks. This method will also be called when the
     * ConfigurationListener is added to the Configuration if the Configuration has already been
     * loaded.
     * 
     * @param configuration
     * @throws Exception
     */
    public void configurationComplete(Configuration configuration) throws Exception;

    public static class Adapter implements ConfigurationListener {
        @Override
        public void configurationLoaded(Configuration configuration) throws Exception {}

        @Override
        public void configurationComplete(Configuration configuration) throws Exception {}
    }
}
