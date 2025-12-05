package org.openpnp.gui;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.JTable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.gui.PartsPanel;

import com.google.common.io.Files;

import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.model.Package;

public class PartsPanelTest {

    @AfterEach
    public void tearDown() {
        // Cleanup if necessary
    }

    @Test
    public void testDeleteLastPartSelectsNewLastPartWithoutException() throws Exception {
        // Setup Configuration
        File workingDirectory = Files.createTempDir();
        Configuration.initialize(workingDirectory);
        
        ReferenceMachine machine = new ReferenceMachine();
        Configuration.get().setMachine(machine);

        // Create a package as it might be needed by PartsPanel
        Package pkg = new Package("Pkg1");
        Configuration.get().addPackage(pkg);
        
        // Add some parts
        Part p1 = new Part("Part1");
        p1.setPackage(pkg);
        Configuration.get().addPart(p1);
        
        Part p2 = new Part("Part2");
        p2.setPackage(pkg);
        Configuration.get().addPart(p2);
        
        Part p3 = new Part("Part3");
        p3.setPackage(pkg);
        Configuration.get().addPart(p3);

        // Create the GUI component on EDT to be safe with Swing components
        AtomicReference<Exception> exceptionRef = new AtomicReference<>();
        
        SwingUtilities.invokeAndWait(() -> {
            try {
                PartsPanel partsPanel = new PartsPanel(Configuration.get(), null);
                
                // We need to access the table to set selection. 
                // Since 'table' is private in PartsPanel, we can access it via component hierarchy or reflection.
                // Or simpler: The table is inside a JScrollPane inside a JSplitPane.
                // Structure: PartsPanel -> JSplitPane -> JScrollPane -> JTable
                
                JTable table = (JTable) ((javax.swing.JScrollPane) ((javax.swing.JSplitPane) partsPanel.getComponent(1)).getLeftComponent()).getViewport().getView();
                
                // Select the last part (index 2)
                int lastRowIndex = 2;
                table.setRowSelectionInterval(lastRowIndex, lastRowIndex);
                
                // Verify selection
                Assertions.assertEquals(lastRowIndex, table.getSelectedRow());
                
                // Remove the last part. This triggers the property change listener in PartsTableModel,
                // which updates the table, which triggers the selection listener in PartsPanel.
                // Before the fix, this would throw IndexOutOfBoundsException.
                Configuration.get().removePart(Configuration.get().getParts().get(lastRowIndex));
                
            } catch (Exception e) {
                exceptionRef.set(e);
            }
        });

        if (exceptionRef.get() != null) {
            throw exceptionRef.get();
        }
        
        // Verify that we have 2 parts left
        Assertions.assertEquals(2, Configuration.get().getParts().size());
    }
}
