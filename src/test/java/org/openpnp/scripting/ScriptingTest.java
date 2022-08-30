package org.openpnp.scripting;

import java.io.File;
import java.util.HashMap;

import javax.script.ScriptException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Configuration;

import com.google.common.io.Files;

public class ScriptingTest {
    /**
     * Executes all supported script types and checks extension mapping and coorect behavior of
     * pooling. Also calls scripts from a script and checks if separate ScriptEngines were used for
     * the nested call.
     * 
     * @throws Exception
     */
    @Test
    public void testScripting() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        File scriptsDirectory = new File(workingDirectory, "scripts");

        Configuration.initialize(workingDirectory);
        Configuration.get()
                     .load();
        ReferenceMachine referenceMachine = (ReferenceMachine) Configuration.get()
                                                                            .getMachine();
        Scripting scripting = new Scripting(scriptsDirectory);

        HashMap<String, String> testResults = new HashMap<>();
        HashMap<String, Object> testGlobals = new HashMap<>();
        testGlobals.put("testResults", testResults);

        // Copy the required scripts over to the scripts directory.
        FileUtils.copyURLToFile(
                ClassLoader.getSystemResource("config/ScriptingTest/callScriptFromScript.java"),
                new File(scriptsDirectory, "callScriptFromScript.java"));
        FileUtils.copyURLToFile(
                ClassLoader.getSystemResource("config/ScriptingTest/throwException.java"),
                new File(scriptsDirectory, "throwException.java"));
        FileUtils.copyURLToFile(
                ClassLoader.getSystemResource("config/ScriptingTest/Events/testEvent.java"),
                new File(scriptsDirectory, "Events/testEvent.java"));
        FileUtils.copyURLToFile(
                ClassLoader.getSystemResource("config/ScriptingTest/Events/testEvent.bsh"),
                new File(scriptsDirectory, "Events/testEvent.bsh"));
        FileUtils.copyURLToFile(
                ClassLoader.getSystemResource("config/ScriptingTest/Events/testEvent.js"),
                new File(scriptsDirectory, "Events/testEvent.js"));
        FileUtils.copyURLToFile(
                ClassLoader.getSystemResource("config/ScriptingTest/Events/testEvent.py"),
                new File(scriptsDirectory, "Events/testEvent.py"));

        referenceMachine.setPoolScriptingEngines(false);
        scripting.on("testEvent", testGlobals);
        System.out.println("Results from scripts: " + testResults);

        // Check if all scripts have been executed and if the execution yielded the expected result
        for (String extension : new String[] {"bsh", "java", "js", "py"}) {
            if (testResults.get(extension) != "ok") {
                throw new Exception("Script execution for " + extension
                        + " file extension didn't return expected result");
            }
        }

        if (scripting.getScriptingEnginePoolObjectCount() != 0) {
            throw new Exception("Engines in the pool even if pooling is disabled");
        }

        referenceMachine.setPoolScriptingEngines(true);

        boolean scriptError = false;
        try {
            scripting.execute(new File(scriptsDirectory, "throwException.java"), testGlobals);
        }
        catch (ScriptException e) {
            scriptError = true;
            System.out.println("Catched test exception from script");
        }
        if (!scriptError) {
            throw new Exception("Test script failed to throw exception");
        }

        // Do the same thing again to test if engines are returned to the pool even if the script
        // throws an exception
        try {
            scripting.execute(new File(scriptsDirectory, "throwException.java"), testGlobals);
        }
        catch (ScriptException e) {
        }
        if (scripting.getScriptingEnginePoolObjectCount() != 1) {
            throw new Exception("Failed to return engine to the pool after script exception");
        }

        scripting.on("testEvent", testGlobals);

        if (scripting.getScriptingEnginePoolObjectCount() != 3) {
            throw new Exception("Number of engines in pool didn't match expectations");
        }

        scripting.clearScriptingEnginePool();

        if (scripting.getScriptingEnginePoolObjectCount() != 0) {
            throw new Exception("Engines left in the pool after the pool was cleared");
        }

        testGlobals.put("testGlobals", testGlobals);
        scripting.execute(new File(scriptsDirectory, "callScriptFromScript.java"), testGlobals);

        if (scripting.getScriptingEnginePoolObjectCount() != 4) {
            throw new Exception("Number of engines in pool didn't match expectations");
        }

    }
}
