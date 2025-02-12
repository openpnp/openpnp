package org.openpnp.scripting;

import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.pool2.impl.DefaultPooledObjectInfo;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.Configuration;
import org.pmw.tinylog.Logger;

import com.google.common.io.Files;

import bsh.engine.BshScriptEngineFactory;

public class Scripting {
    private final ScriptEngineManager manager = new ScriptEngineManager();
    private final File scriptsDirectory;
    private final File eventsDirectory;
    private final HashMap<String, String> extensionToEngineNameMap;
    private final GenericKeyedObjectPool<String, ScriptEngine> enginePool;

    public Scripting(File scriptsDirectory) {
        this.scriptsDirectory = scriptsDirectory;
        extensionToEngineNameMap = new HashMap<>();
        enginePool = new GenericKeyedObjectPool<>(
                new ScriptEngineKeyedPooledObjectFactory(this.manager));
        // Allow unlimited engines, but evict all but five per key after a short idle time
        enginePool.setMaxTotal(-1);
        enginePool.setMaxTotalPerKey(-1);
        enginePool.setMaxIdlePerKey(8);
        enginePool.setTimeBetweenEvictionRuns(Duration.ofSeconds(5));

        if (scriptsDirectory == null) {
            eventsDirectory = null;
            return;
        }

        // In the extensionToEngineNameMap, the first name of ScriptEngineFactory.getNames() is used
        // as an identifier for each engine type. It has been validated for Jython, Nashorn and
        // Beanshell that the order of these names doesn't change between invocations.

        // Collect all the script filename extensions we know how to handle from the list of
        // available scripting engines.
        List<ScriptEngineFactory> factories = manager.getEngineFactories();
        for (ScriptEngineFactory factory : factories) {
            for (String ext : factory.getExtensions()) {
                extensionToEngineNameMap.put(ext.toLowerCase(), factory.getNames()
                                                                       .get(0));
            }
        }

        // Hack to fix BSH on Windows. See https://github.com/openpnp/openpnp/issues/462
        // Update 08/2022: Beanshell 2.1 would probably fix this as per
        // https://github.com/beanshell/beanshell/issues/24#ref-commit-8cd08f4, but is not on Maven
        // Central yet: https://github.com/beanshell/beanshell/issues/603.
        ScriptEngineFactory bshFactory = new BshScriptEngineFactory();
        manager.registerEngineName(bshFactory.getNames()
                .get(0),
                bshFactory);
        extensionToEngineNameMap.put("bsh", bshFactory.getNames()
                .get(0));
        extensionToEngineNameMap.put("java", bshFactory.getNames()
                .get(0));

        // Hack to make sure Nashorn is there.
        ScriptEngineFactory jsFactory = new NashornScriptEngineFactory();
        manager.registerEngineName(jsFactory.getNames()
                .get(0),
                jsFactory);
        extensionToEngineNameMap.put("js", jsFactory.getNames()
                .get(0));

        // Create the scripts directory if it doesn't exist.
        if (!scriptsDirectory.exists()) {
            scriptsDirectory.mkdirs();
        }

        this.eventsDirectory = new File(scriptsDirectory, "Events");
        if (!eventsDirectory.exists()) {
            eventsDirectory.mkdirs();
        }
    }

    public String[] getExtensions() {
        return extensionToEngineNameMap.keySet()
                                       .toArray(new String[0]);
    }

    public String[] getEngineNames() {
        return new HashSet<String>(extensionToEngineNameMap.values()).toArray(new String[0]);
    }

    public File getScriptsDirectory() {
        return scriptsDirectory;
    }

    public File getEventsDirectory() {
        return eventsDirectory;
    }

    public void setPoolMaxIdlePerKey(int poolMaxIdlePerKey) {
        enginePool.setMaxIdlePerKey(poolMaxIdlePerKey);
    }

    public int getPoolMaxIdlePerKey() {
        return enginePool.getMaxIdlePerKey();
    }

    public void execute(String script) throws Exception {
        execute(new File(script), null);
    }

    public void execute(File script) throws Exception {
        execute(script, null);
    }

    public void execute(String script, Map<String, Object> additionalGlobals) throws Exception {
        execute(new File(script), additionalGlobals);
    }

    public void execute(File script, Map<String, Object> additionalGlobals) throws Exception {
        String extension = Files.getFileExtension(script.getName());
        String engineName = extensionToEngineNameMap.get(extension);
        if(engineName==null) {
            throw new Exception("Unknown scripting engine for "+extension);
        }
        boolean usePool = Configuration.get()
                                       .getMachine()
                                       .isPoolScriptingEngines();
        ScriptEngine engine;
        long startTimeNs;
        long elapsedTimeNs;

        if (usePool) {
            startTimeNs = System.nanoTime();
            engine = enginePool.borrowObject(engineName);
            if(engine==null) {
                throw new Exception("Failed to borrow "+engineName+" scripting engine");
            }
            elapsedTimeNs = System.nanoTime() - startTimeNs;
            Logger.trace(engine + " scripting engine borrowed from pool in " + elapsedTimeNs / 1E6
                    + " milliseconds");
        }
        else {
            startTimeNs = System.nanoTime();
            engine = manager.getEngineByName(engineName);
            elapsedTimeNs = System.nanoTime() - startTimeNs;
            if(engine==null) {
                throw new Exception("Failed to create "+engineName+" scripting engine");
            }
            Logger.trace("Engine pooling disabled, " + engine + " scripting engine loaded in "
                    + elapsedTimeNs / 1E6 + " milliseconds");
        }

        // Explicitly re-build and set the bindings to avoid foreign (to another script hooks
        // context) references to objects from past invocations.
        Bindings bindings = engine.createBindings();
        bindings.put("config", Configuration.get());
        bindings.put("machine", Configuration.get()
                                             .getMachine());
        bindings.put("gui", MainFrame.get());
        bindings.put("scripting", this);
        bindings.put(ScriptEngine.FILENAME, script.getName());
        if (additionalGlobals != null) {
            for (String name : additionalGlobals.keySet()) {
                bindings.put(name, additionalGlobals.get(name));
            }
        }
        engine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);

        boolean execError = false;
        startTimeNs = System.nanoTime();
        try (FileReader reader = new FileReader(script)) {
            engine.eval(reader);
        }
        catch (Exception e) {
            execError = true;
            throw e;
        }
        finally {
            elapsedTimeNs = System.nanoTime() - startTimeNs;

            if (usePool) {
                enginePool.returnObject(extensionToEngineNameMap.get(extension), engine);
            }

            if (execError) {
                Logger.trace(
                        "Script " + script.getName() + " execution aborted with exception after "
                                + elapsedTimeNs / 1E6 + " milliseconds");
            }
            else {
                Logger.trace("Script " + script.getName() + " executed in " + elapsedTimeNs / 1E6
                        + " milliseconds");
            }
        }
    }

    public void on(String event, Map<String, Object> globals) throws Exception {
        Logger.trace("Scripting.on " + event);
        if (eventsDirectory == null) {
            return;
        }
        for (File script : FileUtils.listFiles(eventsDirectory, getExtensions(), false)) {
            if (!script.isFile()) {
                continue;
            }
            if (FilenameUtils.getBaseName(script.getName())
                             .equals(event)) {
                Logger.trace("Scripting.on found " + script.getName());
                execute(script, globals);
            }
        }
    }

    public void clearScriptingEnginePool() {
        if (enginePool.listAllObjects()
                      .size() == 0) {
            Logger.info("No scripting engines in pool, nothing to do");
            return;
        }

        ArrayList<DefaultPooledObjectInfo> clearedEngines = new ArrayList<>();
        enginePool.listAllObjects()
                  .values()
                  .forEach(clearedEngines::addAll);
        enginePool.clear();
        Logger.info("Cleared " + clearedEngines.size() + " scripting engines from pool: ");
        for (DefaultPooledObjectInfo clearedEngine : clearedEngines) {
            Logger.info(clearedEngine.getPooledObjectToString() + " engine created "
                    + clearedEngine.getCreateTimeFormatted() + ", borrowed "
                    + clearedEngine.getBorrowedCount() + " times");
        }
    }

    public int getScriptingEnginePoolObjectCount() {
        ArrayList<DefaultPooledObjectInfo> allEngines = new ArrayList<>();
        enginePool.listAllObjects()
                  .values()
                  .forEach(allEngines::addAll);
        return allEngines.size();
    }
}
