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
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.gui.JobPanel;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;

public class DlgAutoPanelize extends JDialog {
    final private JobPanel jobPanel;
    
    private JSpinner textFieldPCBColumns;
    private JSpinner textFieldPCBRows;
    private JTextField textFieldboardXSpacing;
    private JTextField textFieldboardYSpacing;
    private JTextField textFieldboardPanelFid1X;
    private JTextField textFieldboardPanelFid1Y;
    private JTextField textFieldboardPanelFid2X;
    private JTextField textFieldboardPanelFid2Y;
    private JTextField textFieldFidDiameter;
    private JComboBox partsComboBox;
    private JCheckBox checkFidsCheckBox;
    private final Action okAction = new SwingAction();
    private final Action cancelAction = new SwingAction_1();

    public DlgAutoPanelize(Frame parent, JobPanel jobPanel) {
        super(parent, "Panelization Settings", true);
        this.jobPanel = jobPanel;
        
        getRootPane().setLayout(new BoxLayout(getRootPane(), BoxLayout.Y_AXIS));

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Panelize Parameters ", TitledBorder.LEADING,
                TitledBorder.TOP, null, null));
        getRootPane().add(panel);

        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        panel.setLayout(new GridLayout(0, 2, 20, 20));

        // Specify a placeholder panel for now if we don't have one already
        if ((jobPanel.getJob().getPanels() == null)
                || (jobPanel.getJob().getPanels() != null && jobPanel.getJob().getPanels().size() == 0)) {
            jobPanel.getJob().addPanel(
                    new Panel("Panel1", 3, 3, new Length(0, LengthUnit.Millimeters),
                            new Length(0, LengthUnit.Millimeters), "", false,
                            new Placement("PanelFid1"), new Placement("PanelFid2")));
        }
        
        // Row and column
        int rows = jobPanel.getJob().getPanels().get(0).getRows();
        int cols = jobPanel.getJob().getPanels().get(0).getColumns();

        panel.add(new JLabel("Number of Columns", JLabel.RIGHT), "2, 2, right, default");
        textFieldPCBColumns = new JSpinner(new SpinnerNumberModel(cols, 1, 6, 1));
        panel.add(textFieldPCBColumns, "4, 2, fill, default");

        panel.add(new JLabel("Number of Rows", JLabel.RIGHT), "2, 4, right, default");
        textFieldPCBRows = new JSpinner(new SpinnerNumberModel(rows, 1, 6, 1));
        panel.add(textFieldPCBRows, "4, 4, fill, default");

        // Spacing
        panel.add(new JLabel("X Spacing", JLabel.RIGHT), "2, 6, right, default");
        textFieldboardXSpacing = new JTextField();
        textFieldboardXSpacing.setText(String.format("%.3f", jobPanel.getJob().getPanels().get(0)
                .getXGap().convertToUnits(Configuration.get().getSystemUnits()).getValue()));
        panel.add(textFieldboardXSpacing, "4, 6, fill, default");

        panel.add(new JLabel("Y Spacing", JLabel.RIGHT), "2, 8, right, default");
        textFieldboardYSpacing = new JTextField();
        textFieldboardYSpacing.setText(String.format("%.3f", jobPanel.getJob().getPanels().get(0)
                .getYGap().convertToUnits(Configuration.get().getSystemUnits()).getValue()));
        panel.add(textFieldboardYSpacing, "4, 8, fill, default");

        // Fiducial coords

        Location fid0Loc = jobPanel.getJob().getPanels().get(0).getFiducials().get(0).getLocation();
        panel.add(new JLabel("Panel Fid1 X", JLabel.RIGHT), "2, 10, right, default");
        textFieldboardPanelFid1X = new JTextField();
        textFieldboardPanelFid1X.setText(String.format("%.3f",
                fid0Loc.convertToUnits(Configuration.get().getSystemUnits()).getX()));
        panel.add(textFieldboardPanelFid1X, "4, 10, fill, default");

        panel.add(new JLabel("Panel Fid1 Y", JLabel.RIGHT), "2, 12, right, default");
        textFieldboardPanelFid1Y = new JTextField();
        textFieldboardPanelFid1Y.setText(String.format("%.3f",
                fid0Loc.convertToUnits(Configuration.get().getSystemUnits()).getY()));
        panel.add(textFieldboardPanelFid1Y, "4, 12, fill, default");

        Location fid1Loc = jobPanel.getJob().getPanels().get(0).getFiducials().get(1).getLocation();
        panel.add(new JLabel("Panel Fid2 X", JLabel.RIGHT), "2, 14, right, default");
        textFieldboardPanelFid2X = new JTextField();
        textFieldboardPanelFid2X.setText(String.format("%.3f",
                fid1Loc.convertToUnits(Configuration.get().getSystemUnits()).getX()));
        panel.add(textFieldboardPanelFid2X, "4, 14, fill, default");

        panel.add(new JLabel("Panel Fid2 Y", JLabel.RIGHT), "2, 16, right, default");
        textFieldboardPanelFid2Y = new JTextField();
        textFieldboardPanelFid2Y.setText(String.format("%.3f",
                fid1Loc.convertToUnits(Configuration.get().getSystemUnits()).getY()));
        panel.add(textFieldboardPanelFid2Y, "4, 16, fill, default");

        panel.add(new JLabel("Global Fiducial Part", JLabel.RIGHT), "2, 16, right, default");
        partsComboBox = new JComboBox(new PartsComboBoxModel());
        partsComboBox.setRenderer(new IdentifiableListCellRenderer<Part>());

        // Very verbose...there must be a better way
        for (int i = 0; i < partsComboBox.getItemCount(); i++) {
            Part p = (Part) (partsComboBox.getItemAt(i));
            if (p.getId().equals(jobPanel.getJob().getPanels().get(0).getPartId())) {
                partsComboBox.setSelectedIndex(i);
                break;
            }
        }

        panel.add(partsComboBox, "4, 18, fill, default");

        panel.add(new JLabel("Check Fiducials", JLabel.RIGHT), "2, 20, right, default");
        checkFidsCheckBox = new JCheckBox();
        checkFidsCheckBox.setSelected(jobPanel.getJob().getPanels().get(0).isCheckFiducials());
        panel.add(checkFidsCheckBox, "4, 20, fill, default");

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

        setSize(400, 600);
        setResizable(false);
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
            int cols = (int) (textFieldPCBColumns.getValue());
            int rows = (int) (textFieldPCBRows.getValue());
            double gapX = Double.parseDouble(textFieldboardXSpacing.getText());
            double gapY = Double.parseDouble(textFieldboardYSpacing.getText());
            double globalFid1X = Double.parseDouble(textFieldboardPanelFid1X.getText());
            double globalFid1Y = Double.parseDouble(textFieldboardPanelFid1Y.getText());
            double globalFid2X = Double.parseDouble(textFieldboardPanelFid2X.getText());
            double globalFid2Y = Double.parseDouble(textFieldboardPanelFid2Y.getText());
            Part part = (Part) partsComboBox.getSelectedItem();

            // The selected PCB is the one we'll panelize
            BoardLocation rootPCB = jobPanel.getSelectedBoardLocation();

            Placement p0 = new Placement("PanelFid1");
            p0.setType(Placement.Type.Fiducial);
            p0.setLocation(
                    new Location(Configuration.get().getSystemUnits(), globalFid1X, globalFid1Y,
                            rootPCB.getLocation().getZ(), rootPCB.getLocation().getRotation()));
            p0.setPart(part);
            Placement p1 = new Placement("PanelFid2");
            p0.setType(Placement.Type.Fiducial);
            p1.setLocation(
                    new Location(Configuration.get().getSystemUnits(), globalFid2X, globalFid2Y,
                            rootPCB.getLocation().getZ(), rootPCB.getLocation().getRotation()));
            p1.setPart(part);

            Panel pcbPanel = new Panel("Panel1", cols, rows,
                    new Length(gapX, Configuration.get().getSystemUnits()),
                    new Length(gapY, Configuration.get().getSystemUnits()), part.getId(),
                    checkFidsCheckBox.isSelected(), p0, p1);

            jobPanel.getJob().removeAllPanels();

            if ((rows == 1) && (cols == 1)) {
                // Here, the user has effectively shut off panelization by
                // specifying row = col = 1. In this case, we don't
                // want the panelization info to appear in the job file any
                // longer. We also need to remove all the boards created
                // by the panelization previously EXCEPT for the root PCB.
                // Remember, too, that the condition upon entry into
                // this dlg was that there was a single board in the list.
                // When this feature is turned off, there will again
                // be a single board in the list
                BoardLocation b = jobPanel.getJob().getBoardLocations().get(0);
                jobPanel.getJob().removeAllBoards();
                jobPanel.getJob().addBoardLocation(b);
                jobPanel.refresh();
            }
            else {
                // Here, panelization is active.
                jobPanel.getJob().addPanel(pcbPanel);
                jobPanel.populatePanelSettingsIntoBoardLocations();
            }
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