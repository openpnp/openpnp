package org.openpnp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PartAlignment;
import org.openpnp.spi.PartAlignment.PartAlignmentOffset;

import com.google.common.io.Files;

public class VisionUtilsPartAlignmentTest {
    @BeforeAll
    public static void load() throws Exception {
        Configuration.initialize(new File(Files.createTempDir(), ".openpnp"));
        Configuration.get().load();
    }

    @Test
    public void preRotatedTestAlignmentWithoutBoardReportsErrorToFeeder() throws Exception {
        PartAlignment alignment = mock(PartAlignment.class);
        when(alignment.findOffsets(any(), any(), any(), any())).thenReturn(
                new PartAlignmentOffset(new Location(LengthUnit.Millimeters, 0.1, 0.2, 0, 1), true));
        Feeder feeder = mock(Feeder.class);
        Nozzle nozzle = mock(Nozzle.class);
        when(nozzle.getPartsFeeder()).thenReturn(feeder);
        Placement placement = new Placement("Dummy");
        placement.setLocation(new Location(LengthUnit.Millimeters, 0, 0, 0, 90));

        VisionUtils.findPartAlignmentOffsets(alignment, new Part("P"), null, placement, nozzle);

        ArgumentCaptor<Location> error = ArgumentCaptor.forClass(Location.class);
        verify(feeder).deferredBottomVisionResult(error.capture());
        assertEquals(0.2, error.getValue().getX(), 1e-9);
        assertEquals(-0.1, error.getValue().getY(), 1e-9);
    }
}
