package org.openpnp.vision.pipeline.stages.convert;

import java.awt.Color;

import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

public class ColorConverter implements Converter<Color> {

    @Override
    public Color read(InputNode node) throws Exception {
        int r = Integer.parseInt(node.getAttribute("r").getValue());
        int g = Integer.parseInt(node.getAttribute("g").getValue());
        int b = Integer.parseInt(node.getAttribute("b").getValue());
        int a = Integer.parseInt(node.getAttribute("a").getValue());
        return new Color(r, g, b, a);
    }

    @Override
    public void write(OutputNode node, Color value) throws Exception {
        node.setAttribute("r", "" + value.getRed());
        node.setAttribute("g", "" + value.getGreen());
        node.setAttribute("b", "" + value.getBlue());
        node.setAttribute("a", "" + value.getAlpha());
    }
}
