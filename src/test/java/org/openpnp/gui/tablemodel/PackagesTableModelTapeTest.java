package org.openpnp.gui.tablemodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;

import com.google.common.io.Files;

public class PackagesTableModelTapeTest {
    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = new File(Files.createTempDir(), ".openpnp");
        Configuration.initialize(workingDirectory);
        Configuration.get().load();
        Configuration.get().setSystemUnits(LengthUnit.Millimeters);
    }

    @Test
    public void blankOrNotPositiveIsNotKnown() {
        Length old = new Length(4, LengthUnit.Millimeters);
        assertNull(PackagesTableModel.parseTapeLength(null, old));
        assertNull(PackagesTableModel.parseTapeLength("  ", old));
        assertNull(PackagesTableModel.parseTapeLength("0", old));
        assertNull(PackagesTableModel.parseTapeLength("-4", old));
    }

    @Test
    public void withoutUnitsTakesTheOldUnitsElseTheSystemUnits() {
        assertEquals(new Length(8, LengthUnit.Millimeters),
                PackagesTableModel.parseTapeLength("8", null));
        assertEquals(new Length(0.25, LengthUnit.Inches),
                PackagesTableModel.parseTapeLength("0.25", new Length(0.5, LengthUnit.Inches)));
    }

    @Test
    public void keepsTypedUnits() {
        assertEquals(new Length(0.157, LengthUnit.Inches),
                PackagesTableModel.parseTapeLength("0.157\"", null));
    }

    @Test
    public void rejectsText() {
        assertThrows(IllegalArgumentException.class,
                () -> PackagesTableModel.parseTapeLength("eight", null));
    }
}
