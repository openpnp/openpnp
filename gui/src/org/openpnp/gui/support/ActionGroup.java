package org.openpnp.gui.support;

import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;

public class ActionGroup {
	private Set<Action> actions = new HashSet<Action>();
	
	public ActionGroup(Action... actions) {
		for (Action action : actions) {
			this.actions.add(action);
		}
	}
	
	public void addAction(Action action) {
		actions.add(action);
	}
	
	public void removeAction(Action action) {
		actions.remove(action);
	}
	
	public void setEnabled(boolean enabled) {
		for (Action action : actions) {
			action.setEnabled(enabled);
		}
	}
}
