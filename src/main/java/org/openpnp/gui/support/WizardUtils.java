/*
 * Copyright (C) 2021 Tony Luken <tonyluken62+openpnp@gmail.com>
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.openpnp.gui.MainFrame;
import org.pmw.tinylog.Logger;

public class WizardUtils {

    /**
     * Checks to see is a wizard of the specified type and id is already registered as having an
     * actively running process. If so, it returns the registered wizard.  Otherwise, it returns a
     * new wizard of the specified type by calling its constructor with the specified parameters. 
     * The newly constructed wizard is assigned the specified id.
     * @param wizardToConstruct - the class of the wizard to return
     * @param id - the id of the wizard, typically this should be set to the id of whatever item the
     * wizard is responsible for configuring
     * @param parameters - zero or more parameters needed by the wizard's constructor
     * @return the wizard
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractConfigurationWizard> T configurationWizardFactory(
            Class<T> wizardToConstruct, String id, Object... parameters ) {
        if (MainFrame.get().getWizardWithActiveProcess() != null && 
                MainFrame.get().getWizardWithActiveProcess().getClass() ==  wizardToConstruct &&
                MainFrame.get().getWizardWithActiveProcess().getId() == id) {
            //A wizard is already registered so just return it
            return (T) MainFrame.get().getWizardWithActiveProcess();
        }
        else {
            //Attempt to construct a new wizard
            T ret = null;
            Exception savedException = null;
            Constructor<?>[] constructors = wizardToConstruct.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                if (constructor.getParameterCount() == parameters.length) {
                    try {
                        ret = (T) wizardToConstruct.getDeclaredConstructor(constructor.getParameterTypes()).newInstance(parameters);
                        ret.id = id;
                        return ret;
                    }
                    catch (IllegalArgumentException e) {
                        //This may be ok - wait until all constructors have been tried before reporting
                        savedException = e;
                    }
                    catch (InstantiationException | IllegalAccessException | 
                            InvocationTargetException | NoSuchMethodException | SecurityException e) {
                        //These shouldn't happen - report immediately and exit
                        e.printStackTrace();
                        return ret;
                    }
                }
            }
            Logger.error("Wizard could not be constructed with the specified parameters");
            if (savedException != null) {
                savedException.printStackTrace();
            }
            return ret;
        }
    }
}
