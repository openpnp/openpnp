package org.openpnp.machine.reference.solutions;

import org.openpnp.Translations;
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
                        Translations.getString("ScriptingSolutions.Issue.EnginePooling"), //$NON-NLS-1$
                        Translations.getString("ScriptingSolutions.Solution.EnginePooling"), //$NON-NLS-1$
                        Severity.Suggestion,
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
                        return Translations.getString("ScriptingSolutions.ExtendedDescription.EnginePooling"); //$NON-NLS-1$
                    }
                });
            }
        }
    }
}
