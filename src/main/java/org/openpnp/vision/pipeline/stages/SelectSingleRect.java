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

import java.util.Collections;
import java.util.List;

import org.opencv.core.RotatedRect;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

@Stage(category = "Image Processing",
        description = "Selects a single rotated rectangle from a list. Main purpose convert the data type.")
public class SelectSingleRect extends CvStage {
    @Attribute
    @Property(description = "Position (0=first, 1=second, -1=last)")
    private int position = 0;

    @Attribute
    @Property(description = "Previous pipeline stage that outputs rotated rects.")
    private String rotatedRectsStageName = null;

    
    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getRotatedRectsStageName() {
        return rotatedRectsStageName;
    }

    public void setRotatedRectsStageName(String rotatedRectsStageName) {
        this.rotatedRectsStageName = rotatedRectsStageName;
    }

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

        RotatedRect results = null;
        
        if (rects !=  null && rects.size() > 0) {
            if (position == -1) {
                results = rects.get(rects.size() -1);
            } else if (position <= rects.size()) {
                results = rects.get(position);
            }
        }

        return new Result(null, results);
    }
}
