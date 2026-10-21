package org.openpnp.machine.photon.sheets.gui;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;
import org.openpnp.Translations;
import org.openpnp.machine.photon.PhotonFeeder;
import org.openpnp.machine.photon.PhotonProperties;
import org.openpnp.machine.photon.protocol.PhotonBusInterface;
import org.openpnp.machine.photon.protocol.commands.ProgramFeederFloorAddress;
import org.openpnp.machine.photon.protocol.commands.UninitializedFeedersRespond;
import org.openpnp.model.Configuration;
import org.openpnp.util.UiUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FeederSlotUpdateStep extends JPanel {

    private final JSpinner feederAddressSpinner;
    private final JTextArea statusLabel;

    private final SpinnerNumberModel feederAddressModal;

    private final Thread updateThread;
    private final PhotonBusInterface photonBus;
    private final PhotonProperties photonProperties;
    private boolean shouldStopThread = false;

    /**
     * Create the panel.
     */
    public FeederSlotUpdateStep() {
        setLayout(new FormLayout(new ColumnSpec[]{
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("50dlu"),
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("2dlu:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,},
                new RowSpec[]{
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.UNRELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.UNRELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        RowSpec.decode("2dlu:grow"),
                        FormSpecs.RELATED_GAP_ROWSPEC,}));

        JTextArea instructionsText = new JTextArea();
        instructionsText.setBackground(UIManager.getColor("Panel.background"));
        instructionsText.setText(Translations.getString(
                "PhotonFeeder.FeederSlotUpdateStep.instructionsText")); //$NON-NLS-1$
        instructionsText.setWrapStyleWord(true);
        instructionsText.setLineWrap(true);
        instructionsText.setEditable(false);
        add(instructionsText, "2, 2, 5, 1, fill, fill");

        JLabel lblNewLabel = new JLabel(Translations.getString(
                "PhotonFeeder.FeederSlotUpdateStep.CurrentSlotAddressLabel.text")); //$NON-NLS-1$
        add(lblNewLabel, "2, 4");

        feederAddressModal = new SpinnerNumberModel(254, 1, 254, 1);
        feederAddressSpinner = new JSpinner(feederAddressModal);
        add(feederAddressSpinner, "4, 4");
        feederAddressModal.addChangeListener(feederAddressChangedAction);

        statusLabel = new JTextArea();
        statusLabel.setEditable(false);
        statusLabel.setBackground(UIManager.getColor("Panel.background"));
        add(statusLabel, "2, 6, 5, 1, fill, fill");

        // Force a value update to get the change listener to respond and update the status label
        feederAddressSpinner.setValue(1);

        updateThread = new Thread(updateRunnable);

        photonBus = PhotonFeeder.getBus();
        photonProperties = new PhotonProperties(Configuration.get().getMachine());
    }

    public void startThread() {
        shouldStopThread = false;
        updateThread.start();
    }

    public void stopThread() {
        shouldStopThread = true;
    }

    private final ChangeListener feederAddressChangedAction = new ChangeListener() {
        @Override
        public void stateChanged(ChangeEvent e) {
            int feederAddress = (int) feederAddressSpinner.getValue();
            statusLabel.setText(Translations.format(
                    "PhotonFeeder.FeederSlotUpdateStep.PleaseInsertFeeder", feederAddress)); //$NON-NLS-1$
        }
    };

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            while (!shouldStopThread) {
                try {
                    String uuid = findUninitializedFeeder();

                    if (uuid == null) {
                        continue;
                    } else if (shouldStopThread) {
                        break;  // We don't want to accidentally program a feeder floor after they clicked finish
                    }

                    feederAddressSpinner.setEnabled(false);
                    int feederAddress = (int) feederAddressSpinner.getValue();

                    statusLabel.setText(Translations.format(
                            "PhotonFeeder.FeederSlotUpdateStep.FoundFeeder", feederAddress)); //$NON-NLS-1$

                    updateFeederAddress(uuid, feederAddress);

                    PhotonFeeder feeder = new PhotonFeeder();
                    feeder.setHardwareId(uuid);
                    feeder.setSlotAddress(feederAddress);
                    UiUtils.submitUiMachineTask(feeder::initializeIfNeeded).get();

                    if (!feeder.isInitialized()) {
                        throw new Exception(Translations.getString(
                                "PhotonFeeder.FeederSlotUpdateStep.Exception.InitAfterUpdateFailed")); //$NON-NLS-1$
                    }

                    if (feederAddress == 254) {
                        shouldStopThread = true;
                        statusLabel.setText(Translations.getString(
                                "PhotonFeeder.FeederSlotUpdateStep.MaxAddressReached")); //$NON-NLS-1$
                        break;
                    }

                    // Set the max feeder address at least to what we've just programmed, as a convenience for searching.
                    photonProperties.setMaxFeederAddress(Math.max(photonProperties.getMaxFeederAddress(), feederAddress));
                    feederAddressSpinner.setValue(feederAddress + 1);
                    feederAddressSpinner.setEnabled(true);
                } catch (Exception e) {
                    // UiUtils.showError(e);
                    shouldStopThread = true;
                    return;
                }
            }
        }

        private void updateFeederAddress(String uuid, int feederAddress) throws ExecutionException, InterruptedException {
            Future<Void> voidFuture = UiUtils.submitUiMachineTask(() -> {
                ProgramFeederFloorAddress command = new ProgramFeederFloorAddress(uuid, feederAddress);
                ProgramFeederFloorAddress.Response response = command.send(photonBus);

                if (response == null) {
                    shouldStopThread = true;
                    statusLabel.setText(Translations.getString(
                            "PhotonFeeder.FeederSlotUpdateStep.ProgrammingFailed")); //$NON-NLS-1$

                    throw new Exception(Translations.getString(
                            "PhotonFeeder.FeederSlotUpdateStep.Exception.UpdateSlotAddressFailed")); //$NON-NLS-1$
                }
            });
            voidFuture.get();

            statusLabel.setText(Translations.getString(
                    "PhotonFeeder.FeederSlotUpdateStep.ProgrammingDone")); //$NON-NLS-1$
        }

        private String findUninitializedFeeder() throws ExecutionException, InterruptedException {
            Future<String> stringFuture = UiUtils.submitUiMachineTask(() -> {

                UninitializedFeedersRespond command = new UninitializedFeedersRespond();
                UninitializedFeedersRespond.Response response = command.send(photonBus);

                if (response == null) {
                    return null;
                }

                return response.uuid;
            });

            return stringFuture.get();
        }
    };
}
