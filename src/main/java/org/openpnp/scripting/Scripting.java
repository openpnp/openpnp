package org.openpnp.scripting;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.openpnp.gui.MainFrame;
import org.openpnp.model.Configuration;
import org.pmw.tinylog.Logger;

import com.google.common.io.Files;

import bsh.engine.BshScriptEngineFactory;

public class Scripting {
    private final ScriptEngineManager manager = new ScriptEngineManager();
    private final String[] extensions;
    private final File scriptsDirectory;
    private final File eventsDirectory;
    private final HashMap<String, ScriptEngine> engineCache;

    public Scripting(File scriptsDirectory) {
        this.scriptsDirectory = scriptsDirectory;
        this.engineCache = new HashMap<>();

        if (scriptsDirectory == null) {
            extensions = null;
            scriptsDirectory = null;
            eventsDirectory = null;
            return;
        }

        // Collect all the script filename extensions we know how to handle from the list of
        // available scripting engines.
        List<ScriptEngineFactory> factories = manager.getEngineFactories();
        Set<String> extensions = new HashSet<>();
        for (ScriptEngineFactory factory : factories) {
            for (String ext : factory.getExtensions()) {
                extensions.add(ext.toLowerCase());
            }
        }

        // Hack to fix BSH on Windows. See https://github.com/openpnp/openpnp/issues/462
        manager.registerEngineExtension("bsh", new BshScriptEngineFactory());
        manager.registerEngineExtension("java", new BshScriptEngineFactory());
        extensions.add("bsh");
        extensions.add("java");

        this.extensions = extensions.toArray(new String[] {});

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
        return extensions;
    }

    public File getScriptsDirectory() {
        return scriptsDirectory;
    }

    public File getEventsDirectory() {
        return eventsDirectory;
    }

    public void execute(String script) throws Exception {
        execute(new File(script), null);
    }

    public void execute(File script) throws Exception {
        execute(script, null);
    }

    public void execute(String script, Map<String, Object> additionalGlobals) throws Exception {
        execute(new File(script), additionalGlobals );
    }

    public void execute(File script, Map<String, Object> additionalGlobals) throws Exception {
        String extension = Files.getFileExtension(script.getName());
        ScriptEngine engine;
        boolean useCache = Configuration.get()
                                        .getMachine()
                                        .isCacheScriptingEngines();

        long startTimeNs;
        long elapsedTimeNs;

        if (useCache && engineCache.containsKey(extension)) {
            engine = engineCache.get(extension);
            Logger.trace(engine + "scripting engine found in cache");
        }
        else {
            startTimeNs = System.nanoTime();
            engine = engineCache.getOrDefault(extension, manager.getEngineByExtension(extension));
            elapsedTimeNs = System.nanoTime() - startTimeNs;

            if (useCache) {
                Logger.trace(engine + "scripting engine not found in cache, loaded in "
                        + elapsedTimeNs / 1E6 + " milliseconds");
                engineCache.put(extension, engine);
            }
            else {
                Logger.trace("Caching disabled, " + engine + "scripting engine loaded in "
                        + elapsedTimeNs / 1E6 + " milliseconds");
            }
        }

        engine.put("config", Configuration.get());
        engine.put("machine", Configuration.get()
                                           .getMachine());
        engine.put("gui", MainFrame.get());
        engine.put("scripting", this);
        engine.put(ScriptEngine.FILENAME, script.getName());

        if (additionalGlobals != null) {
            for (String name : additionalGlobals.keySet()) {
                engine.put(name, additionalGlobals.get(name));
            }
        }

        try (FileReader reader = new FileReader(script)) {
            startTimeNs = System.nanoTime();
            engine.eval(reader);
            elapsedTimeNs = System.nanoTime() - startTimeNs;
            Logger.trace("Script " + script.getName() + " executed in " + elapsedTimeNs / 1E6
                    + " milliseconds");
        }
    }

    public void on(String event, Map<String, Object> globals) throws Exception {
        Logger.trace("Scripting.on " + event);
        if (eventsDirectory == null) {
            return;
        }
        for (File script : FileUtils.listFiles(eventsDirectory, extensions, false)) {
            if (!script.isFile()) {
                continue;
            }
            if (FilenameUtils.getBaseName(script.getName()).equals(event)) {
                Logger.trace("Scripting.on found " + script.getName());
                execute(script, globals);
            }
        }
    }

    public void clearScriptingEnginesCache() {
        if (engineCache.size() == 0) {
            Logger.info("No scripting engines cached, nothing to do");
        }

        List<String> clearedEngines = new ArrayList<String>();
        for (Map.Entry<String, ScriptEngine> entry : engineCache.entrySet()) {
            String engineInfo = "[" + entry.getKey() + ", " + entry.getValue() + "]";
            clearedEngines.add(engineInfo);
        }
        engineCache.clear();
        Logger.info("Cleared " + clearedEngines.size() + " scripting engines from cache: "
                + String.join(", ", clearedEngines));
    }

    public int getCachedScriptingEnginesCount() {
        return engineCache.size();
    }
}
