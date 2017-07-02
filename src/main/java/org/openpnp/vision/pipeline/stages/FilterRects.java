/*
 * Copyright (C) 2017 dzach, @ https://github.com/dzach
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opencv.core.RotatedRect;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

@Stage(category = "Image Processing",
        description = "Filter rotated rects based on given width, length and aspect ratio limits.")
public class FilterRects extends CvStage {
    @Attribute
    @Property(description = "Max width of filtered rects.")
    private double widthMax = 50.0;

    @Attribute
    @Property(description = "Min width of filtered rects.")
    private double widthMin = 25.0;

    @Attribute
    @Property(description = "Max length of filtered rects.")
    private double lengthMax = 50.0;
    @Attribute
    @Property(description = "Min length of filtered rects.")
    private double lengthMin = 25.0;

    @Attribute
    @Property(
            description = "Max aspect ratio for selecting rects, used if one or both of width and length are 0. If both width and length are 0, then any rect with size satisfying the aspect ratio limits will pass through.")
    private double aspectRatioMax = 0.0;

    @Attribute
    @Property(
            description = "Min aspect ratio for selecting rects, used if one or both of width and length are 0. If both width and length are 0, then any rect with size satisfying the aspect ratio limits will pass through.")
    private double aspectRatioMin = 0.0;

    @Attribute(required = false)
    @Property(description = "Enable logging of rect data.")
    private boolean enableLogging = false;

    @Attribute
    @Property(description = "Previous pipeline stage that outputs rotated rects.")
    private String rotatedRectsStageName = null;

    public double getWidthMax() {
        return widthMax;
    }

    public void setWidthMax(double widthMax) {
        this.widthMax = Math.abs(widthMax);
    }

    public double getWidthMin() {
        return widthMin;
    }

    public void setWidthMin(double widthMin) {
        this.widthMin = Math.abs(widthMin);
    }

    public double getLengthMax() {
        return lengthMax;
    }

    public void setLengthMax(double lengthMax) {
        this.lengthMax = Math.abs(lengthMax);
    }

    public double getLengthMin() {
        return lengthMin;
    }

    public void setLengthMin(double lengthMin) {
        this.lengthMin = Math.abs(lengthMin);
    }

    public double getAspectRatioMax() {
        return aspectRatioMax;
    }

    public void setAspectRatioMax(double aspectRatioMax) {
        this.aspectRatioMax = Math.abs(aspectRatioMax);
    }

    public double getAspectRatioMin() {
        return aspectRatioMin;
    }

    public void setAspectRatioMin(double aspectRatioMin) {
        this.aspectRatioMin = Math.abs(aspectRatioMin);
    }

    public boolean getEnableLogging() {
        return enableLogging;
    }

    public void setEnableLogging(boolean enableLogging) {
        this.enableLogging = enableLogging;
    }

    public String getRotatedRectsStageName() {
        return rotatedRectsStageName;
    }

    public void setRotatedRectsStageName(String rotatedRectsStageName) {
        this.rotatedRectsStageName = rotatedRectsStageName;
    }

    private static String[] verdict = {" ", "+"};

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (rotatedRectsStageName == null) {
            return null;
        }

        Result result = pipeline.getResult(rotatedRectsStageName);
        if (result == null || result.model == null) {
            return null;
        }

        List<RotatedRect> rects;
        if (result.model instanceof RotatedRect) {
            rects = Collections.singletonList((RotatedRect) result.model);
        }
        else {
            rects = (List<RotatedRect>) result.model;
        }

        List<RotatedRect> results = new ArrayList<RotatedRect>();

        double wmax, wmin, lmax, lmin, armax, armin, rw, rl, rar;
        int sizeType = 0;
        String pass;
        boolean wok, lok, arok;

        // only positive dimensions
        wmax = Math.max(widthMax, widthMin);
        wmin = Math.min(widthMax, widthMin);
        lmax = Math.max(lengthMax, lengthMin);
        lmin = Math.min(lengthMax, lengthMin);
        armax = Math.max(aspectRatioMax, aspectRatioMin);
        armin = Math.min(aspectRatioMax, aspectRatioMin);

        // check input parameters
        if (armin == 0 && (wmax == 0 || lmax == 0)) {
            // cannot accept this as there will be divisions by 0
            throw new Exception(
                    "If width or length limits are 0, then both aspectRatioMax and aspepctRatioMin must be non zero.");
        }
        if (armax != 0 && (wmax == 0 && lmax == 0)) {
            sizeType = 2;
        }
        else if (lmax == 0) {
            // derive length from aspect ratio
            lmax = wmax / armin;
            lmin = wmin / armax;
            sizeType = 1;
        }
        else if (wmax == 0) {
            wmax = lmax * armax;
            wmin = lmin * armin;
            sizeType = 1;
        }
        else {
            sizeType = 3;
        }
        if (enableLogging) {
            if (sizeType == 1) {
                Logger.info("Deriving " + (wmax == 0 ? "width" : "length")
                        + " limits based on aspect ratio limits.");
            }
            else if (sizeType == 2) {
                Logger.info("Pass any size rect within aspect ratio limits.");
            }
        }

        // iterate over input rects
        for (RotatedRect rect : rects) {

            rw = Math.max(rect.size.width, rect.size.height);
            rl = Math.min(rect.size.width, rect.size.height);
            // ignore aspect ratio = NaN or infinity
            rar = rw / rl;
            if (Double.isNaN(rar) || Double.isInfinite(rar)) {
                continue;
            }

            if (sizeType == 2) {
                // Any size:
                // Select rects based only on the aspect ratio of each input rectangle
                lmin = rl;
                lmax = rl;
                wmax = rl * armax;
                wmin = rl * armin;
            }

            // check criteria
            if (rw >= wmin && rw <= wmax) {
                wok = true;
            }
            else {
                wok = false;
            }
            if (rl >= lmin && rl <= lmax) {
                lok = true;
            }
            else {
                lok = false;
            }
            if (rar >= armin && rar <= armax) {
                arok = true;
            }
            else {
                arok = false;
            }
            // if sizeTYpe == 3 we don't care about aspect ratio
            // because specified width and length limits take precedence over aspect ratio limits
            if (wok && lok && (sizeType == 3 || arok)) {
                // rect passes criteria
                results.add(rect);
                pass = verdict[1];
            }
            else {
                pass = verdict[0];
            }
            if (enableLogging) {
                Logger.info("{} rect: xy={}, {} area={} angle= {}°", pass,
                        String.format("%4d", (int) rect.center.x),
                        String.format("%-4d", (int) rect.center.y),
                        String.format("%6d", (int) (rw * rl)),
                        String.format("%4d", (int) rect.angle));
                Logger.info("    {} widths : {} <= {} <= {}, mean= {}", (wok ? " " : "!"),
                        String.format("%.3f", wmin), String.format("%.3f", rw),
                        String.format("%.3f", wmax), String.format("%.3f", (wmin + wmax) / 2.0));
                Logger.info("    {} lengths: {} <= {} <= {}, mean= {}", (lok ? " " : "!"),
                        String.format("%.3f", lmin), String.format("%.3f", rl),
                        String.format("%.3f", lmax), String.format("%.3f", (lmin + lmax) / 2.0));
                Logger.info("    {} aspectR: {} <= {} <= {}, mean= {}",
                        (sizeType == 3 || arok ? " " : "!"), String.format("%.3f", armin),
                        String.format("%.3f", rar), String.format("%.3f", armax),
                        String.format("%.3f", (armin + armax) / 2.0));
            }
        }
        if (enableLogging) {
            Logger.info("Total detected rects: {}", results.size());
        }
        // deside what type to return
        if (results.size() == 0) {
            return null;
        }
        if (result.model instanceof RotatedRect) {
            return new Result(null, results.get(0));
        }
        return new Result(null, results);
    }
}
