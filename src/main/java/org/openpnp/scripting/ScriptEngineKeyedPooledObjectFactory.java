package org.openpnp.scripting;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.pmw.tinylog.Logger;

public class ScriptEngineKeyedPooledObjectFactory
        extends BaseKeyedPooledObjectFactory<String, ScriptEngine> {
    private final ScriptEngineManager manager;

    public ScriptEngineKeyedPooledObjectFactory(ScriptEngineManager manager) {
        this.manager = manager;
    }

    @Override
    public ScriptEngine create(String engineName) throws Exception {
        long startTimeNs = System.nanoTime();
        long elapsedTimeNs;
        // `Interface PooledObjectFactory<T>` docs state "PooledObjectFactory must be thread-safe".
        // As there is no specific information about this, it is assumed that the
        // ScriptEngineFactory used by getEngineByName is thread-safe. Not being thread-safe would
        // be unreasonable for a factory class.
        ScriptEngine engine = manager.getEngineByName(engineName);
        elapsedTimeNs = System.nanoTime() - startTimeNs;
        Logger.trace(engineName + "scripting engine created in " + elapsedTimeNs / 1E6
                + " milliseconds");
        return engine;
    }

    @Override
    public PooledObject<ScriptEngine> wrap(ScriptEngine value) {
        return new DefaultPooledObject<ScriptEngine>(value);
    }
}
