package org.openpnp.gui.importer.diptrace.csv;

import com.google.common.io.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.model.Configuration;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DipTrace3xCsvParserTest {

    @BeforeEach
    void setUp() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);
        Configuration.initialize(workingDirectory);
        Configuration configuration = Configuration.get();
        configuration.load();

        removeDefaultParts(configuration);
    }

    private static void removeDefaultParts(Configuration configuration) {
        for (Part part : configuration.getParts()) {
            configuration.removePart(part);
        }
    }

    @Test
    void importLegacyCSV() throws Exception {
        // given
        DipTrace3xCsvParser parser = new DipTrace3xCsvParser();
        boolean createMissingParts = true;
        boolean updateHeights = true;

        // and
        String content =
            "RefDes,Name,X (mm),Y (mm),Side,Rotate,Value\n" +
            "C1,C0603,8.6,7.2,Top,0,1nF\n" +
            "C2,C0402,10.81,22.99,Top,180,0.1uF/16V\n";

        BufferedReader reader = new BufferedReader(new StringReader(content));

        // and
        // FUTURE: Improve the method of placement comparison, currently we simply use toString on each placement and compare them using regular expressions.
        //         The reason for this is that it's hard to construct a placement to compare another one against.
        //         Also, tying presentation code (Placement::toString, Location::toString) to test code, is far from ideal.
        //         And using regular expressions doesn't lend towards readable code.
        List<String> expectedPlacementPatterns = List.of(
            "^Placement C1 @(.*)+, location=\\(8\\.600000, 7\\.200000, 0\\.000000, 0\\.000000 mm\\), side=Top, part=id C0603-1nF, name null, heightUnits Millimeters, height 0\\.000000, packageId \\(null\\), type=Placement$",
            "^Placement C2 @(.*)+, location=\\(10\\.810000, 22\\.990000, 0\\.000000, 180\\.000000 mm\\), side=Top, part=id C0402-0.1uF/16V, name null, heightUnits Millimeters, height 0\\.000000, packageId \\(null\\), type=Placement$"
        );

        // when
        List<Placement> placements = parser.parse(reader, createMissingParts, updateHeights);

        // then
        Configuration configuration = Configuration.get();

        // and
        List<String> placementStrings = placements.stream().map((Placement::toString)).collect(Collectors.toList());
        assertLinesMatch(expectedPlacementPatterns, placementStrings);

        // and
        assertEquals(2, configuration.getParts().size());
    }
}