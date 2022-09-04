package org.openpnp.machine.reference.solutions;

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Configuration;

import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.model.Solutions.State;

/**
 * This helper class implements the Issues & Solutions for the Scripting class.
 *
 */
public class ScriptingSolutions implements Solutions.Subject {
    private ReferenceMachine machine;

    public ScriptingSolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Advanced)) {
            if (Configuration.get()
                             .getMachine()
                             .isPoolScriptingEngines() == false) {
                solutions.add(new Solutions.Issue(machine,
                        "Script exeuction performance can be improved by enabling engine pooling.",
                        "Enable script engine pooling.", Severity.Suggestion,
                        "https://github.com/openpnp/openpnp/wiki/Scripting#script-engine-pooling") {

                    @Override
                    public void setState(Solutions.State state) throws Exception {
                        ((ReferenceMachine) Configuration.get()
                                                         .getMachine()).setPoolScriptingEngines(
                                                                 state == State.Solved ? true
                                                                         : false);
                        super.setState(state);
                    }

                    @Override
                    public String getExtendedDescription() {
                        return "<html>By default, every time a script should be executed, a new instance of the appropriate script engine is created.<br><br>"
                                + "By enabling script engine pooling, a new instance of any script engine is only created if the pool doesn't contain an available instance of the appropriate type. "
                                + "Following executions re-use the already initialized script engines, lowering execution time for scripting hooks."
                                + "This feature is only relevant if you use scripting in OpenPnP.<br><br>"
                                + "<span style=\"color:red;\">Script engine pooling can - depending on the script engine implementation - cause global state to be kept in following invocations. "
                                + "Check the wiki for further information.</span></html>";
                    }
                });
            }
        }
    }
}
