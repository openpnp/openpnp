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

import javax.swing.Action;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.Binding.SyncFailure;

public class ApplyResetBindingListener extends AbstractBindingListener {
    private final Action saveAction;
    private final Action resetAction;

    public ApplyResetBindingListener(Action saveAction, Action resetAction) {
        this.saveAction = saveAction;
        this.resetAction = resetAction;
    }

    @Override
    public void syncFailed(Binding binding, SyncFailure failure) {
        saveAction.setEnabled(false);
    }

    @Override
    public void synced(Binding binding) {
        saveAction.setEnabled(true);
        resetAction.setEnabled(true);
    }
}
