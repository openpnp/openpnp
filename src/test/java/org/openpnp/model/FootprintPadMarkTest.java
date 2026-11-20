package org.openpnp.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class FootprintPadMarkTest {
    private static Footprint.Pad pad(boolean mark) {
        Footprint.Pad pad = new Footprint.Pad();
        pad.setX(-4);
        pad.setY(-2.625);
        pad.setWidth(0.36);
        pad.setHeight(1.0);
        pad.setRotation(90);
        pad.setMark(mark);
        return pad;
    }

    private static double[] points(Shape shape) {
        List<Double> list = new ArrayList<>();
        double[] c = new double[6];
        for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
            int type = it.currentSegment(c);
            list.add((double) type);
            for (int i = 0; i < 6; i++) {
                list.add(c[i]);
            }
        }
        return list.stream().mapToDouble(Double::doubleValue).toArray();
    }

    @Test
    public void markedPadIsFilledAtItsCenter() {
        // Vision fills the pads shape to make templates, so a mark must not cut a hole in it.
        assertTrue(pad(true).getShape().contains(-4, -2.625));
    }

    @Test
    public void markDoesNotChangeThePadShape() {
        assertArrayEquals(points(pad(false).getShape()), points(pad(true).getShape()), 1e-12);
    }
}
