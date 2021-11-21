import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.machine.reference.vision.ReferenceBottomVision;
import org.openpnp.model.BottomVisionSettings;
import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.spi.Machine;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class ReferenceBottomVisionInheritanceTest {
    static File workingDirectory;
    private ReferenceBottomVision bottomVision;

    @BeforeAll
    public static void setup() throws Exception {
        /**
         * Create a new config directory and load the default configuration.
         */
        workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        // Copy the required configuration files over to the new configuration
        // directory.
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/ReferenceBottomVisionOffset/packages.xml"),
                new File(workingDirectory, "packages.xml"));
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/ReferenceBottomVisionOffset/parts.xml"),
                new File(workingDirectory, "parts.xml"));

        Configuration.initialize(workingDirectory);
        Configuration.get().load();
        // Save back migrated.
        Configuration.get().save();
    }
    
    @BeforeEach
    public void before() throws Exception {
        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        Machine machine = Configuration.get().getMachine();
        bottomVision = (ReferenceBottomVision) machine.getPartAlignments().get(0);
    }
    
    private void assertBottomVisionIsDefault(BottomVisionSettings bottomVisionSettings) {
        BottomVisionSettings defaultBottomVisionSettings = bottomVision.getBottomVisionSettings();
        
        assertEquals(bottomVisionSettings.isEnabled(), defaultBottomVisionSettings.isEnabled(), "Part BottomVisionSettings should be the built-in default");
        assertEquals(bottomVisionSettings.getPreRotateUsage(), defaultBottomVisionSettings.getPreRotateUsage(), "Part BottomVisionSettings should be the built-in default");
        assertEquals(bottomVisionSettings.getCheckPartSizeMethod(), defaultBottomVisionSettings.getCheckPartSizeMethod(), "Part BottomVisionSettings should be the built-in default");
        assertEquals(bottomVisionSettings.getCheckSizeTolerancePercent(), defaultBottomVisionSettings.getCheckSizeTolerancePercent(), "Part BottomVisionSettings should be the built-in default");
        assertEquals(bottomVisionSettings.getMaxRotation(), defaultBottomVisionSettings.getMaxRotation(), "Part BottomVisionSettings should be the built-in default");
    }

    @Test
    public void testBottomVisionSettingsInheritance() throws Exception {
        Part part = Configuration.get().getPart("R0805-1K");
        Package pkg = part.getPackage();
        assertNull(part.getVisionSettings(), "Part Bottom Vision should be null");
        assertNull(pkg.getVisionSettings(), "Part Package Bottom Vision should be null");
        
        BottomVisionSettings bottomVisionSettings = bottomVision.getBottomVisionSettings(part);
        assertBottomVisionIsDefault(bottomVisionSettings);
        
        BottomVisionSettings customVisionSettings = new BottomVisionSettings();
        customVisionSettings.setEnabled(false);
        pkg.setVisionSettings(customVisionSettings);

        assertNull(part.getVisionSettings(),"Part Bottom Vision should be null");
        bottomVisionSettings = bottomVision.getBottomVisionSettings(part);
        assertFalse(bottomVisionSettings.isEnabled(), "Part should inherit BottomVisionSettings from Package");
        
        bottomVision.getBottomVisionSettings().setPreRotateUsage(ReferenceBottomVision.PreRotateUsage.AlwaysOn);
        bottomVisionSettings = bottomVision.getBottomVisionSettings(part);
        assertEquals(ReferenceBottomVision.PreRotateUsage.Default, bottomVisionSettings.getPreRotateUsage(), "Part should inherit from Package custom settings");
        
        pkg.resetVisionSettings();
        bottomVisionSettings = bottomVision.getBottomVisionSettings(part);
        assertEquals(ReferenceBottomVision.PreRotateUsage.AlwaysOn, bottomVisionSettings.getPreRotateUsage(),"Part should inherit from Package custom settings which is the built-in default");
    }
    
    @Test
    public void testBottomVisionReset() {
        Part part1 = Configuration.get().getPart("R0805-1K");
        Part part2 = Configuration.get().getPart("R0805-2K");
        Package pkg = part1.getPackage();

        BottomVisionSettings customPackageVisionSettings = new BottomVisionSettings();
        customPackageVisionSettings.setPreRotateUsage(ReferenceBottomVision.PreRotateUsage.AlwaysOn);
        customPackageVisionSettings.setMaxRotation(ReferenceBottomVision.MaxRotation.Full);
        pkg.setVisionSettings(customPackageVisionSettings);
        
        BottomVisionSettings customPartVisionSettings = new BottomVisionSettings();
        customPackageVisionSettings.setPreRotateUsage(ReferenceBottomVision.PreRotateUsage.AlwaysOn);
        customPartVisionSettings.setMaxRotation(ReferenceBottomVision.MaxRotation.Adjust);
        part1.setVisionSettings(customPartVisionSettings);
        
        pkg.resetParts();
        assertNull(part1.getVisionSettings(), "Part Bottom Vision should be null");
        assertNull(part2.getVisionSettings(), "Part Bottom Vision should be null");
        
        assertEquals(ReferenceBottomVision.PreRotateUsage.AlwaysOn, bottomVision.getBottomVisionSettings(part1).getPreRotateUsage(), "Part1 should inherit BottomVisionSettings from Package");
        assertEquals(ReferenceBottomVision.MaxRotation.Full, bottomVision.getBottomVisionSettings(part1).getMaxRotation(), "Part1 should inherit BottomVisionSettings from Package");

        assertEquals(ReferenceBottomVision.PreRotateUsage.AlwaysOn, bottomVision.getBottomVisionSettings(part2).getPreRotateUsage(), "Part2 should inherit BottomVisionSettings from Package");
        assertEquals(ReferenceBottomVision.MaxRotation.Full, bottomVision.getBottomVisionSettings(part2).getMaxRotation(), "Part2 should inherit BottomVisionSettings from Package");
        
        customPackageVisionSettings.setValues(bottomVision.getBottomVisionSettings());
        
        assertBottomVisionIsDefault(bottomVision.getBottomVisionSettings(part1));
        assertBottomVisionIsDefault(bottomVision.getBottomVisionSettings(part2));
    }
}
