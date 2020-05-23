package org.openpnp.machine.reference.pipeline.wizards;

import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.spi.Pipeline;

/**
 * The idea of this class is to give an overview which items do use the pipeline
 */
public class PipelineUsageWizard extends AbstractConfigurationWizard {

    private Pipeline pipeline;

    public PipelineUsageWizard(Pipeline pipeline) {
        super();
        this.pipeline = pipeline;
    }

    @Override
    public void createBindings() {

    }
}
