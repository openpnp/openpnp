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

import javax.imageio.ImageIO;

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

@Stage(description="A very simple OCR stage that returns a (multi-line) text string. <br/>"
        + "Use an AffineWarp stage to extract the region of interest first (for cropping, rotation and acceptable speed).<br/>"
        + "It is also recommended to convert the image to grayscale first. Do not apply a threshold stage.")
public class SimpleOcr extends CvStage {
    @Attribute
    @Property(description = "Alphabet of all the characters that can be recognized. The smaller the alphabet, the faster and the "
            + "more reliable the OCR works. The alphabet can be overriden with the \"alphabet\" property.")
    private String alphabet = "0123456789.-+_RCLDQYXJIVAFH%GMKkmuµnp";

    @Attribute
    @Property(description = "Name of the font to be recognized. Monospace fonts work much better and allow lower resolution. "
            + "Use a font where all the used characters are easily distinguishable. Fonts with clear separation between characters "
            + "are preferred.")
    private String fontName = "Liberation Mono";

    @Attribute
    @Property(description = "Size of the font in typographic points (1 pt = 1/72 in).")
    private double fontSizePt = 7.0;

    @Attribute(required = false)
    @Property(description = "If the font size is larger in pixels than this value, the OCR stage will resizes the image to a smaller resolution first "
            + "(to achieve acceptable OCR speed). Alternatively, you can use an AffineWarp stage with scale < 1.0, but then you need to scale your pointSize too.")
    private int fontMaxPixelSize = 20;

    @Attribute(required = false)
    @Property(description = "<strong style=\"color:red;\">CAUTION, Quick&Dirty Hack:</strong> This will "
            + "auto-detect the font size upwards and downwards of your currently set pointSize. "
            + "This is a one-shot option, used while editing the pipeline. Once the detection is done, the switch "
            + "is cleared.<br/>"
            + "The process may take a while and appear to hang, be patient. Afterwards you need to switch stages to refresh the GUI.")
    private boolean autoDetectSize = false;

    @Attribute(required = false)
    @Property(description = "Template matching minimum match threshold (CCOEFF_NORMED method). Default is 0.75.")
    private double threshold = 0.75;

    public enum DrawStyle {
        None,
        OverScaledImage, 
        OverOriginalImage
    };

    @Attribute(required = false)
    @Property(description = "Draw the OCR match onto the image. ")
    private DrawStyle drawStyle = DrawStyle.OverOriginalImage;

    @Attribute(required = false)
    @Property(description = "Write debug images and messages. Will slow down operation.")
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

    public double getFontSizePt() {
        return fontSizePt;
    }

    public void setFontSizePt(double fontSizePt) {
        this.fontSizePt = fontSizePt;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
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

    public DrawStyle getDrawStyle() {
        return drawStyle;
    }

    public void setDrawStyle(DrawStyle drawStyle) {
        this.drawStyle = drawStyle;
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
        private int charNum = 0; 

        protected double getOverallScore() {
            return (score + 0.05*bonus - 0.05* malus); // this is scaled to the width, so narrow characters count as less good matches
        }

        protected boolean overlaps(CharacterMatch sibling) {
            final double tolerance = height/10.0+1;
            return (sibling.x+tolerance < this.x + this.width)
                    && (sibling.x+sibling.width-tolerance > this.x)
                    && (sibling.y+tolerance < this.y + this.height)
                    && (sibling.y+sibling.height-tolerance > this.y);
        }
        protected boolean expandsRight(CharacterMatch sibling) {
            final double tolerance = height/10.0+1;
            return (Math.abs(sibling.x - this.x) < tolerance
                    && Math.abs(sibling.y - this.y) < tolerance)
                    && (sibling.width < this.width);
        }
        protected boolean expandsLeft(CharacterMatch sibling) {
            final double tolerance = height/10.0+1;
            return (Math.abs(sibling.x + sibling.width - this.x - this.width) < tolerance
                    && Math.abs(sibling.y - this.y) < tolerance)
                    && (sibling.width < this.width);
        }
        protected boolean precedesInLine(CharacterMatch sibling) {
            final double tolerance = height/10.0+1;
            return (Math.abs(sibling.x - (this.x + this.width)) < tolerance
                    && Math.abs(sibling.y - this.y) < tolerance);
        }
        protected void setExcluded() {
            excluded = true;
        }
        protected boolean isExcluded() {
            return excluded;
        }
        protected void setCharNum(int charNum) {
            this.charNum = charNum;
            excluded = true;
        }
        protected int getCharNum() {
            return charNum;
        }
        public char getCh() {
            return ch;
        }

        @Override
        public String toString() {
            return "CharacterMatch [x=" + x + ", y=" + y + ", width=" + width + ", height="
                    + height + ", ch=\"" + ch + "\", score=" + score + ", charNum="+charNum+"]";
        }
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Camera camera = (Camera) pipeline.getProperty("camera");
        if (camera == null) {
            throw new Exception("Property \"camera\" is required.");
        }

        String alphabet = (String)pipeline.getProperty("alphabet");
        if (alphabet == null) {
            alphabet = getAlphabet();
        }
        if (alphabet == null || alphabet.isEmpty()) {
            return null;
        }
        String fontName = (String)pipeline.getProperty("fontName");
        if (fontName == null) {
            fontName = getFontName();
        }
        if (fontName == null || fontName.isEmpty()) {
            return null;
        }
        Double fontSizePt = (Double)pipeline.getProperty("fontSizePt");
        if (fontSizePt == null) {
            fontSizePt = getFontSizePt();
        }
        if (fontSizePt == null || fontSizePt == 0.0) {
            return null;
        }

        // Note, the following is an ugly HACK, to get this functionality within the constraints of pipeline processing
        if (autoDetectSize) {
            autoDetectSize = false;
            // very crude and brute force 
            OcrModel bestRes = null;
            double bestSize = Double.NaN;
            for (double testSize = getFontSizePt()*0.5;
                    testSize < getFontSizePt()*2.0;
                    testSize *= 1.05) {  // 5% steps
                Logger.debug("["+getClass().getName()+"] auto-detecting at font size = "+testSize+"pt");
                OcrModel res = (OcrModel)performOcr(pipeline, camera, fontName, testSize, alphabet).model;
                if (res.overallScore > 0.0) {
                    if (bestRes == null ||  bestRes.overallScore < res.overallScore) {
                        bestRes = res;
                        bestSize = testSize;
                        Logger.debug("["+getClass().getName()+"] new best font size = "+testSize+"pt, overallScore = "+bestRes.overallScore+", text = "+bestRes.text);
                    }
                }
            }
            if (bestRes != null) {
                setFontSizePt(Math.round(bestSize*100.0)/100.0); 
                fontSizePt = bestSize;
            }
        }

        return performOcr(pipeline, camera, fontName, fontSizePt, alphabet);
    }

    public static class OcrModel {
        private String text;
        private int numChars;
        private double overallScore;

        public OcrModel(String text, int numChars, double overallScore) {
            super();
            this.text = text;
            this.numChars = numChars;
            this.overallScore = overallScore;
        }
        public String getText() {
            return text;
        }
        public int getNumChars() {
            return numChars;
        } 
        public double getOverallScore() {
            return overallScore;
        } 
        public double getAvgScore() {
            return overallScore/Math.max(1,  numChars);
        } 

        @Override
        public String toString() {
            return "OcrResult [text=" + text + ", numChars=" + numChars + ", score=" + overallScore + "]";
        }
    }

    protected Result performOcr(CvPipeline pipeline, Camera camera, String fontName, double fontSizePt, String alphabet) throws Error, IOException {

        // Determine the scaling factor to go from given LengthUnit/pt units to
        // Camera units and pixels.
        Location unitsPerPixel = camera.getUnitsPerPixelAtZ().convertToUnits(LengthUnit.Millimeters);
        Length l = new Length(1.0/72.0, LengthUnit.Inches);
        l = l.convertToUnits(unitsPerPixel.getUnits());
        double scalePt = l.getValue()/unitsPerPixel.getY();

        // get the working image
        Mat textImage = pipeline.getWorkingImage();

        // Automatic rescale, but don't do it if we're already too close i.e. at least a 0.5 x rescale must be achieved,
        // otherwise the image quality suffers too much.
        double rescale = 1.0;
        if (fontMaxPixelSize >= 7 && fontMaxPixelSize < 0.5*rescale*scalePt*fontSizePt) {
            rescale = fontMaxPixelSize  / (rescale*scalePt*fontSizePt);
            if (debug) {
                Logger.debug("["+getClass().getName()+"] rescale of input = "+rescale);
            }
        }
        if (rescale != 1.0) {
            Mat dst = new Mat();
            Size size = new Size(textImage.cols()*rescale, textImage.rows()*rescale);
            Imgproc.resize(textImage, dst, size);
            // we must NOT do a textImage.release(); It is the property of the previous stage.
            textImage = dst;
            scalePt *= rescale;
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
        Font font = new Font(fontName, Font.PLAIN, (int)Math.round(scalePt*fontSizePt));
        // Create a pseudo graphics context to get font metrics 
        Graphics2D gfm = new BufferedImage(1, 1, type).createGraphics();
        FontMetrics fm = gfm.getFontMetrics(font);
        final int maxAscent = fm.getAscent();// fm.getMaxAscent();
        final int fontHeight = maxAscent+fm.getDescent();//fm.getHeight();
        final int margin = 0; // tests have shown that no margin is best
        final int height = fontHeight+2*margin;
        if (fontHeight < 5 || fontHeight >= textImage.rows()) {
            // dud
            return new Result(textImage, new OcrModel("", 0, 0.0));
        }

        // try find each character of the alphabet in the text image 
        List<CharacterMatch> matches = new ArrayList<>();
        for (char ch : alphabet.toCharArray()) {
            if (ch == ' ' ) {
                // we can't search for nothing :-) 
                // spaces will be recognized by discontinuity
                continue;
            }
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

            // do the actual template match
            Mat matchMap = new Mat();
            Imgproc.matchTemplate(textImage, template, matchMap, Imgproc.TM_CCOEFF_NORMED);

            // determine the range
            MinMaxLocResult mmr = Core.minMaxLoc(matchMap);
            double maxVal = mmr.maxVal;
            double rangeMin = threshold;
            double rangeMax = maxVal;

            // create the matches
            for (Point point : OpenCvUtils.matMaxima(matchMap, rangeMin, rangeMax)) {
                int x = point.x;
                int y = point.y;
                CharacterMatch match = new CharacterMatch(ch, 
                        x, y, template.cols(), template.rows(),
                        matchMap.get(y, x)[0]);
                matches.add(match);
            }

            if (debug) {
                File file = Configuration.get().createResourceFile(getClass(), "match-map-"+characterTag, ".png");
                // this is a 3x32bit image, cannot save this as .png, need to convert to known image format first
                BufferedImage img = OpenCvUtils.toBufferedImage(matchMap);
                ImageIO.write(img, "png", file);
            }

            // cleanup
            matchMap.release();
            template.release();
        }

        // ready to harvest
        StringBuilder text = new StringBuilder();
        double overallScore = 0.0;
        int numChars = 0;

        if (matches.size() > 0) {
            // go through all the matches and bonus/malus each other by overlaps (bad) 
            // and continuity on a line (good). Some special treatment is needed for first/last character in a word
            // because there is no continuity to judge the quality. Instead we must determine which character expands 
            // farthest to the edge. Otherwise in some proportional fonts, because an "r" is a partial match of an "n" (which 
            // is a partial match of an "m" etc.) can have a high template match score and the same continuity as the larger 
            // character and win. With the first/last character test, this seems to be eliminated.
            // However, there is still the chance that "rn" is seen as "m", and therefore monospaced fonts are still recommended. 
            for (CharacterMatch match : matches) {
                // in a proportional font, the widest character should win at the end of a word/line
                boolean firstInWord = true;
                boolean lastInWord = true;
                for (CharacterMatch sibling : matches) {
                    if (sibling != match) {
                        if (match.overlaps(sibling)) {
                            // overlapping 
                            sibling.malus++;
                            if (sibling.expandsLeft(match)) {
                                // we're not the widest to the left
                                firstInWord = false;
                            }
                            if (sibling.expandsRight(match)) {
                                // we're not the widest to the right
                                lastInWord = false;
                            }
                        }
                        else if (match.precedesInLine(sibling)) {
                            // continuity in line to sibling
                            match.bonus++;
                            // we're not at the end of a word
                            lastInWord = false;
                        }
                        else if (sibling.precedesInLine(match)) {
                            // continuity in line to us
                            match.bonus++;
                            // we're not at the beginning of a word
                            firstInWord = false;
                        }
                    }
                }
                if (firstInWord) {
                    // we're the first on the word/line and should win over any narrower character
                    match.bonus++;
                }
                if (lastInWord) {
                    // we're the last on the word/line and should win over any narrower character
                    match.bonus++;
                }
            }

            // exclude overlaps by overall score, weighing in any bonus/malus 
            for (CharacterMatch match : matches) {
                for (CharacterMatch sibling : matches) {
                    if (match != sibling && match.overlaps(sibling)) {
                        if (match.getOverallScore() > sibling.getOverallScore()) {
                            // beat you!
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

            // finally compose the lines of the text
            double prevLineY = -1;
            do {
                // find the next line Y by finding the top-most character
                int charsLeft = 0;
                double nextLineY = Double.MAX_VALUE;
                for (CharacterMatch match : matches) {
                    if (!match.isExcluded()) {
                        if (match.y < prevLineY) {
                            // character off the line grid
                            match.setExcluded();
                        }
                        else {
                            charsLeft++;
                            if (match.y < nextLineY) {
                                nextLineY = match.y;
                            }
                        }
                    }
                }
                if (charsLeft == 0) {
                    break; // done --> 
                }
                if (text.length() > 0) {
                    // this isn't the first line, append new line
                    text.append('\n');
                }
                // anything roughly on the same Y will be composed into the line
                final double lineTolerance = height / 4;
                CharacterMatch prevChar = null;
                for (CharacterMatch match : matches) {
                    if (!match.isExcluded()) {
                        if (Math.abs(match.y - nextLineY) < lineTolerance) {
                            // the character matches the line Y
                            if (prevChar != null && ! prevChar.precedesInLine(match)) {
                                // discontinuity detected, first add a space 
                                // Note, we collapse all whitespace into one space
                                text.append(' ');
                            }
                            // finally harvest the character
                            text.append(match.getCh());
                            numChars++;
                            // we're done with that one
                            match.setCharNum(numChars);
                            overallScore += match.getOverallScore();
                            // remember for space detection
                            prevChar = match;

                            prevLineY = Math.max(prevLineY, match.y+match.height-lineTolerance);
                        }
                    }
                }
            }
            while (true);
        }

        if (debug) {
            // just for debugging: sort taken characters first, then others by x  
            Collections.sort(matches, new Comparator<CharacterMatch>() {
                @Override
                public int compare(CharacterMatch o1, CharacterMatch o2) {
                    if (o1.charNum > 0 && o2.charNum == 0) {
                        return -1;
                    }
                    if (o1.charNum == 0 && o2.charNum > 0) {
                        return 1;
                    }
                    int takenCmp = ((Integer) o1.charNum).compareTo(o2.charNum);
                    if (takenCmp != 0) {
                        return takenCmp;
                    }

                    return ((Double) o1.x).compareTo(o2.x);
                }
            });
            Logger.debug("["+getClass().getName()+"] matches = "+matches);
        }

        if (drawStyle != DrawStyle.None) {
            double matchScale = 1.0;
            if (drawStyle == DrawStyle.OverOriginalImage && rescale != 1.0) {
                textImage.release();
                textImage = pipeline.getWorkingImage();
                scalePt /= rescale;
                unitsPerPixel = unitsPerPixel.multiply(rescale, rescale, 0, 0);
                matchScale = 1.0/rescale;
            }
            // get the image and convert to RGB
            BufferedImage image = OpenCvUtils.toBufferedImage(textImage);
            BufferedImage colorImage = new BufferedImage(textImage.cols(), textImage.rows(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = (Graphics2D) colorImage.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            // scale the font and get metrics
            Font drawFont = new Font(fontName, Font.PLAIN, (int)Math.round(scalePt*fontSizePt));
            FontMetrics dfm = g2d.getFontMetrics(drawFont);
            final int dmaxAscent = dfm.getAscent();// fm.getMaxAscent();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setFont(drawFont);
            // overlay the characters
            for (CharacterMatch match : matches) {
                if (match.getCharNum() > 0) {
                    // use green-red color mapping to visualize the score
                    float score = (float)Math.min(1.0, Math.max(0.0, (match.score-threshold)/(1.0f-(float)threshold)));
                    float neg_score = 1.0f-score;
                    g2d.setColor(new Color(neg_score, score,  0.0f,  0.4f));
                    g2d.drawString(String.valueOf(match.getCh()), 
                            (int)Math.round(match.getX()*matchScale), (int)Math.round(match.getY()*matchScale+dmaxAscent));
                }
            }
            g2d.dispose();
            textImage = OpenCvUtils.toMat(colorImage);
        }

        // deliver the goods 
        return new Result(textImage, new OcrModel(text.toString(), numChars, overallScore));
    }
}
