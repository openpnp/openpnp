package org.openpnp.vision.pipeline.stages;

import java.io.File;
import java.io.FileReader;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

import com.google.common.io.Files;

@Stage(description="Run an arbitrary script file using the built in scripting engine. pipeline and stage are exposed as globals for use by the script. To return a pipeline result you can't use a return statement, but instead just let the object be the last thing the script evaluates.")
public class ScriptRun extends CvStage {
    @Attribute
    private File file = new File("");

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Attribute
    private String args = new String("");

    public String getArgs() {
        return args;
    }

    public void setArgs(String args) {
        this.args = args;
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
        engine.put("args",args);

        try (FileReader reader = new FileReader(file)) {
            Object result = engine.eval(reader);
            if (result instanceof Result) {
                return (Result) result;
            }
            return null;
        }
    }
}
