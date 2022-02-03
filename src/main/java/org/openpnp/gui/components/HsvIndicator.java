/*
 * Copyright (C) 2022 <mark@makr.zone>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

public class HsvIndicator extends JComponent {

    private static final double EXCLUDED_RATIO = 0.33;

    public HsvIndicator() {
        super();
    }

    private int minHue;
    private int maxHue;
    private int minSaturation;
    private int maxSaturation;
    private int minValue;
    private int maxValue;

    @Override
    public Dimension getPreferredSize() {
        Dimension superDim = super.getPreferredSize();
        int width = (int)Math.max(superDim.getWidth(), 192);
        int height = (int)Math.max(superDim.getHeight(), 128); 
        return new Dimension(width, height);
    }

    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        int width = getWidth();
        int height = getHeight();
        int diameter = Math.min(width, height);
        int unit = diameter/5;
        double radius = diameter/2.0; 
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        boolean monochrome = minSaturation == 0 && maxSaturation == 255;
        int [] data = new int[width*height];
        for (int y = 0; y < diameter; y++) {
            for (int x = 0; x < diameter; x++) {
                double dx = x - radius;
                double dy = y - radius;
                double r = Math.sqrt(dx*dx + dy*dy);
                if (r < radius) {
                    double saturation = 255.0*r/radius;
                    double hue = 255.0*((1.0 + Math.atan2(-dy, dx)/Math.PI/2.0) % 1.0);
                    double alpha = Math.min(1.0, radius - r);
                    double dHue = r/64;
                    double dSat = radius/255;
                    double included = monochrome ? 1.0 : Math.max(0, Math.min(1.0, Math.min(
                            dHue*(minHue <= maxHue ? Math.min(hue + 1 - minHue, maxHue + 1 - hue) : Math.max(hue + 1 - minHue, maxHue + 1 - hue)),
                            dSat*Math.min(saturation + 1 - minSaturation, maxSaturation + 1 - saturation))));
                    double includedAlpha = (included*(maxValue - minValue) + minValue)/255;
                    Color color = makeHsvColor((int)hue, (isEnabled() ? (int)saturation : 0), (int) (255*includedAlpha), (int) (255*alpha));
                    data[y*width + x] = color.getRGB();
                }
            }
        }

        for (int y = 0; y < diameter; y++) {
            int value = 255 - 255*y/diameter; 
            int hue = (minHue <= maxHue ? (minHue + maxHue)/2 : ((255 + minHue + maxHue)/2) & 0xFF);
            double dVal = diameter/255.0;
            double included = Math.max(0, Math.min(1.0, 
                    dVal*(Math.min(value - minValue, maxValue - value))));
            double alpha = included*(1.0 - EXCLUDED_RATIO) + EXCLUDED_RATIO;
            int x0 = diameter + unit;
            int x1 = Math.min(diameter + unit*2, width);
            for (int x = x0; x < x1; x++) {
                double r = (double)(x - x0)/(x1 - x0);
                int saturation =  monochrome ? 0 : isEnabled() && included > 0 ? 
                        ((int) (r*(maxSaturation - minSaturation) + minSaturation)) : 0;
                Color color = makeHsvColor(hue, 
                        saturation, 
                        value, (int) (255*alpha));
                data[y*width + x] = color.getRGB();
            }
        }
        image.setRGB(0, 0, width, height, data, 0, width);
        g2d.drawImage(image, 0, 0, width, height, 0, 0, width, height, null);
    }

    public int getMinHue() {
        return minHue;
    }

    public void setMinHue(int minHue) {
        this.minHue = minHue;
        repaint();
    }

    public int getMaxHue() {
        return maxHue;
    }

    public void setMaxHue(int maxHue) {
        this.maxHue = maxHue;
        repaint();
    }

    public int getMinSaturation() {
        return minSaturation;
    }

    public void setMinSaturation(int minSaturation) {
        this.minSaturation = minSaturation;
        repaint();
    }

    public int getMaxSaturation() {
        return maxSaturation;
    }

    public void setMaxSaturation(int maxSaturation) {
        this.maxSaturation = maxSaturation;
        repaint();
    }

    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
        repaint();
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        repaint();
    }

    private Color makeHsvColor(int hue, int saturation, int value, int alpha){
        double s = saturation/255.0;
        double v = value/255.0;
        double h = 360.0*hue/255;
        double c = s*v;
        double x = c*(1 - Math.abs(((h/60.0) % 2) - 1.0));
        double m = v - c;
        double r, g, b;
        if (h >= 0 && h < 60){
            r = c; 
            g = x; 
            b = 0;
        }
        else if (h >= 60 && h < 120){
            r = x; 
            g = c; 
            b = 0;
        }
        else if (h >= 120 && h < 180){
            r = 0; 
            g = c; 
            b = x;
        }
        else if (h >= 180 && h < 240){
            r = 0; 
            g = x; 
            b = c;
        }
        else if (h >= 240 && h < 300){
            r = x; 
            g = 0; 
            b = c;
        }
        else{
            r = c;
            g = 0;
            b = x;
        }
        int red = (int) ((r+m)*255);
        int green = (int) ((g+m)*255);
        int blue = (int) ((b+m)*255);
        return new Color(red, green, blue, alpha);
    }
}
