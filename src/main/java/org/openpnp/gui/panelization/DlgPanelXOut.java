package org.openpnp.gui.panelization;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import org.openpnp.gui.JobPanel;
import org.openpnp.gui.support.Helpers;

public class DlgPanelXOut extends JDialog {
    private final Action okAction = new SwingAction();
    private final Action cancelAction = new SwingAction_1();
    private final JobPanel jobPanel;

    private JPanel checkBoxPanel;
    private JTextField textFieldPCBColumns;
    
    public DlgPanelXOut(Frame parent, JobPanel jobPanel) {
        super(parent, "", true);
        this.jobPanel = jobPanel;
        
        getRootPane().setLayout(new BoxLayout(getRootPane(), BoxLayout.Y_AXIS));

        // Header
        JPanel headerPanel = new JPanel();
        // headerPanel.setLayout(new FlowLayout());
        headerPanel.add(new JLabel(
                "<html>Select the PCB to be DISABLED in the panel.<p>Note that the lower left panel is designated 1,1</html>"));
        getRootPane().add(headerPanel);

        // Panel with Checkboxes
        int cols = jobPanel.getJob().getPanels().get(0).getColumns();
        int rows = jobPanel.getJob().getPanels().get(0).getRows();
        checkBoxPanel = new JPanel();
        checkBoxPanel.setBorder(new EmptyBorder(0, 30, 0, 0));
        checkBoxPanel.setLayout(new GridLayout(rows, cols));
        getRootPane().add(checkBoxPanel);

        // Checkboxes will be added to the grid as columns from upper left
        // to lower right. The
        // board locations are stored as a linear array from lower left to
        // upper right. To help
        // sort this out, we store the linear array offset with each
        // checkbox so that we don't
        // have to deal with this mapping outside of the few lines below.
        for (int i = 0; i < rows * cols; i++) {
            int x = i % cols;
            int y = i / cols;
            String lbl = String.format("%d,%d", x + 1, rows - y);
            JCheckBox cb = new JCheckBox(lbl);
            cb.putClientProperty("index", (rows - y - 1) * cols + x);
            checkBoxPanel.add(cb);
        }

        // Footer
        JPanel footerPanel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) footerPanel.getLayout();
        flowLayout.setAlignment(FlowLayout.RIGHT);
        getRootPane().add(footerPanel);

        JButton btnCancel = new JButton("Cancel");
        btnCancel.setAction(cancelAction);
        footerPanel.add(btnCancel);

        JButton btnImport = new JButton("OK");
        btnImport.setAction(okAction);
        footerPanel.add(btnImport);

        setSize(400, 400);
        setLocationRelativeTo(parent);

        JRootPane rootPane = getRootPane();
        KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(stroke, "ESCAPE");
        rootPane.getActionMap().put("ESCAPE", cancelAction);
    }

    private class SwingAction extends AbstractAction {
        public SwingAction() {
            putValue(NAME, "OK");
            putValue(SHORT_DESCRIPTION, "OK");
        }

        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < checkBoxPanel.getComponentCount(); i++) {
                JCheckBox cb = (JCheckBox) checkBoxPanel.getComponent(i);
                int index = (int) cb.getClientProperty("index");
                jobPanel.getJob().getBoardLocations().get(index).setEnabled(!cb.isSelected());
            }

            jobPanel.refresh();
            Helpers.selectFirstTableRow(jobPanel.getBoardLocationsTable());

            setVisible(false);
        }
    }

    private class SwingAction_1 extends AbstractAction {
        public SwingAction_1() {
            putValue(NAME, "Cancel");
            putValue(SHORT_DESCRIPTION, "Cancel");
        }

        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }
}