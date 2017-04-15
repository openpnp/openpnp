import java.io.File;

import org.junit.Test;
import org.openpnp.gui.importer.EagleMountsmdUlpImporter;
import org.openpnp.model.Board.Side;
import org.openpnp.model.Configuration;

import com.google.common.io.Files;

public class EagleMountsmdUlpImporterTest {
    @Test
    public void testDemoBoard() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        File directory = new File("samples", "Demo Board");
        File top = new File(directory, "Demo Board v2.mnt");
        File bottom = new File(directory, "Demo Board v2.mnb");
        
        EagleMountsmdUlpImporter.parseFile(top, Side.Top, true);
        EagleMountsmdUlpImporter.parseFile(bottom, Side.Bottom, true);
        
        directory = new File("samples", "EAT001");
        top = new File(directory, "EAT001.mnt");
        bottom = new File(directory, "EAT001.mnb");
        
        EagleMountsmdUlpImporter.parseFile(top, Side.Top, true);
        EagleMountsmdUlpImporter.parseFile(bottom, Side.Bottom, true);
    }
    
    @Test
    public void testEAT001() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        File directory = new File("samples", "EAT001");
        File top = new File(directory, "EAT001.mnt");
        File bottom = new File(directory, "EAT001.mnb");
        
        EagleMountsmdUlpImporter.parseFile(top, Side.Top, true);
        EagleMountsmdUlpImporter.parseFile(bottom, Side.Bottom, true);
    }

    /**
     * Tests to to handle whole numbers in Eagle MN* files. See:
     * https://github.com/openpnp/openpnp/issues/390
     * @throws Exception
     */
    @Test
    public void testWholeNumbers() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        File directory = new File("samples", "test");
        File top = new File(directory, "mountsmd_whole_numbers.mnt");
        
        EagleMountsmdUlpImporter.parseFile(top, Side.Top, true);
    }
}
