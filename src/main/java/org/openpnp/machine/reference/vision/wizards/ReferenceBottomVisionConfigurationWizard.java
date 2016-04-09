package org.openpnp.machine.reference.vision.wizards;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.ui.CvPipelineEditor;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

public class ReferenceBottomVisionConfigurationWizard extends AbstractConfigurationWizard {
    private final ReferenceBottomVision bottomVision;
    private JCheckBox enabledCheckbox;

    public ReferenceBottomVisionConfigurationWizard(ReferenceBottomVision bottomVision) {
        this.bottomVision = bottomVision;

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "General", TitledBorder.LEADING, TitledBorder.TOP,
                null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(
                new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                        FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

        JLabel lblEnabled = new JLabel("Enabled?");
        panel.add(lblEnabled, "2, 2");

        enabledCheckbox = new JCheckBox("");
        panel.add(enabledCheckbox, "4, 2");

        JLabel lblPipeline = new JLabel("Pipeline");
        panel.add(lblPipeline, "2, 4");

        JButton editPipelineButton = new JButton("Edit");
        editPipelineButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // TODO: Move this into CvPipelineEditor as showDialog for convenience.
                CvPipeline pipeline = bottomVision.getPipeline();
                CvPipelineEditor editor = new CvPipelineEditor(pipeline);
                JDialog dialog = new JDialog(MainFrame.mainFrame, "Bottom Vision Pipeline");
                dialog.getContentPane().setLayout(new BorderLayout());
                dialog.getContentPane().add(editor);
                dialog.setSize(1024, 768);
                dialog.setVisible(true);
            }
        });
        panel.add(editPipelineButton, "4, 4");
    }

    @Override
    public void createBindings() {
        addWrappedBinding(bottomVision, "enabled", enabledCheckbox, "selected");
    }
}
