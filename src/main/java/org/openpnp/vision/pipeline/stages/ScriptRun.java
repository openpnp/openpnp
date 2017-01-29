package org.openpnp.vision.pipeline.stages;

import java.io.File;
import java.io.FileReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;

import com.google.common.io.Files;

/**
 * Run an arbitrary script file using the built in scripting engine. pipeline and stage
 * are exposed as globals for use by the script. 
 */
public class ScriptRun extends CvStage {
    @Attribute
    private File file = new File("");

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (!file.exists()) {
            return null;
        }

        ScriptEngine engine = new ScriptEngineManager()
                .getEngineByExtension(Files.getFileExtension(file.getName()));
        
        if (engine == null) {
            throw new Exception("Unable to find scriping engine for " + file);
        }

        engine.put("pipeline", pipeline);
        engine.put("stage", this);

        try (FileReader reader = new FileReader(file)) {
            Object result = engine.eval(reader);
            if (result instanceof Result) {
                return (Result) result;
            }
            return null;
        }


    }
}
