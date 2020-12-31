package org.openpnp.machine.reference;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.openpnp.scripting.Scripting;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ScriptActuatorConfigurationWizard;
import org.openpnp.model.Configuration;
import org.simpleframework.xml.Element;

public class ScriptActuator extends ReferenceActuator {
    @Element(required=false)
    protected String scriptName = "";

    private void execute(Map<String, Object> globals) throws Exception {
        // Using https://docs.oracle.com/javase/7/docs/technotes/guides/scripting/programmer_guide/ 
        // we should be able to call specific functions, and return a value.
        Scripting scripting = Configuration.get().getScripting();
        File scriptsDirectory = scripting.getScriptsDirectory();
        File script = new File(scriptsDirectory, scriptName);
        scripting.execute(script, globals);
    }

    @Override
    protected void driveActuation(boolean on) throws Exception {
        Map<String, Object> globals = new HashMap<>();
        globals.put("actuateBoolean", on);
        this.execute(globals);
    }

    @Override
    protected void driveActuation(double value) throws Exception {
        Map<String, Object> globals = new HashMap<>();
        globals.put("actuateDouble", value);
        this.execute(globals);
    }

    @Override
    protected void driveActuation(String value) throws Exception {
        Map<String, Object> globals = new HashMap<>();
        globals.put("actuateString", value);
        this.execute(globals);
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ScriptActuatorConfigurationWizard(getMachine(), this);
    }

    public String getScriptName() {
        return this.scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
        firePropertyChange("scriptName", null, this.scriptName);
    }

}
