package org.openpnp.vision.pipeline.stages;
import org.I18n.I18n;


import org.openpnp.ConfigurationListener;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Actuator.ActuatorValueType;
import org.openpnp.spi.Head;
import org.openpnp.spi.base.AbstractActuator;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Stage;
import org.openpnp.vision.pipeline.TerminalException;
import org.openpnp.vision.pipeline.ui.PipelinePropertySheetTable;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

@Stage(description="Performs simple actuator write. Machine must be connected, otherwise error is thrown.")
public class ActuatorWrite extends CvStage {
    @Attribute(required=false)
    protected String actuatorName = "";

    @Deprecated
    @Attribute(required=false)
    private Actuator.ActuatorValueType actuatorType = null;

    @Deprecated
    @Attribute(required=false)
    private double actuatorValue;

    // For an Object property we need @Element persistence, therefore the original actuatorValue is deprecated.
    @Element(required=false)
    protected Object actuatorWriteValue = true;

    public String getActuatorName() {
        return actuatorName;
    }

    public void setActuatorName(String actuatorName) {
        this.actuatorName = actuatorName;
    }

    public Object getActuatorWriteValue() {
        return actuatorWriteValue;
    }

    public void setActuatorWriteValue(Object actuatorWriteValue) {
        this.actuatorWriteValue = actuatorWriteValue;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (actuatorName == null || actuatorName.equals("")) {
            Logger.warn("No actuator name specified for pipeline {}.", getName());
        }
        else {
            Actuator actuator = getActuator();
            if (actuator == null)
            {
                throw new Exception("Actuator writing (CvStage operation) failed. Unable to find an actuator named " + actuatorName);
            }
            try {
                // Make sure this happens in a machine task, but wait for completion.
                Configuration.get().getMachine().execute(() -> {
                    actuator.actuate(actuatorWriteValue);
                    return null;
                    });
            }
            catch (Exception e) {
                // These machine exceptions are terminal to the pipeline.
                throw new TerminalException(e);
            }
        }
        return null;
    }

    protected Actuator getActuator() {
        Actuator actuator =  Configuration.get().getMachine().getActuatorByName(actuatorName);
        if (actuator == null) {
            for (final Head head : Configuration.get().getMachine().getHeads()) {
                actuator = head.getActuatorByName(actuatorName);
                if (actuator != null){
                    break;
                }
            }
        }
        return actuator;
    }

    @Override
    public void customizePropertySheet(PipelinePropertySheetTable table, CvPipeline pipeline) {
        super.customizePropertySheet(table, pipeline);
        Actuator actuator = getActuator();
        String propertyName = "actuatorWriteValue";
        table.customizeActuatorProperty(propertyName, actuator);
    }

    public ActuatorWrite() {
        Configuration.get().addListener(new ConfigurationListener.Adapter() {
            @Override
            public void configurationLoaded(Configuration configuration) throws Exception {
                // Migrate actuator value type.
                if (actuatorType != null) { 
                    Actuator actuator = getActuator();
                    AbstractActuator.suggestValueType(actuator, actuatorType);
                    if (actuatorType == ActuatorValueType.Boolean) {
                        actuatorWriteValue = (actuatorValue != 0.0);
                    }
                    else {
                        actuatorWriteValue = actuatorValue;
                    }
                    actuatorType = null;
                }
            }
        });
    }
}
