package org.openpnp.machine.index;

import com.google.common.io.Files;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openpnp.model.Configuration;

import java.io.File;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * This has to be a separate test from IndexFeederTest because in there we load the machine in the setup which precludes
 * us from testing if the index properties are correctly loaded based on the machine.
 *
 * @see IndexFeederTest
 */
public class IndexFeederLoadingTest {

    private IndexFeeder feeder;

    @Before
    public void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);

        feeder = new IndexFeeder();
    }

    @Test
    public void loadingOfIndexProperties() throws Exception {
        assertNull(feeder.indexProperties);

        Configuration.get().load();

        assertSame(Configuration.get().getMachine(), feeder.indexProperties.machine);
    }
}
