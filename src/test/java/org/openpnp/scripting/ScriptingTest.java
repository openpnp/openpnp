package org.openpnp.scripting;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

        // File extensions that there are test files for
        List<String> availableTestFileExtensions = Arrays.asList("java", "bsh", "js", "py");
        // Only retain the set of extensions for which there is also a matching
        // scripting engine, avoids environment-dependent test failures
        List<String> supportedTestFileExtensions = new ArrayList<>(availableTestFileExtensions);
        supportedTestFileExtensions.retainAll(Arrays.asList(scripting.getExtensions()));

        if (supportedTestFileExtensions.size() != availableTestFileExtensions.size()) {
            List<String> engineMissingTestFileExtensions =
                    new ArrayList<>(availableTestFileExtensions);
            engineMissingTestFileExtensions.removeAll(Arrays.asList(scripting.getExtensions()));
            System.out.println(
                    "Warning: Some script engine types for which test files exist are missing from the environment, these engines cannot be tested: "
                            + engineMissingTestFileExtensions);
        }

        // Copy the supported scripts over to the scripts directory.
        for (String testFileExtension : supportedTestFileExtensions) {
            String testFileName = "testEvent." + testFileExtension;
            FileUtils.copyURLToFile(
                    ClassLoader.getSystemResource("config/ScriptingTest/Events/" + testFileName),
                    new File(scriptsDirectory, "Events/" + testFileName));
        }

        // Copy the required scripts over to the scripts directory.
        FileUtils.copyURLToFile(
                ClassLoader.getSystemResource("config/ScriptingTest/callScriptFromScript.java"),
                new File(scriptsDirectory, "callScriptFromScript.java"));

        FileUtils.copyURLToFile(
                ClassLoader.getSystemResource("config/ScriptingTest/throwException.java"),
                new File(scriptsDirectory, "throwException.java"));

        ConcurrentHashMap<String, String> testResults = new ConcurrentHashMap<>();
        HashMap<String, Object> testGlobals = new HashMap<>();
        testGlobals.put("testResults", testResults);
        testGlobals.put("threadedTest", false);

        // ==== Test 1 ====
        // Check that general script execution works
        // ================

        referenceMachine.setPoolScriptingEngines(false);
        scripting.on("testEvent", testGlobals);
        System.out.println("Results from scripts: " + testResults);

        // Check if all scripts have been executed and if the execution yielded the
        // expected result
        for (String extension : supportedTestFileExtensions) {
            if (testResults.get(extension) != "ok") {
                throw new Exception("Script execution for " + extension
                        + " file extension didn't return expected result");
            }
        }

        if (scripting.getScriptingEnginePoolObjectCount() != 0) {
            throw new Exception("Engines in the pool even if pooling is disabled");
        }

        // ==== Test 2 ====
        // Check if engines are returned to the pool correctly even when the script throws an
        // exception
        // ================

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

        // Do the same thing again to test if engines are returned to the pool even if
        // the script throws an exception
        try {
            scripting.execute(new File(scriptsDirectory, "throwException.java"), testGlobals);
        }
        catch (ScriptException e) {
        }
        if (scripting.getScriptingEnginePoolObjectCount() != 1) {
            throw new Exception("Failed to return engine to the pool after script exception");
        }

        // ==== Test 3 ====
        // Check if engines are cached correctly in general
        // ================

        scripting.on("testEvent", testGlobals);

        if (scripting.getScriptingEnginePoolObjectCount() != scripting.getEngineNames().length) {
            throw new Exception("Number of engines in pool didn't match expectations");
        }

        // ==== Test 4 ====
        // Check if clearing the pool works
        // ================

        scripting.clearScriptingEnginePool();

        if (scripting.getScriptingEnginePoolObjectCount() != 0) {
            throw new Exception("Engines left in the pool after the pool was cleared");
        }

        // ==== Test 5 ====
        // Do some heavy parallel threaded execution of scripts to validate robustness
        // ================

        testGlobals.put("threadedTest", true);

        int numThreads = 20;
        ExecutorService es = Executors.newCachedThreadPool();
        for (int threadId = 0; threadId < numThreads; threadId++) {
            final Integer innerThreadId = new Integer(threadId);
            es.execute(new Runnable() {
                @Override
                public void run() {
                    HashMap<String, Object> testGlobalsCopy = new HashMap<>(testGlobals);
                    testGlobalsCopy.put("threadId", innerThreadId);
                    testGlobalsCopy.put("testGlobals", testGlobalsCopy);
                    try {
                        scripting.execute(new File(scriptsDirectory, "callScriptFromScript.java"),
                                testGlobalsCopy);
                    }
                    catch (Exception e) {
                        // Exception can be ignored here as the test results are vaildated
                        // explicitly later on
                    }
                }
            });
        }
        es.shutdown();
        // 3 minutes for slow systems
        es.awaitTermination(3, TimeUnit.MINUTES);

        for (int threadId = 0; threadId < numThreads; threadId++) {
            for (String extension : supportedTestFileExtensions) {
                if (testResults.getOrDefault(extension + threadId, "") != "ok") {
                    throw new Exception("Threading test didn't return a OK result for thread "
                            + extension + threadId);
                }
            }
            if (testResults.getOrDefault("base" + threadId, "") != "ok") {
                throw new Exception(
                        "Threading test didn't return a OK result for thread base" + threadId);
            }
        }

        if (scripting.getScriptingEnginePoolObjectCount() < scripting.getPoolMaxIdlePerKey() * scripting.getEngineNames().length) {
            throw new Exception(
                "Number of engines in pool after threading test is too low");
        }

        System.out.println("All " + numThreads + " threads returned the expected results");
    }
}
