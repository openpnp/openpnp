package org.openpnp.gui;

import org.openpnp.gui.support.TableComboBoxModel;
import org.openpnp.model.Configuration;
import org.openpnp.model.Pipeline;

@SuppressWarnings("serial")
public class PipelinesComboBoxModel extends TableComboBoxModel<Pipeline> {

    public PipelinesComboBoxModel() {
        super("pipelines");
    }

    @Override
    protected void addAllElements() {
        Configuration.get().getPipelines().stream().sorted(comparator).forEach(this::addElement);
    }
}
