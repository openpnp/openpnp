package org.openpnp.gui.support;

import javax.swing.Action;

import org.jdesktop.beansbinding.AbstractBindingListener;
import org.jdesktop.beansbinding.Binding;
import org.jdesktop.beansbinding.Binding.SyncFailure;

public class SaveResetBindingListener extends AbstractBindingListener {
	private final Action saveAction;
	private final Action resetAction;
	
	public SaveResetBindingListener(Action saveAction, Action resetAction) {
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
