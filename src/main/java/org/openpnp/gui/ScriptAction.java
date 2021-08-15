package org.openpnp.gui;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;

import org.openpnp.model.Configuration;
import org.openpnp.util.UiUtils;

@SuppressWarnings("serial")
public class ScriptAction extends AbstractAction{

    public ScriptAction(String eventName) {
        this.eventName = eventName;
    }
    private String eventName;

    @Override
    public void actionPerformed(ActionEvent arg0) {
        UiUtils.messageBoxOnException(() -> {
            Map<String, Object> globals = new HashMap<>();
            Configuration.get()
                        .getScripting()
                        .on(this.eventName, globals);
        });
    }
}
