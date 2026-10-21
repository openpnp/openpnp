package org.openpnp.machine.photon.sheets.gui;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.openpnp.Translations;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.AbstractConfigurationWizard;
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

public class GlobalConfigConfigurationWizard extends AbstractConfigurationWizard {

    private final PhotonProperties photonProperties;

    private final FeederSearchProgressBar progressBarPanel;
    private final JButton searchButton;
    private final JSpinner maxFeederSpinner;
    private final JButton btnStartFeedSlotsWizard;
    private final JLabel lblNewLabel;

    /**
     * Create the panel.
     */
    public GlobalConfigConfigurationWizard() {
        photonProperties = new PhotonProperties(Configuration.get().getMachine());

        JPanel searchPanel = new JPanel();
        searchPanel.setBorder(new TitledBorder(null,
                Translations.getString("PhotonFeeder.GlobalConfig.SearchPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
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

        JLabel lblMaxFeeder = new JLabel(Translations.getString(
                "PhotonFeeder.GlobalConfig.MaxFeederAddressLabel.text")); //$NON-NLS-1$
        lblMaxFeeder.setToolTipText(Translations.getString(
                "PhotonFeeder.GlobalConfig.MaxFeederAddressLabel.toolTipText")); //$NON-NLS-1$
        searchPanel.add(lblMaxFeeder, "2, 2");

        int initialMaxFeederAddress = photonProperties.getMaxFeederAddress();
        SpinnerNumberModel maxFeederSpinnerModel = new SpinnerNumberModel(
                initialMaxFeederAddress, 1, 254, 1
        );
        maxFeederSpinner = new JSpinner(maxFeederSpinnerModel);
        maxFeederSpinner.setToolTipText(Translations.getString(
                "PhotonFeeder.GlobalConfig.MaxFeederAddressLabel.toolTipText")); //$NON-NLS-1$
        searchPanel.add(maxFeederSpinner, "4, 2");

        searchButton = new JButton(Translations.getString(
                "PhotonFeeder.GlobalConfig.SearchButton.text")); //$NON-NLS-1$
        searchButton.setToolTipText(Translations.getString(
                "PhotonFeeder.GlobalConfig.SearchButton.toolTipText")); //$NON-NLS-1$
        searchButton.addActionListener(searchAction);
        searchPanel.add(searchButton, "6, 2");

        progressBarPanel = new FeederSearchProgressBar();
        searchPanel.add(progressBarPanel, "2, 4, 5, 1, fill, fill");
        progressBarPanel.setVisible(false);
        progressBarPanel.setNumberOfElements(initialMaxFeederAddress);

        JPanel programFeederSlotsPanel = new JPanel();
        programFeederSlotsPanel.setBorder(new TitledBorder(null,
                Translations.getString("PhotonFeeder.GlobalConfig.ProgramSlotsPanel.Border.title"), //$NON-NLS-1$
                TitledBorder.LEADING, TitledBorder.TOP, null, null));
        contentPanel.add(programFeederSlotsPanel);
        programFeederSlotsPanel.setLayout(new FormLayout(new ColumnSpec[]{
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

        lblNewLabel = new JLabel(Translations.getString(
                "PhotonFeeder.GlobalConfig.ProgramSlotsHelpLabel.text")); //$NON-NLS-1$
        programFeederSlotsPanel.add(lblNewLabel, "2, 2, 3, 1");

        btnStartFeedSlotsWizard = new JButton(Translations.getString(
                "PhotonFeeder.GlobalConfig.StartWizardButton.text")); //$NON-NLS-1$
        btnStartFeedSlotsWizard.setToolTipText(Translations.getString(
                "PhotonFeeder.GlobalConfig.StartWizardButton.toolTipText")); //$NON-NLS-1$
        btnStartFeedSlotsWizard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if(! Configuration.get().getMachine().isEnabled()) {
                    UiUtils.showError(new Exception(Translations.getString(
                            "PhotonFeeder.GlobalConfig.Exception.ConnectMachineFirst"))); //$NON-NLS-1$
                    return;
                }

                ProgramFeederSlotWizard wizard = new ProgramFeederSlotWizard();
                wizard.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                wizard.setVisible(true);
            }
        });
        programFeederSlotsPanel.add(btnStartFeedSlotsWizard, "4, 4");
    }

    @Override
    public void createBindings() {
        bind(UpdateStrategy.READ_WRITE, photonProperties, "maxFeederAddress", maxFeederSpinner, "value");
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

                MessageBoxes.errorBox(MainFrame.get(),
                        Translations.getString("CommonWords.Error"), throwable); //$NON-NLS-1$
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
