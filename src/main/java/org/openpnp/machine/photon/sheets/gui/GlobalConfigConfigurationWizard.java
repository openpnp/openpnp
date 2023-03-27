package org.openpnp.machine.photon.sheets.gui;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.JBindings;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.photon.PhotonFeeder;
import org.openpnp.machine.photon.PhotonProperties;
import org.openpnp.model.Configuration;
import org.openpnp.util.UiUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GlobalConfigConfigurationWizard extends JPanel {

    private final PhotonProperties photonProperties;

    protected JPanel contentPanel;
    private final JScrollPane scrollPane;

    private final FeederSearchProgressBar progressBarPanel;
    private final JButton searchButton;
    private final JSpinner maxFeederSpinner;
    private final JButton btnStartFeedFloorWizard;
    private final JLabel lblNewLabel;

    /**
     * Create the panel.
     */
    public GlobalConfigConfigurationWizard() {
        photonProperties = new PhotonProperties(Configuration.get().getMachine());

        setLayout(new BorderLayout());

        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(contentPanel);

        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        JPanel searchPanel = new JPanel();
        searchPanel.setBorder(new TitledBorder(null, "Search", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(searchPanel);
        searchPanel.setLayout(new FormLayout(new ColumnSpec[]{
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("50dlu"),
                ColumnSpec.decode("4dlu:grow"),
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,},
                new RowSpec[]{
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("10dlu"),
                        FormSpecs.RELATED_GAP_ROWSPEC,}));

        JLabel lblMaxFeeder = new JLabel("Maximum Feeder Address To Scan");
        searchPanel.add(lblMaxFeeder, "2, 2");

        int initialMaxFeederAddress = photonProperties.getMaxFeederAddress();
        SpinnerNumberModel maxFeederSpinnerModel = new SpinnerNumberModel(
                initialMaxFeederAddress, 1, 254, 1
        );
        maxFeederSpinner = new JSpinner(maxFeederSpinnerModel);
        searchPanel.add(maxFeederSpinner, "4, 2");

        searchButton = new JButton("Search");
        searchButton.addActionListener(searchAction);
        searchPanel.add(searchButton, "6, 2");

        progressBarPanel = new FeederSearchProgressBar();
        searchPanel.add(progressBarPanel, "2, 4, 5, 1, fill, fill");
        progressBarPanel.setVisible(false);
        progressBarPanel.setNumberOfElements(initialMaxFeederAddress);

        JPanel programFeederFloorsPanel = new JPanel();
        programFeederFloorsPanel.setBorder(new TitledBorder(null, "Program Feeder Floors", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(programFeederFloorsPanel);
        programFeederFloorsPanel.setLayout(new FormLayout(new ColumnSpec[]{
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("4dlu:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,},
                new RowSpec[]{
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        RowSpec.decode("6dlu:grow"),
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,}));

        lblNewLabel = new JLabel("If you've built your own slots and need to program them, use this wizard.");
        programFeederFloorsPanel.add(lblNewLabel, "2, 2, 3, 1");

        btnStartFeedFloorWizard = new JButton("Start Wizard");
        btnStartFeedFloorWizard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if(! Configuration.get().getMachine().isEnabled()) {
                    UiUtils.showError(new Exception("Please connect to the machine before running this wizard."));
                    return;
                }

                ProgramFeederSlotWizard wizard = new ProgramFeederSlotWizard();
                wizard.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                wizard.setVisible(true);
            }
        });
        programFeederFloorsPanel.add(btnStartFeedFloorWizard, "4, 4");

        createBindings();
    }

    public void createBindings() {
        JBindings.bind(photonProperties, "maxFeederAddress", maxFeederSpinner, "value");

        maxFeederSpinner.addChangeListener(e -> {
            int maxFeederAddress = (int) maxFeederSpinner.getValue();
            photonProperties.setMaxFeederAddress(maxFeederAddress);
        });
    }

    private final Action searchAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            progressBarPanel.setVisible(true);
            searchButton.setEnabled(false);
            maxFeederSpinner.setEnabled(false);

            int maxFeederAddress = photonProperties.getMaxFeederAddress();
            progressBarPanel.setNumberOfElements(maxFeederAddress);

            UiUtils.submitUiMachineTask(() -> {
                PhotonFeeder.findAllFeeders(progressBarPanel::updateFeederState);
                return null;
            }, (parameter) -> {
                resetState();
            }, (throwable) -> {
                resetState();

                MessageBoxes.errorBox(MainFrame.get(), "Error", throwable);
            });
        }

        private void resetState() {
            progressBarPanel.setVisible(false);
            progressBarPanel.clearAllState();
            searchButton.setEnabled(true);
            maxFeederSpinner.setEnabled(true);
        }
    };
}
