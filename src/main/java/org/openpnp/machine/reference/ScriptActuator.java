package org.openpnp.machine.reference;

import java.io.File;
import java.util.Map;

import org.openpnp.Scripting;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.wizards.ScriptActuatorConfigurationWizard;
import org.openpnp.model.Configuration;
import org.simpleframework.xml.Element;

public class ScriptActuator extends ReferenceActuator {
    @Element
    String scriptName;
    
    private void execute(Map<String, Object> globals) throws Exception {
        // Using https://docs.oracle.com/javase/7/docs/technotes/guides/scripting/programmer_guide/ 
        // we should be able to call specific functions, and return a value.
        
        
        Scripting scripting = Configuration.get().getScripting();
        File scriptsDirectory = scripting.getScriptsDirectory();
        File script = new File(scriptsDirectory, scriptName);
        scripting.execute(script, globals);
    }
    
    @Override
    public void actuate(boolean on) throws Exception {
        super.actuate(on);
    }

    @Override
    public void actuate(double value) throws Exception {
        super.actuate(value);
    }

    @Override
    public String read() throws Exception {
        return super.read();
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new ScriptActuatorConfigurationWizard(this);
    }
}
