import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

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

import com.google.common.io.Files;

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
        bottomVision = ReferenceBottomVision.getDefault();
    }
    
    private void assertBottomVisionIsDefault(BottomVisionSettings bottomVisionSettings) {
        BottomVisionSettings defaultBottomVisionSettings = bottomVision.getVisionSettings();
        
        assertEquals(bottomVisionSettings.isEnabled(), defaultBottomVisionSettings.isEnabled(), "Part BottomVisionSettings should be the built-in default");
        assertEquals(bottomVisionSettings.getPreRotateUsage(), defaultBottomVisionSettings.getPreRotateUsage(), "Part BottomVisionSettings should be the built-in default");
        assertEquals(bottomVisionSettings.getCheckPartSizeMethod(), defaultBottomVisionSettings.getCheckPartSizeMethod(), "Part BottomVisionSettings should be the built-in default");
        assertEquals(bottomVisionSettings.getCheckSizeTolerancePercent(), defaultBottomVisionSettings.getCheckSizeTolerancePercent(), "Part BottomVisionSettings should be the built-in default");
        assertEquals(bottomVisionSettings.getMaxRotation(), defaultBottomVisionSettings.getMaxRotation(), "Part BottomVisionSettings should be the built-in default");
        assertEquals(bottomVisionSettings.getVisionOffset(), defaultBottomVisionSettings.getVisionOffset(), "Part BottomVisionSettings should be the built-in default");
    }

    @Test
    public void testBottomVisionSettingsInheritance() {
        Part part = Configuration.get().getPart("R0805-1K");
        Package pkg = part.getPackage();
        assertNull(part.getVisionSettings(), "Part Bottom Vision should be null");
        assertNull(pkg.getVisionSettings(), "Part Package Bottom Vision should be null");
        
        BottomVisionSettings bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        assertBottomVisionIsDefault(bottomVisionSettings);
        
        BottomVisionSettings customVisionSettings = new BottomVisionSettings();
        customVisionSettings.setEnabled(false);
        pkg.setVisionSettings(customVisionSettings);

        assertNull(part.getVisionSettings(),"Part Bottom Vision should be null");
        bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        assertFalse(bottomVisionSettings.isEnabled(), "Part should inherit BottomVisionSettings from Package");
        
        bottomVision.getVisionSettings().setPreRotateUsage(ReferenceBottomVision.PreRotateUsage.AlwaysOn);
        bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        assertEquals(ReferenceBottomVision.PreRotateUsage.Default, bottomVisionSettings.getPreRotateUsage(), "Part should inherit from Package custom settings");
        
        pkg.setVisionSettings(null);
        bottomVisionSettings = bottomVision.getInheritedVisionSettings(part);
        assertEquals(ReferenceBottomVision.PreRotateUsage.AlwaysOn, bottomVisionSettings.getPreRotateUsage(),"Part should inherit from Package custom settings which is the built-in default");
    }
    
    @Test
    public void testBottomVisionReset() throws CloneNotSupportedException {
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
        
        pkg.resetSpecializedVisionSettings();
        assertNull(part1.getVisionSettings(), "Part Bottom Vision should be null");
        assertNull(part2.getVisionSettings(), "Part Bottom Vision should be null");
        
        assertEquals(ReferenceBottomVision.PreRotateUsage.AlwaysOn, bottomVision.getInheritedVisionSettings(part1).getPreRotateUsage(), "Part1 should inherit BottomVisionSettings from Package");
        assertEquals(ReferenceBottomVision.MaxRotation.Full, bottomVision.getInheritedVisionSettings(part1).getMaxRotation(), "Part1 should inherit BottomVisionSettings from Package");

        assertEquals(ReferenceBottomVision.PreRotateUsage.AlwaysOn, bottomVision.getInheritedVisionSettings(part2).getPreRotateUsage(), "Part2 should inherit BottomVisionSettings from Package");
        assertEquals(ReferenceBottomVision.MaxRotation.Full, bottomVision.getInheritedVisionSettings(part2).getMaxRotation(), "Part2 should inherit BottomVisionSettings from Package");
        
        customPackageVisionSettings.setValues(bottomVision.getVisionSettings());
        
        assertBottomVisionIsDefault(bottomVision.getInheritedVisionSettings(part1));
        assertBottomVisionIsDefault(bottomVision.getInheritedVisionSettings(part2));
    }
}
