package org.openpnp.machine.photon.sheets.gui;

import javax.swing.*;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;

import javax.swing.border.TitledBorder;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.FormSpecs;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.photon.PhotonFeeder;
import org.openpnp.machine.photon.PhotonProperties;
import org.openpnp.model.Configuration;
import org.openpnp.util.UiUtils;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.function.IntConsumer;

public class GlobalConfigConfigurationWizard extends AbstractConfigurationWizard {

    private final FeederSearchProgressBar progressBarPanel;
    private final JButton searchButton;

    /**
     * Create the panel.
     */
    public GlobalConfigConfigurationWizard() {

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

        JSpinner maxFeederSpinner = new JSpinner();
        searchPanel.add(maxFeederSpinner, "4, 2");

        searchButton = new JButton("Search");
        searchButton.addActionListener(searchAction);
        searchPanel.add(searchButton, "6, 2");

        progressBarPanel = new FeederSearchProgressBar();
        searchPanel.add(progressBarPanel, "2, 4, 5, 1, fill, fill");

        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder(null, "Program Feeder Floors", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(panel);
        panel.setLayout(new FormLayout(new ColumnSpec[]{},
                new RowSpec[]{}));

    }

    @Override
    public void createBindings() {
        // TODO Auto-generated method stub

    }

    private final Action searchAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            searchButton.setEnabled(false);
            PhotonProperties photonProperties = new PhotonProperties(Configuration.get().getMachine());
            int maxFeederAddress = photonProperties.getMaxFeederAddress();
            progressBarPanel.setNumberOfElements(maxFeederAddress);

            UiUtils.submitUiMachineTask(() -> {
                PhotonFeeder.findAllFeeders(new PhotonFeeder.FeederSearchProgressConsumer() {
                    @Override
                    public void accept(int feederAddress, PhotonFeeder.FeederSearchState feederSearchState) {
                        progressBarPanel.updateFeederState(feederAddress, feederSearchState);
                    }
                });
                return null;
            }, (parameter) -> {
                searchButton.setEnabled(true);
                progressBarPanel.clearAllState();
            }, (throwable) -> {
                searchButton.setEnabled(true);
                progressBarPanel.clearAllState();

                MessageBoxes.errorBox(MainFrame.get(), "Error", throwable);
            });
        }
    };
}
