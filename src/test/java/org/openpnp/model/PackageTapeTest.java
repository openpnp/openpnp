package org.openpnp.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.simpleframework.xml.Serializer;

import com.google.common.io.Files;

public class PackageTapeTest {
    @BeforeEach
    public void setUp() throws Exception {
        File workingDirectory = new File(Files.createTempDir(), ".openpnp");
        Configuration.initialize(workingDirectory);
        Configuration.get().load();
    }

    private String write(Package pkg) throws Exception {
        Serializer serializer = Configuration.createSerializer();
        StringWriter writer = new StringWriter();
        serializer.write(pkg, writer);
        return writer.toString();
    }

    @Test
    public void packageWithoutTapeValuesWritesNoTapeValues() throws Exception {
        String xml = write(new Package("plain"));

        assertFalse(xml.contains("tape-rotation"), xml);
        assertFalse(xml.contains("tape-pitch"), xml);
        assertFalse(xml.contains("tape-width"), xml);

        Package read = Configuration.createSerializer().read(Package.class, xml);
        assertNull(read.getTapeRotation());
        assertNull(read.getTapePitch());
        assertNull(read.getTapeWidth());
    }

    @Test
    public void tapeValuesRoundTrip() throws Exception {
        Package pkg = new Package("SOT-23-6");
        pkg.setTapeRotation(-90.0);
        pkg.setTapePitch(new Length(4, LengthUnit.Millimeters));
        pkg.setTapeWidth(new Length(8, LengthUnit.Millimeters));

        String xml = write(pkg);
        assertTrue(xml.contains("tape-rotation=\"-90.0\""), xml);
        assertTrue(xml.contains("<tape-pitch value=\"4.0\" units=\"Millimeters\"/>"), xml);
        assertTrue(xml.contains("<tape-width value=\"8.0\" units=\"Millimeters\"/>"), xml);

        Package read = Configuration.createSerializer().read(Package.class, xml);
        assertEquals(-90.0, read.getTapeRotation(), 1e-9);
        assertEquals(new Length(4, LengthUnit.Millimeters), read.getTapePitch());
        assertEquals(new Length(8, LengthUnit.Millimeters), read.getTapeWidth());
    }

    @Test
    public void readsPackagesWithTapeValues() throws Exception {
        String xml = "<package version=\"1.1\" id=\"SOT-23-6\" tape-rotation=\"90\">"
                + "<footprint units=\"Millimeters\" body-width=\"1.6\" body-height=\"2.9\"/>"
                + "<tape-pitch value=\"4\" units=\"Millimeters\"/>"
                + "<tape-width value=\"8\" units=\"Millimeters\"/>"
                + "</package>";

        Package read = Configuration.createSerializer().read(Package.class, xml);
        assertEquals(90.0, read.getTapeRotation(), 1e-9);
        assertEquals(new Length(4, LengthUnit.Millimeters), read.getTapePitch());
        assertEquals(new Length(8, LengthUnit.Millimeters), read.getTapeWidth());
    }
}
