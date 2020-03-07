/*
 * Copyright (C) 2020 <mark@makr.zone>
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
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

package org.openpnp.vision.pipeline.stages;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Core.MinMaxLocResult;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.CvStage.Result.TemplateMatch;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

@Stage(description="A very simple OCR stage that returns a text string. For acceptable speed use AffineWarp to extract the "
        + "smallest-possible region of text first.")
public class SimpleOcr extends CvStage {
    @Attribute
    @Property(description = "Alphabet of all the caracters that can be recognized. The smaller the alphabet, the faster and the "
            + "more likely to be accurate.")
    private String alphabet = "0123456789.-+RCLIUVAFH%TGMKkmuµnp";

    @Attribute
    @Property(description = "Name of the font to be recognized. Monospace fonts work much better and with lower resolution. "
            + "Use a font where all the used characters are easily distinguishable. Fonts with clear separataion between caharacters "
            + "are preferred. ")
    private String fontName = "Liberation Mono";

    @Attribute
    @Property(description = "Size of the font in points.")
    private double pointSize = 7.0;

    @Attribute(required = false)
    @Property(description = "If the font size is larger in pixels than this value, the stage resizes the image to a smaller resolution first, "
            + "for acceptable OCR speed. Alternatively, use a AffineWarp stage with scale.")
    private int fontMaxPixelSize = 20;

    @Attribute(required = false)
    @Property(description = "<strong style=\"color:red;\">CAUTION, Quick&Dirty Hack:</strong> This will "
            + "auto-detect the font size upwards and downwards of your currently set pointSize. "
            + "This is a one-shot option, used while editing the pipeline. Once the detection is done, the switch "
            + "is cleared.<br/>"
            + "The process may take a while and appear to hang, be patient. Afterwards you need to switch stages to refresh the GUI.")
    private boolean autoDetectSize = false;

    @Attribute(required = false)
    @Property(description = "Template matching minimum match threshold (CCOEFF_NORMED method). Default is 0.7.")
    private double threshold = 0.7;

    @Attribute(required = false)
    @Property(description = "If more than one match is found, the others must be above this ratio to the maximum. Default is 0.85.")
    private double corr = 0.85;

    @Attribute(required = false)
    @Property(description = "Debug write images. Will slow down operation.")
    private boolean debug;


    public String getAlphabet() {
        return alphabet;
    }

    public void setAlphabet(String alphabet) {
        this.alphabet = alphabet;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public double getPointSize() {
        return pointSize;
    }

    public void setPointSize(double pointSize) {
        this.pointSize = pointSize;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public double getCorr() {
        return corr;
    }

    public void setCorr(double corr) {
        this.corr = corr;
    }

    public int getFontMaxPixelSize() {
        return fontMaxPixelSize;
    }

    public void setFontMaxPixelSize(int fontMaxPixelSize) {
        this.fontMaxPixelSize = fontMaxPixelSize;
    }

    public boolean isAutoDetectSize() {
        return autoDetectSize;
    }

    public void setAutoDetectSize(boolean autoDetectSize) {
        this.autoDetectSize = autoDetectSize;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    protected static class CharacterMatch extends TemplateMatch {
        public CharacterMatch(char ch, double x, double y, double width, double height, double score) {
            super(x, y, width, height, score);
            this.ch = ch;
        }

        private char ch;
        private int bonus = 0;
        private int malus = 0;
        private boolean excluded = false; 

        protected double getOverallScore() {
            return (score + 0.05*bonus - 0.05* malus); // this is scaled to the width, so narrow characters count as less good matches
        }

        protected boolean overlaps(CharacterMatch sibling) {
            final double tolerance = height/10.0;
            return (sibling.x+tolerance < this.x + this.width)
                    && (sibling.x+sibling.width-tolerance > this.x)
                    && (sibling.y+tolerance < this.y + this.height)
                    && (sibling.y+sibling.height-tolerance > this.y);
        }
        protected boolean expandsOver(CharacterMatch sibling) {
            final double tolerance = height/10.0;
            return (Math.abs(sibling.x - this.x) < tolerance
                    && Math.abs(sibling.y - this.y) < tolerance)
                    && (sibling.width < this.width);
        }
        protected boolean precedesInLine(CharacterMatch sibling) {
            final double tolerance = height/10.0;
            return (Math.abs(sibling.x - (this.x + this.width)) < tolerance
                    && Math.abs(sibling.y - this.y) < tolerance);
        }
        protected void setExcluded() {
            excluded = true;
        }
        protected boolean isExcluded() {
            return excluded;
        }
        public char getCh() {
            return ch;
        }
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (alphabet == null || alphabet.isEmpty()) {
            return null;
        }
        if (fontName == null || fontName.isEmpty()) {
            return null;
        }

        Camera camera = (Camera) pipeline.getProperty("camera");
        if (camera == null) {
            throw new Exception("Property \"camera\" is required.");
        }

        if (autoDetectSize) {
            autoDetectSize = false;
            // very crude brute force and no refinement
            double pointSizeOrg = pointSize;
            OcrResult bestRes = null;
            double bestSize = Double.NaN;
            pointSize = pointSizeOrg*0.5;
            while (pointSize < pointSizeOrg*2.0) {
                Logger.debug("["+getClass().getName()+"] auto-detecting at point size = "+pointSize);
                OcrResult res = (OcrResult)performOcr(pipeline, camera).model;
                if (res.overallScore > 0.0) {
                    if (bestRes == null ||  bestRes.overallScore < res.overallScore) {
                        bestRes = res;
                        bestSize = pointSize;
                        Logger.debug("["+getClass().getName()+"] new best Size = "+pointSize+", overallScore = "+bestRes.overallScore+", text = "+bestRes.text);
                    }
                }
                pointSize *= 1.05;
            }
            if (bestRes != null) {
                pointSize = bestSize; 
            }
            else {
                pointSize = pointSizeOrg;
            }
        }
        return performOcr(pipeline, camera);
    }

    public static class OcrResult {
        private String text;
        private double overallScore;
        public OcrResult(String text, double overallScore) {
            super();
            this.text = text;
            this.overallScore = overallScore;
        }
        public String getText() {
            return text;
        }
        public double getOverallScore() {
            return overallScore;
        } 

        @Override
        public String toString() {
            return "OcrResult [text=" + text + ", score=" + overallScore + "]";
        }
    }

    protected Result performOcr(CvPipeline pipeline, Camera camera) throws Error, IOException {
        Location unitsPerPixel = camera.getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters);

        // Determine the scaling factor to go from mm/point units to
        // Camera units and pixels.
        Length l = new Length(1.0/72.0, LengthUnit.Inches);
        l = l.convertToUnits(unitsPerPixel.getUnits());
        double ptScale = l.getValue()/unitsPerPixel.getY();

        // get the working image
        Mat textImage = pipeline.getWorkingImage();

        // automatic rescale
        double rescale = 1.0;
        if (fontMaxPixelSize > 5.0 && fontMaxPixelSize < rescale*ptScale*pointSize) {
            rescale = fontMaxPixelSize  / (rescale*ptScale*pointSize);
        }
        if (rescale != 1.0) {
            Mat dst = new Mat();
            Size size = new Size(textImage.cols()*rescale, textImage.rows()*rescale);
            Imgproc.resize(textImage, dst, size);
            // we mus not: textImage.release(); It is the property of the previous stage.
            textImage = dst;
            ptScale *= rescale;
            unitsPerPixel = unitsPerPixel.multiply(1.0/rescale, 1.0/rescale, 0, 0);
        }

        // get the corresponding buffered image type
        Integer type = null;
        if (textImage.type() == CvType.CV_8UC1) {
            type = BufferedImage.TYPE_BYTE_GRAY;
        }
        else if (textImage.type() == CvType.CV_8UC3) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        else if (textImage.type() == CvType.CV_32F) {
            type = BufferedImage.TYPE_BYTE_GRAY;
            Mat tmp = new Mat();
            textImage.convertTo(tmp, CvType.CV_8UC1, 255);
            textImage = tmp;
        }
        if (type == null) {
            throw new Error(String.format("Unsupported Mat: type %d, channels %d, depth %d",
                    textImage.type(), textImage.channels(), textImage.depth()));
        }

        // create the font
        Font font = new Font(fontName, Font.PLAIN, (int)Math.round(ptScale*pointSize));
        // Create a pseudo graphics context to get text metrics 
        Graphics2D gfm = new BufferedImage(1, 1, type).createGraphics();
        FontMetrics fm = gfm.getFontMetrics(font);
        final int maxAscent = fm.getMaxAscent();
        final int fontHeight = fm.getHeight();
        final int margin = 0; // just a guess
        final int height = fontHeight+2*margin;

        List<CharacterMatch> matches = new ArrayList<>();

        // try find each character of the alphabet in the text image 
        for (char ch : alphabet.toCharArray()) {
            String character = new String(new char[] { ch });
            String characterTag = (Character.isLetterOrDigit(ch) ? character : String.valueOf((int)ch))+"-";
            // create a template image of the current character
            int width = fm.stringWidth(character)+2*margin;
            BufferedImage templateImage =
                    new BufferedImage(width, height, type);
            Graphics2D g2d = (Graphics2D) templateImage.getGraphics();
            g2d.setColor(Color.white);
            g2d.fillRect(0, 0, width, height);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.black);
            g2d.setFont(font);
            g2d.drawString(character, margin, margin+maxAscent);
            g2d.dispose();
            Mat template = OpenCvUtils.toMat(templateImage);
            if (debug) {
                File file = Configuration.get().createResourceFile(getClass(), "character-"+characterTag, ".png");
                Imgcodecs.imwrite(file.getAbsolutePath(), template);
            }

            Mat matchMap = new Mat();
            Imgproc.matchTemplate(textImage, template, matchMap, Imgproc.TM_CCOEFF_NORMED);

            /*Does not work, it seems
             if (debug) {

                File file = Configuration.get().createResourceFile(getClass(), "match-map-"+characterTag, ".png");
                Imgcodecs.imwrite(file.getAbsolutePath(), matchMap);
            }*/

            MinMaxLocResult mmr = Core.minMaxLoc(matchMap);
            double maxVal = mmr.maxVal;

            double rangeMin = Math.max(threshold, corr * maxVal);
            double rangeMax = maxVal;

            for (Point point : OpenCvUtils.matMaxima(matchMap, rangeMin, rangeMax)) {
                int x = point.x;
                int y = point.y;
                CharacterMatch match = new CharacterMatch(ch, 
                        x, y, template.cols(), template.rows(),
                        matchMap.get(y, x)[0]);
                matches.add(match);
            }
            matchMap.release();
            template.release();
        }

        if (debug) {
            Logger.debug("["+getClass().getName()+"] matches = "+matches);
        }

        StringBuilder text = new StringBuilder();
        double overallScore = 0.0;
        if (matches.size() > 0) {
            // go through all the matches and bonus/malus each other
            for (CharacterMatch match : matches) {
                // in a proportional font, the widest character should win at the end of a word/line
                boolean lastInWord = true;
                for (CharacterMatch sibling : matches) {
                    if (sibling != match) {
                        if (match.overlaps(sibling)) {
                            // overlapping 
                            sibling.malus++;
                            if (sibling.expandsOver(match)) {
                                // we're not the widest
                                lastInWord = false;
                            }
                        }
                        else if (match.precedesInLine(sibling)) {
                            // chained on one line as string
                            sibling.bonus++;
                            match.bonus++;
                            // we're not at the end of a word
                            lastInWord = false;
                        }
                    }
                }
                if (lastInWord) {
                    // we're the last on the word/line and should win over any
                    // narrower character
                    match.bonus++;
                }
            }

            // exclude overlaps
            for (CharacterMatch match : matches) {
                for (CharacterMatch sibling : matches) {
                    if (match != sibling && match.overlaps(sibling)) {
                        if (match.getOverallScore() > sibling.getOverallScore()) {
                            sibling.setExcluded();
                        }
                    }
                }
            }

            // sort by x 
            Collections.sort(matches, new Comparator<CharacterMatch>() {
                @Override
                public int compare(CharacterMatch o1, CharacterMatch o2) {
                    return ((Double) o1.x).compareTo(o2.x);
                }
            });

            // finally compose the text
            do {
                // form text lines
                int charsLeft = 0;
                double nextLineY = Double.MAX_VALUE;
                for (CharacterMatch match : matches) {
                    if (!match.isExcluded()) {
                        charsLeft++;
                        if (match.y < nextLineY) {
                            nextLineY = match.y;
                        }
                    }
                }
                if (charsLeft == 0) {
                    break; // done --> 
                }
                if (text.length() > 0) {
                    // append new line
                    text.append('\n');
                }
                double lineTolerance = height / 2;
                CharacterMatch prevChar = null;
                for (CharacterMatch match : matches) {
                    if (!match.isExcluded()) {
                        if (Math.abs(match.y - nextLineY) < lineTolerance) {
                            // append the character matching the line Y
                            if (prevChar != null && ! prevChar.precedesInLine(match)) {
                                text.append(' ');
                            }
                            text.append(match.getCh());
                            overallScore += match.getOverallScore();
                            match.setExcluded();
                            prevChar = match;
                        }
                    }
                }
            }
            while (true);
        }

        return new Result(textImage, new OcrResult(text.toString(), overallScore));
    }
}
