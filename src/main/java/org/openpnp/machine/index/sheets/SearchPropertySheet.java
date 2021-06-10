package org.openpnp.machine.index.sheets;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.machine.index.IndexFeeder;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.UiUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.function.IntConsumer;

public class SearchPropertySheet implements PropertySheetHolder.PropertySheet {
    @Override
    public String getPropertySheetTitle() {
        return "Search";
    }

    @Override
    public JPanel getPropertySheetPanel() {
        JPanel panel = new JPanel();
        JButton searchButton = new JButton("Search");
        JProgressBar progressBar = new JProgressBar(0, 100);
        searchButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchButton.setEnabled(false);
                UiUtils.submitUiMachineTask(() -> {
                    IndexFeeder.findAllFeeders(new IntConsumer() {
                        @Override
                        public void accept(int value) {
                            progressBar.setIndeterminate(false);
                            progressBar.setValue(value);
                        }
                    });
                    return null;
                }, (parameter) -> {
                    searchButton.setEnabled(true);
                    progressBar.setValue(0);
                }, (throwable) -> {
                    searchButton.setEnabled(true);
                    progressBar.setValue(0);

                    MessageBoxes.errorBox(MainFrame.get(), "Error", throwable);
                });
            }
        });
        panel.add(searchButton);
        panel.add(progressBar);
        progressBar.setIndeterminate(true);
        return panel;
    }
}
