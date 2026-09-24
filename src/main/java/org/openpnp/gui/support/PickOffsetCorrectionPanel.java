package org.openpnp.gui.support;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.PickOffsetCorrection;
import org.openpnp.spi.PickOffsetCorrectableFeeder;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;

/**
 * Shared property-sheet tab for the {@link PickOffsetCorrection} of a {@link PickOffsetCorrectableFeeder}.
 * It shows the live state, some debug info and allows the user to configure the PickOffsetCorrection.
 */
@SuppressWarnings("serial")
public class PickOffsetCorrectionPanel extends JPanel {
    private final PickOffsetCorrectableFeeder owner;
    private final PickOffsetCorrection correction;
    private final boolean nudging;

    private final LengthUnit userFavoriteUnit = Configuration.get().getSystemUnits();

    private final JCheckBox enabledCheckBox =
            new JCheckBox(Translations.getString("PickOffsetCorrectionPanel.EnabledCheckBox.text")); //$NON-NLS-1$
    private final JLabel offsetLabel = new JLabel();
    private final JLabel magnitudeLabel = new JLabel();
    private final JLabel lastVisionLabel = new JLabel();
    private final JLabel samplesLabel = new JLabel();
    private final JSpinner iTermSpinner =
            new JSpinner(new SpinnerNumberModel(PickOffsetCorrection.DEFAULT_I_TERM, 0.01, 1.0, 0.05));
    private final JSpinner limitSpinner = new JSpinner();

    // Do not show if the feeder doesn't support part pitch nudging.
    private JCheckBox nudgingCheckBox;
    private final JLabel lastNudgeLabel = new JLabel();
    private final JLabel nudgeSumLabel = new JLabel();
    private final JLabel nudgeAbsSumLabel = new JLabel();
    private JToggleButton xPlusButton;
    private JToggleButton xMinusButton;
    private JToggleButton yPlusButton;
    private JToggleButton yMinusButton;

    // True while refresh() is pushing model values into the controls, so their listeners don't echo
    // the change straight back into the model.
    private boolean refreshing;

    private final PropertyChangeListener listener = e -> SwingUtilities.invokeLater(this::refresh);

    public PickOffsetCorrectionPanel(PickOffsetCorrectableFeeder owner) {
        this.owner = owner;
        this.correction = owner.getPickOffsetCorrection();
        this.nudging = correction instanceof PickOffsetCorrection.Nudging;

        setBorder(new EmptyBorder(8, 8, 8, 8));
        FormLayout layout = new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,},
                new com.jgoodies.forms.layout.RowSpec[] {});
        setLayout(layout);

        enabledCheckBox.setToolTipText(Translations.getString("PickOffsetCorrectionPanel.EnabledCheckBox.toolTipText")); //$NON-NLS-1$
        enabledCheckBox.addActionListener(e -> correction.setEnabled(enabledCheckBox.isSelected()));
        span(layout, enabledCheckBox);

        separator(layout);

        String offsetTip = Translations.getString("PickOffsetCorrectionPanel.OffsetLabel.toolTipText"); //$NON-NLS-1$
        offsetLabel.setToolTipText(offsetTip);
        labelled(layout, Translations.getString("PickOffsetCorrectionPanel.OffsetLabel.text"), offsetTip, //$NON-NLS-1$
                offsetLabel);

        String iTermTip = Translations.getString("PickOffsetCorrectionPanel.ITermLabel.toolTipText"); //$NON-NLS-1$
        iTermSpinner.setToolTipText(iTermTip);
        iTermSpinner.addChangeListener(e -> {
            if (!refreshing) {
                correction.setITerm(((Number) iTermSpinner.getValue()).doubleValue());
            }
        });
        labelled(layout, Translations.getString("PickOffsetCorrectionPanel.ITermLabel.text"), iTermTip, //$NON-NLS-1$
                iTermSpinner);


        String limitTip = Translations.getString("PickOffsetCorrectionPanel.CorrectionMagnitudeLabel.toolTipText"); //$NON-NLS-1$
        // display: "0.230 unit / <limit> unit".
        // <limit> is a field allowing the user to configure the limit.
        limitSpinner.setToolTipText(limitTip);
        limitSpinner.setModel(new SpinnerNumberModel(toUnit(PickOffsetCorrection.DEFAULT_CORRECTION_LIMIT),
                toUnit(mmLength(0.1)), toUnit(mmLength(Double.POSITIVE_INFINITY)), toUnit(mmLength(0.5))));
        limitSpinner.addChangeListener(e -> {
            if (!refreshing) {
                correction.setCorrectionLimit(fromUnit(((Number) limitSpinner.getValue()).doubleValue()));
            }
        });
        JPanel magnitudeRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        magnitudeRow.add(magnitudeLabel);
        JLabel slash = new JLabel("/");
        slash.setToolTipText(limitTip);
        magnitudeRow.add(slash);
        magnitudeRow.add(limitSpinner);
        JLabel limitUnit = new JLabel(userFavoriteUnit.getShortName());
        limitUnit.setToolTipText(limitTip);
        magnitudeRow.add(limitUnit);
        labelled(layout, Translations.getString("PickOffsetCorrectionPanel.CorrectionMagnitudeLabel.text"), //$NON-NLS-1$
                limitTip, magnitudeRow);

        separator(layout);

        labelled(layout, Translations.getString("PickOffsetCorrectionPanel.LastVisionResultLabel.text"), null, //$NON-NLS-1$
                lastVisionLabel);


        String samplesTip = Translations.getString("PickOffsetCorrectionPanel.PendingVisionSamplesLabel.toolTipText"); //$NON-NLS-1$
        samplesLabel.setToolTipText(samplesTip);
        labelled(layout, Translations.getString("PickOffsetCorrectionPanel.PendingVisionSamplesLabel.text"), //$NON-NLS-1$
                samplesTip, samplesLabel);

        if (nudging) {
            separator(layout);

            nudgingCheckBox = new JCheckBox(
                    Translations.getString("PickOffsetCorrectionPanel.NudgingCheckBox.text")); //$NON-NLS-1$
            nudgingCheckBox.setToolTipText(Translations.getString("PickOffsetCorrectionPanel.NudgingCheckBox.toolTipText")); //$NON-NLS-1$
            nudgingCheckBox.addActionListener(e -> ((PickOffsetCorrection.Nudging) correction)
                    .setNudgingEnabled(nudgingCheckBox.isSelected()));
            span(layout, nudgingCheckBox);

            span(layout, buildFeedPlanePanel());

            labelled(layout, Translations.getString("PickOffsetCorrectionPanel.LastNudgeLabel.text"), null, //$NON-NLS-1$
                    lastNudgeLabel);

            String nudgeSumTip = Translations.getString("PickOffsetCorrectionPanel.TotalNudgeNetLabel.toolTipText"); //$NON-NLS-1$
            nudgeSumLabel.setToolTipText(nudgeSumTip);
            labelled(layout, Translations.getString("PickOffsetCorrectionPanel.TotalNudgeNetLabel.text"), //$NON-NLS-1$
                    nudgeSumTip, nudgeSumLabel);

            labelled(layout, Translations.getString("PickOffsetCorrectionPanel.TotalNudgeAbsoluteLabel.text"), //$NON-NLS-1$
                    null, nudgeAbsSumLabel);
        }

        separator(layout);
        span(layout, buildButtonRow());

        refresh();
    }

    private int newRow(FormLayout layout) {
        layout.appendRow(FormSpecs.RELATED_GAP_ROWSPEC);
        layout.appendRow(FormSpecs.DEFAULT_ROWSPEC);
        return layout.getRowCount();
    }

    private void span(FormLayout layout, JComponent component) {
        int r = newRow(layout);
        add(component, "2, " + r + ", 3, 1, left, default");
    }

    private void separator(FormLayout layout) {
        int r = newRow(layout);
        add(new JSeparator(), "2, " + r + ", 3, 1");
    }

    private void labelled(FormLayout layout, String text, String tip, JComponent value) {
        int r = newRow(layout);
        JLabel label = new JLabel(text);
        label.setToolTipText(tip);
        add(label, "2, " + r);
        add(value, "4, " + r + ", left, default");
    }

    private JPanel buildButtonRow() {
        JPanel row = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 0, 0, 8);
        row.add(new JButton(resetAction), c);
        row.add(new JButton(bakeAction), c);
        return row;
    }

    private final Action resetAction = new AbstractAction(
            Translations.getString("PickOffsetCorrectionPanel.Action.Reset")) { //$NON-NLS-1$
        @Override
        public void actionPerformed(ActionEvent e) {
            correction.reset();
        }
    };

    private final Action bakeAction = new AbstractAction(
            Translations.getString("PickOffsetCorrectionPanel.Action.Bake")) { //$NON-NLS-1$
        {
            putValue(Action.SHORT_DESCRIPTION, Translations.getString("PickOffsetCorrectionPanel.Action.Bake.Description")); //$NON-NLS-1$
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                owner.bakePickOffsetCorrectionIntoPosition();
            }
            catch (Exception ex) {
                MessageBoxes.errorBox(PickOffsetCorrectionPanel.this,
                        Translations.getString("PickOffsetCorrectionPanel.Action.Bake"), ex); //$NON-NLS-1$
            }
        }
    };

    private JPanel buildFeedPlanePanel() {
        JPanel cross = new JPanel(new GridBagLayout());
        cross.setToolTipText(Translations.getString("PickOffsetCorrectionPanel.FeedPlaneBorder.toolTipText"));
        cross.setBorder(new TitledBorder(null,
                Translations.getString("PickOffsetCorrectionPanel.FeedPlaneBorder.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));

        yPlusButton = directionButton("Y+", 0, 1);
        yMinusButton = directionButton("Y-", 0, -1);
        xPlusButton = directionButton("X+", 1, 0);
        xMinusButton = directionButton("X-", -1, 0);

        // Mutually exclusive selection, arranged as a compass cross.
        ButtonGroup group = new ButtonGroup();
        group.add(yPlusButton);
        group.add(yMinusButton);
        group.add(xPlusButton);
        group.add(xMinusButton);

        cross.add(yPlusButton, at(1, 0));
        cross.add(xMinusButton, at(0, 1));
        cross.add(xPlusButton, at(2, 1));
        cross.add(yMinusButton, at(1, 2));
        return cross;
    }

    private JToggleButton directionButton(String text, double x, double y) {
        JToggleButton button = new JToggleButton(text);
        button.setToolTipText(Translations.getString("PickOffsetCorrectionPanel.FeedPlaneBorder.toolTipText")); //$NON-NLS-1$
        button.addActionListener(e -> ((PickOffsetCorrection.Nudging) correction)
                .setFeedPlane(new Location(LengthUnit.Millimeters, x, y, 0, 0)));
        return button;
    }

    private static GridBagConstraints at(int gridx, int gridy) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = gridx;
        c.gridy = gridy;
        c.insets = new Insets(2, 2, 2, 2);
        return c;
    }

    private void refresh() {
        refreshing = true;
        try {
            enabledCheckBox.setSelected(correction.isEnabled());

            Location offset = correction.getOffset().convertToUnits(userFavoriteUnit);
            offsetLabel.setText(fmt(offset.getX()) + ", " + fmt(offset.getY()) + " " + userFavoriteUnit.getShortName());
            magnitudeLabel.setText(fmt(offset.getLinearDistanceTo(0, 0)) + " " + userFavoriteUnit.getShortName());

            iTermSpinner.setValue(correction.getITerm());
            limitSpinner.setValue(toUnit(correction.getCorrectionLimit()));

            Location lastVision = correction.getLastVision();
            if (lastVision == null) {
                lastVisionLabel.setText("—");
            }
            else {
                Location v = lastVision.convertToUnits(userFavoriteUnit);
                lastVisionLabel.setText(fmt(v.getX()) + ", " + fmt(v.getY()) + " " + userFavoriteUnit.getShortName());
            }

            samplesLabel.setText(Integer.toString(correction.getVisionsSinceLastFeed()));

            if (nudging) {
                PickOffsetCorrection.Nudging n = (PickOffsetCorrection.Nudging) correction;
                nudgingCheckBox.setSelected(n.isNudgingEnabled());
                Location plane = n.getFeedPlane().convertToUnits(LengthUnit.Millimeters);
                yPlusButton.setSelected(plane.getX() == 0 && plane.getY() > 0);
                yMinusButton.setSelected(plane.getX() == 0 && plane.getY() < 0);
                xPlusButton.setSelected(plane.getY() == 0 && plane.getX() > 0);
                xMinusButton.setSelected(plane.getY() == 0 && plane.getX() < 0);

                lastNudgeLabel.setText(fmtLength(n.getLastNudge()));
                nudgeSumLabel.setText(fmtLength(n.getNudgeSum()));
                nudgeAbsSumLabel.setText(fmtLength(n.getNudgeAbsSum()));
            }

            updateEnabledState();
        }
        finally {
            refreshing = false;
        }
    }

    /** Grays out everything the master switch controls when the correction is disabled. */
    private void updateEnabledState() {
        boolean on = correction.isEnabled();
        iTermSpinner.setEnabled(on);
        limitSpinner.setEnabled(on);
        bakeAction.setEnabled(on);
        if (nudging) {
            nudgingCheckBox.setEnabled(on);
            boolean planeOn = on && nudgingCheckBox.isSelected();
            xPlusButton.setEnabled(planeOn);
            xMinusButton.setEnabled(planeOn);
            yPlusButton.setEnabled(planeOn);
            yMinusButton.setEnabled(planeOn);
        }
    }

    private static String fmt(double value) {
        return String.format("%.3f", value);
    }

    /** Formats a length in the user's configured unit, e.g. "0.500 mm". */
    private String fmtLength(Length length) {
        return fmt(length.convertToUnits(userFavoriteUnit).getValue()) + " " + userFavoriteUnit.getShortName();
    }

    /** Converts a length into the user's configured unit. */
    private double toUnit(Length l) {
        return l.convertToUnits(userFavoriteUnit).getValue();
    }

    /** Converts the user's configured unit to a length. */
    private Length fromUnit(double value) {
        return new Length(value, userFavoriteUnit);
    }

    private Length mmLength(double mm) {
        return new Length(mm, LengthUnit.Millimeters);
    }

    // Register/unregister with the model only while the panel is on screen, so selecting away from
    // the feeder doesn't leak a listener onto the long-lived correction object.
    @Override
    public void addNotify() {
        super.addNotify();
        correction.addPropertyChangeListener(listener);
        refresh();
    }

    @Override
    public void removeNotify() {
        correction.removePropertyChangeListener(listener);
        super.removeNotify();
    }
}
