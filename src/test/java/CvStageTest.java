/*
 * Copyright (C) 2021 Tony Luken <tonyluken@att.net>
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.openpnp.model.Area;
import org.openpnp.model.AreaUnit;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

import com.google.common.io.Files;


public class CvStageTest {
	class TestStage extends CvStage {
        @Override
        public Result process(CvPipeline pipeline) throws Exception {
            return null;
        }
	}
	/**
	 * Tests the following pipeline overrides with conversion:
	 * <pre>
     * Parameter Type               Overriding Pipeline Property Type(s)
     * -----------------------      --------------------------------------------------------
     * Boolean                  <-  Boolean
     * Double                   <-  Double, Integer, Long, Area, Length
     * Integer                  <-  Double, Integer, Long, Area, Length
     * Long                     <-  Double, Integer, Long, Area, Length
     * String                   <-  String
     * org.opencv.core.Point    <-  org.opencv.core.Point, org.openpnp.model.Point, Location 
     * org.openpnp.model.Point  <-  org.opencv.core.Point, org.openpnp.model.Point, Location
     * Area                     <-  Area
     * Length                   <-  Length
     * Location                 <-  Location
     * </pre>
	 */
    @Test
    public void testPipelinePropertyOverrides() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        Camera camera = new VisionUtilsTest.TestCamera();
        
        String propName = "propName";
        
        //To Boolean conversion
        {
            //From Boolean
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            boolean originalParameter = true;
            boolean overridingParameter = false;
            boolean overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(overridingParameter, overriddenParameter);
        }

        //To String conversion
        {
            //From String
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            String originalParameter = "true";
            String overridingParameter = "false";
            String overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(overridingParameter, overriddenParameter);
        }

        //To Double conversions
        {
            //From Double
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Double originalParameter = 39.5;
            Double overridingParameter = -87.4;
            Double overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter, overriddenParameter);
        }

        {
            //From Integer
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Double originalParameter = 39.5;
            Integer overridingParameter = -87;
            Double overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.doubleValue(), overriddenParameter);
        }

        {
            //From Long
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Double originalParameter = 39.5;
            Long overridingParameter = -87L;
            Double overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.doubleValue(), overriddenParameter);
        }

        {
            //From Area
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Double originalParameter = 39.5;
            Area overridingParameter = new Area(-87.4, AreaUnit.SquareMillimeters);
            Double overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.getValue(), overriddenParameter);
        }

        {
            //From Length
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Double originalParameter = 39.5;
            Length overridingParameter = new Length(-87.4, LengthUnit.Millimeters);
            Double overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.getValue(), overriddenParameter);
        }

        //To Integer conversions
        {
            //From Double
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Integer originalParameter = 39;
            Double overridingParameter = -87.4;
            Integer overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(((Long) Math.round(overridingParameter)).intValue(), overriddenParameter);
        }

        {
            //From Integer
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Integer originalParameter = 39;
            Integer overridingParameter = -87;
            Integer overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter, overriddenParameter);
        }

        {
            //From Long
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Integer originalParameter = 39;
            Long overridingParameter = -87L;
            Integer overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.intValue(), overriddenParameter);
        }

        {
            //From Area
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Integer originalParameter = 39;
            Area overridingParameter = new Area(-87.4, AreaUnit.SquareMillimeters);
            Integer overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(((Long) Math.round(overridingParameter.getValue())).intValue(), overriddenParameter);
        }

        {
            //From Length
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Integer originalParameter = 39;
            Length overridingParameter = new Length(-87.4, LengthUnit.Millimeters);
            Integer overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(((Long) Math.round(overridingParameter.getValue())).intValue(), overriddenParameter);
        }

        //To Long conversions
        {
            //From Double
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Long originalParameter = 39L;
            Double overridingParameter = -87.4;
            Long overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(Math.round(overridingParameter), overriddenParameter);
        }

        {
            //From Integer
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Long originalParameter = 39L;
            Integer overridingParameter = -87;
            Long overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.longValue(), overriddenParameter);
        }

        {
            //From Long
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Long originalParameter = 39L;
            Long overridingParameter = -87L;
            Long overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter, overriddenParameter);
        }

        {
            //From Area
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Long originalParameter = 39L;
            Area overridingParameter = new Area(-87.4, AreaUnit.SquareMillimeters);
            Long overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(Math.round(overridingParameter.getValue()), overriddenParameter);
        }

        {
            //From Length
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Long originalParameter = 39L;
            Length overridingParameter = new Length(-87.4, LengthUnit.Millimeters);
            Long overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(Math.round(overridingParameter.getValue()), overriddenParameter);
        }

        //Conversions to org.opencv.core.Point
        {
            //From org.opencv.core.Point
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            org.opencv.core.Point originalParameter = new org.opencv.core.Point(39.5, -87.4);
            org.opencv.core.Point overridingParameter = new org.opencv.core.Point(-47.8, 73.1);
            org.opencv.core.Point overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(overridingParameter, overriddenParameter);
        }
        
        {
            //From org.openpnp.model.Point
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            org.opencv.core.Point originalParameter = new org.opencv.core.Point(39.5, -87.4);
            org.openpnp.model.Point overridingParameter = new org.openpnp.model.Point(-47.8, 73.1);
            org.opencv.core.Point overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(overridingParameter.toOpencv(), overriddenParameter);
        }
        
        {
            //From Location
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            org.opencv.core.Point originalParameter = new org.opencv.core.Point(39.5, -87.4);
            Location overridingParameter = new Location(LengthUnit.Millimeters, -47.8, 73.1, 0, 0);
            org.opencv.core.Point overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(new org.opencv.core.Point(camera.getWidth()/2 + overridingParameter.getX(),
                    camera.getHeight()/2 - overridingParameter.getY()), overriddenParameter);
        }
        
        //Conversions to org.openpnp.model.Point
        {
            //From org.opencv.core.Point
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            org.openpnp.model.Point originalParameter = new org.openpnp.model.Point(39.5, -87.4);
            org.opencv.core.Point overridingParameter = new org.opencv.core.Point(-47.8, 73.1);
            org.openpnp.model.Point overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(org.openpnp.model.Point.fromOpencv(overridingParameter), overriddenParameter);
        }
        
        {
            //From org.openpnp.model.Point
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            org.openpnp.model.Point originalParameter = new org.openpnp.model.Point(39.5, -87.4);
            org.openpnp.model.Point overridingParameter = new org.openpnp.model.Point(-47.8, 73.1);
            org.openpnp.model.Point overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(overridingParameter, overriddenParameter);
        }
        
        {
            //From Location
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            org.openpnp.model.Point originalParameter = new org.openpnp.model.Point(39.5, -87.4);
            Location overridingParameter = new Location(LengthUnit.Millimeters, -47.8, 73.1, 0, 0);
            org.openpnp.model.Point overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(new org.openpnp.model.Point(camera.getWidth()/2 + overridingParameter.getX(),
                    camera.getHeight()/2 - overridingParameter.getY()), overriddenParameter);
        }
        
        //To Area conversion
        {
            //From Area
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Area originalParameter = new Area(39.5, AreaUnit.SquareMillimeters);
            Area overridingParameter = new Area(87.4, AreaUnit.SquareMillimeters);
            Area overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(overridingParameter, overriddenParameter);
        }

        //To Length conversion
        {
            //From Length
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Length originalParameter = new Length(39.5, LengthUnit.Millimeters);
            Length overridingParameter = new Length(87.4, LengthUnit.Millimeters);
            Length overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(overridingParameter, overriddenParameter);
        }

        //To Location conversion
        {
            //From Location
            CvPipeline pipeline = new CvPipeline();
            CvStage stage = new TestStage();
            pipeline.setProperty("camera", camera);
            Location originalParameter = new Location(LengthUnit.Millimeters, 39.5, 57.2, -34.5, 93.3);
            Location overridingParameter = new Location(LengthUnit.Millimeters, 87.4, -38.4, 84.2, 18.4);
            Location overriddenParameter;
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = stage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(overridingParameter, overriddenParameter);
        }

    }
}
