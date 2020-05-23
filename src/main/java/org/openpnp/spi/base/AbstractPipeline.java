package org.openpnp.spi.base;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.pipeline.wizards.PipelineConfigurationWizard;
import org.openpnp.machine.reference.pipeline.wizards.PipelineUsageWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Board;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Pipeline;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.WizardConfigurable;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

import javax.swing.*;
import java.awt.event.ActionEvent;

public abstract class AbstractPipeline extends AbstractModelObject implements Pipeline, WizardConfigurable {

    @Attribute
    protected String id;

    @Attribute(required = false)
    protected String name;

    @Element(required = false)
    protected CvPipeline pipeline = createDefaultCvPipeline();

    public AbstractPipeline() {
        this.id = Configuration.createId("CVP");
        this.name = getClass().getSimpleName();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
        firePropertyChange("name", null, name);
    }

    public CvPipeline getCvPipeline() {
        return pipeline;
    }

    @Override
    public void resetCvPipeline() {
        this.pipeline = createDefaultCvPipeline();
    }

    public abstract Board.Side getBoardSide();

    @Override
    public Camera getCamera() throws Exception {

        Board.Side side = getBoardSide();

        if(side == Board.Side.Top) {
            return VisionUtils.getTopVisionCamera();
        }

        if(side == Board.Side.Bottom) {
            return VisionUtils.getBottomVisionCamera();
        }

        throw new Exception("No suitable camera found on the machine.");
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
            new PropertySheetWizardAdapter(getConfigurationWizard()),
            new PropertySheetWizardAdapter(new PipelineUsageWizard(this), "Usage")
        };
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return new Action[] { deleteAction };
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new PipelineConfigurationWizard(this);
    }

    public Action deleteAction = new AbstractAction("Delete Pipeline") {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Delete Pipeline");
            putValue(SHORT_DESCRIPTION, "Delete the currently selected pipeline.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int ret = JOptionPane.showConfirmDialog(MainFrame.get(),
                    "Are you sure you want to delete " + getName() + "?",
                    "Delete " + getName() + "?", JOptionPane.YES_NO_OPTION);
            if (ret == JOptionPane.YES_OPTION) {
                Configuration.get().getMachine().removePipeline(AbstractPipeline.this);
            }
        }
    };
}
