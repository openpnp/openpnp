import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.openpnp.gui.importer.KicadModImporter;
import org.openpnp.model.Footprint.Pad;

public class KiCadModImporterTest {
    private static final double delta = 0.0001;
    
    @Test
    public void testImportCapacitorV7() throws Exception {
        InputStream stream = ClassLoader.getSystemResourceAsStream("samples/kicad/C_0603_1608Metric_v7.kicad_mod");
        KicadModImporter importer = new KicadModImporter(stream);
        
        List<Pad> pads = importer.getPads();
        assertEquals(2, pads.size());
        
        Pad pad1 = pads.get(0);
        assertEquals("1", pad1.getName());
        assertEquals(-0.775, pad1.getX(), delta);
        assertEquals(0.0, pad1.getY(), delta);
        assertEquals(0.9, pad1.getWidth(), delta);
        assertEquals(0.95, pad1.getHeight(), delta);
        assertEquals(25.0, pad1.getRoundness(), delta);
        assertEquals(0.0, pad1.getRotation(), delta);

        Pad pad2 = pads.get(1);
        assertEquals("2", pad2.getName());
        assertEquals(0.775, pad2.getX(), delta);
        assertEquals(0.0, pad2.getY(), delta);
        assertEquals(0.9, pad2.getWidth(), delta);
        assertEquals(0.95, pad2.getHeight(), delta);
        assertEquals(25.0, pad2.getRoundness(), delta);
        assertEquals(0.0, pad2.getRotation(), delta);
    }

    @Test
    public void testImportCapacitorV10() throws Exception {
        InputStream stream = ClassLoader.getSystemResourceAsStream("samples/kicad/C_0603_1608Metric_v10.kicad_mod");
        KicadModImporter importer = new KicadModImporter(stream);
        
        List<Pad> pads = importer.getPads();
        assertEquals(2, pads.size());
        
        Pad pad1 = pads.get(0);
        assertEquals("1", pad1.getName());
        assertEquals(-0.775, pad1.getX(), delta);
        assertEquals(0.0, pad1.getY(), delta);
        assertEquals(0.9, pad1.getWidth(), delta);
        assertEquals(0.95, pad1.getHeight(), delta);
        assertEquals(25.0, pad1.getRoundness(), delta);
        assertEquals(0.0, pad1.getRotation(), delta);

        Pad pad2 = pads.get(1);
        assertEquals("2", pad2.getName());
        assertEquals(0.775, pad2.getX(), delta);
        assertEquals(0.0, pad2.getY(), delta);
        assertEquals(0.9, pad2.getWidth(), delta);
        assertEquals(0.95, pad2.getHeight(), delta);
        assertEquals(25.0, pad2.getRoundness(), delta);
        assertEquals(0.0, pad2.getRotation(), delta);
    }
    
    @Test
    public void testImportMsopWithCustomPadShape() throws Exception {
        // This test loads an MSOP-10 footprint with a center thermal pad that uses a
        // custom pad shape. Since custom pad shapes are not yet supported, the expected
        // behavior is to only import the recognized pad types and ignore the rest.
        // This can be used as a starting point for future handling of custom
        // pad shapes.
        InputStream stream = ClassLoader.getSystemResourceAsStream("samples/kicad/MSOP-10-1EP_3x3mm_P0.5mm_EP1.73x1.7mm_ThermalVias.kicad_mod");
        KicadModImporter importer = new KicadModImporter(stream);
        
        List<Pad> pads = importer.getPads();
        assertEquals(10, pads.size());
        
        for (int i = 0; i < 10; i++) {
            Pad pad = pads.get(i);
            assertEquals(Integer.toString(i + 1), pad.getName());
            assertEquals(1.45, pad.getWidth(), delta);
            assertEquals(0.3, pad.getHeight(), delta);
            assertEquals(25.0, pad.getRoundness(), delta);

            if (i < 5) {
                assertEquals(-2.2, pad.getX(), delta);
                assertEquals(1.0 - (0.5 * i), pad.getY(), delta);
            } else {
                assertEquals(2.2, pad.getX(), delta);
                assertEquals(-1.0 + (0.5 * (i - 5)), pad.getY(), delta);
            }
        }
    }
    
    @Test
    public void testImportLedWithOnlyCustomPadShapes() throws Exception {
        // This test loads an LED footprint that only has custom pad shapes.
        // Since custom pad shapes are not yet supported, the expected behavior
        // is to not import any pads.
        // This can be used as a starting point for future handling of custom
        // pad shapes.
        InputStream stream = ClassLoader.getSystemResourceAsStream("samples/kicad/SunLike_LED_S1S0.kicad_mod");
        KicadModImporter importer = new KicadModImporter(stream);
        
        List<Pad> pads = importer.getPads();
        assertEquals(0, pads.size());
    }
}
