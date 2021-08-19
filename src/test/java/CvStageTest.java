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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.Icon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openpnp.CameraListener;
import org.openpnp.gui.support.Wizard;
import org.openpnp.model.Area;
import org.openpnp.model.AreaUnit;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.FocusProvider;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.spi.VisionProvider;
import org.openpnp.spi.base.AbstractHeadMountable;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;

import com.google.common.io.Files;

import static org.junit.jupiter.api.Assertions.*;


public class CvStageTest {
	
//	@BeforeEach
//	public void before() throws Exception {
//		/**
//		 * Create a new config directory and load the default configuration.
//		 */
//		File workingDirectory = Files.createTempDir();
//		workingDirectory = new File(workingDirectory, ".openpnp");
//		System.out.println("Configuration directory: " + workingDirectory);
//		Configuration.initialize(workingDirectory);
//		Configuration.get().load();
//
//	}
	 
	/**
	 * <pre>
     * Parameter Type(s)            Acceptable Pipeline Property Type(s)
     * -----------------------      --------------------------------------------------------
     * boolean, Boolean         <-  Boolean
     * double, Double           <-  Double, Integer, Long, Area, Length
     * int, Integer             <-  Double, Integer, Long, Area, Length
     * long, Long               <-  Double, Integer, Long, Area, Length
     * String                   <-  String
     * org.opencv.core.Point    <-  org.opencv.core.Point, org.openpnp.model.Point, Location 
     * org.openpnp.model.Point  <-  org.opencv.core.Point, org.openpnp.model.Point, Location
     * </pre>
	 */
    @Test
    public void testPipelinePropertyOverrides() throws Exception {
        Camera camera = new TestCamera();
        
        String propName = "propName";
        
        //To Boolean conversion
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            boolean originalParameter = true;
            boolean overridingParameter = false;
            boolean overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(overridingParameter, overriddenParameter);
        }

        //To String conversion
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            String originalParameter = "true";
            String overridingParameter = "false";
            String overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(overridingParameter, overriddenParameter);
        }

        //To Double conversions
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Double originalParameter = 39.5;
            Double overridingParameter = -87.4;
            Double overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter, overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Double originalParameter = 39.5;
            Integer overridingParameter = -87;
            Double overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.doubleValue(), overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Double originalParameter = 39.5;
            Long overridingParameter = -87L;
            Double overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.doubleValue(), overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Double originalParameter = 39.5;
            Area overridingParameter = new Area(-87.4, AreaUnit.SquareMillimeters);
            Double overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.getValue(), overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Double originalParameter = 39.5;
            Length overridingParameter = new Length(-87.4, LengthUnit.Millimeters);
            Double overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.getValue(), overriddenParameter);
        }

        //To Integer conversions
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Integer originalParameter = 39;
            Double overridingParameter = -87.4;
            Integer overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(((Long) Math.round(overridingParameter)).intValue(), overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Integer originalParameter = 39;
            Integer overridingParameter = -87;
            Integer overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter, overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Integer originalParameter = 39;
            Long overridingParameter = -87L;
            Integer overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.intValue(), overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Integer originalParameter = 39;
            Area overridingParameter = new Area(-87.4, AreaUnit.SquareMillimeters);
            Integer overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(((Long) Math.round(overridingParameter.getValue())).intValue(), overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Integer originalParameter = 39;
            Length overridingParameter = new Length(-87.4, LengthUnit.Millimeters);
            Integer overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(((Long) Math.round(overridingParameter.getValue())).intValue(), overriddenParameter);
        }

        //To Long conversions
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Long originalParameter = 39L;
            Double overridingParameter = -87.4;
            Long overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(Math.round(overridingParameter), overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Long originalParameter = 39L;
            Integer overridingParameter = -87;
            Long overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter.longValue(), overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Long originalParameter = 39L;
            Long overridingParameter = -87L;
            Long overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(overridingParameter, overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Long originalParameter = 39L;
            Area overridingParameter = new Area(-87.4, AreaUnit.SquareMillimeters);
            Long overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(Math.round(overridingParameter.getValue()), overriddenParameter);
        }

        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Long originalParameter = 39L;
            Length overridingParameter = new Length(-87.4, LengthUnit.Millimeters);
            Long overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, Double.class, Integer.class, Long.class, Area.class, Length.class);
            assertEquals(Math.round(overridingParameter.getValue()), overriddenParameter);
        }

        //Conversions to org.opencv.core.Point
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            org.opencv.core.Point originalParameter = new org.opencv.core.Point(39.5, -87.4);
            org.opencv.core.Point overridingParameter = new org.opencv.core.Point(-47.8, 73.1);
            org.opencv.core.Point overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(overridingParameter, overriddenParameter);
        }
        
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            org.opencv.core.Point originalParameter = new org.opencv.core.Point(39.5, -87.4);
            org.openpnp.model.Point overridingParameter = new org.openpnp.model.Point(-47.8, 73.1);
            org.opencv.core.Point overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(overridingParameter.toOpencv(), overriddenParameter);
        }
        
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            org.opencv.core.Point originalParameter = new org.opencv.core.Point(39.5, -87.4);
            Location overridingParameter = new Location(LengthUnit.Millimeters, -47.8, 73.1, 0, 0);
            org.opencv.core.Point overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(new org.opencv.core.Point(camera.getWidth()/2 + overridingParameter.getX(),
                    camera.getHeight()/2 - overridingParameter.getY()), overriddenParameter);
        }
        
        //Conversions to org.openpnp.model.Point
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            org.openpnp.model.Point originalParameter = new org.openpnp.model.Point(39.5, -87.4);
            org.opencv.core.Point overridingParameter = new org.opencv.core.Point(-47.8, 73.1);
            org.openpnp.model.Point overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(org.openpnp.model.Point.fromOpencv(overridingParameter), overriddenParameter);
        }
        
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            org.openpnp.model.Point originalParameter = new org.openpnp.model.Point(39.5, -87.4);
            org.openpnp.model.Point overridingParameter = new org.openpnp.model.Point(-47.8, 73.1);
            org.openpnp.model.Point overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(overridingParameter, overriddenParameter);
        }
        
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            org.openpnp.model.Point originalParameter = new org.openpnp.model.Point(39.5, -87.4);
            Location overridingParameter = new Location(LengthUnit.Millimeters, -47.8, 73.1, 0, 0);
            org.openpnp.model.Point overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName, org.opencv.core.Point.class, org.openpnp.model.Point.class, Location.class);
            assertEquals(new org.openpnp.model.Point(camera.getWidth()/2 + overridingParameter.getX(),
                    camera.getHeight()/2 - overridingParameter.getY()), overriddenParameter);
        }
        
        //To Area conversion
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Area originalParameter = new Area(39.5, AreaUnit.SquareMillimeters);
            Area overridingParameter = new Area(87.4, AreaUnit.SquareMillimeters);
            Area overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(overridingParameter, overriddenParameter);
        }

        //To Length conversion
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Length originalParameter = new Length(39.5, LengthUnit.Millimeters);
            Length overridingParameter = new Length(87.4, LengthUnit.Millimeters);
            Length overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(overridingParameter, overriddenParameter);
        }

        //To Location conversion
        {
            CvPipeline pipeline = new CvPipeline();
            pipeline.setProperty("camera", camera);
            Location originalParameter = new Location(LengthUnit.Millimeters, 39.5, 57.2, -34.5, 93.3);
            Location overridingParameter = new Location(LengthUnit.Millimeters, 87.4, -38.4, 84.2, 18.4);
            Location overriddenParameter;
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(originalParameter, overriddenParameter);
            pipeline.setProperty(propName, overridingParameter);
            overriddenParameter = CvStage.getPossiblePipelinePropertyOverride(originalParameter, 
                    pipeline, propName);
            assertEquals(overridingParameter, overriddenParameter);
        }

    }

    static class TestCamera extends AbstractHeadMountable implements Camera {
        protected Head head;

        @Override
        public String getId() {
            return null;
        }

        @Override
        public Head getHead() {
            return head;
        }

        @Override
        public void setHead(Head head) {
            this.head = head;
        }

        @Override
        public Location getLocation() {
            return new Location(LengthUnit.Millimeters, 0, 0, 0, 0);
        }

        @Override
        public Location getLocation(HeadMountable tool) {
            if (tool != null) {
                return getLocation().subtract(tool.getCameraToolCalibratedOffset(this));
            }

            return getLocation();
        }

       @Override
        public Location getCameraToolCalibratedOffset(Camera camera) {
            return new Location(camera.getUnitsPerPixel().getUnits());
        }

        @Override
        public Wizard getConfigurationWizard() {
            return null;
        }

        @Override
        public String getPropertySheetHolderTitle() {
            return null;
        }

        @Override
        public PropertySheetHolder[] getChildPropertySheetHolders() {
            return null;
        }

        @Override
        public PropertySheet[] getPropertySheets() {
            return null;
        }

        @Override
        public Action[] getPropertySheetHolderActions() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void setName(String name) {

        }

        @Override
        public Looking getLooking() {
            return null;
        }

        @Override
        public void setLooking(Looking looking) {

        }

        @Override
        public Location getUnitsPerPixel() {
            return new Location(LengthUnit.Millimeters, 1, 1, 0, 0);
        }

        @Override
        public void setUnitsPerPixel(Location unitsPerPixel) {

        }

        @Override
        public BufferedImage capture() {
            return null;
        }

        @Override
        public BufferedImage captureTransformed() {
            return null;
        }

        @Override
        public BufferedImage captureRaw() {
            return null;
        }

        @Override
        public void startContinuousCapture(CameraListener listener) {

        }

        @Override
        public void stopContinuousCapture(CameraListener listener) {

        }

        @Override
        public void setVisionProvider(VisionProvider visionProvider) {

        }

        @Override
        public VisionProvider getVisionProvider() {
            return null;
        }

        @Override
        public int getWidth() {
            return 640;
        }

        @Override
        public int getHeight() {
            return 480;
        }

        @Override
        public Icon getPropertySheetHolderIcon() {
            return null;
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public BufferedImage settleAndCapture() throws Exception {
            return null;
        }

        @Override
        public BufferedImage lightSettleAndCapture() {
            return null;
        }

        @Override
        public void actuateLightBeforeCapture(Object light) throws Exception {
        }

        @Override
        public void actuateLightAfterCapture() throws Exception {
        }

        @Override
        public Length getSafeZ() {
            return null;
        }

        @Override
        public Location getHeadOffsets() {
            return null;
        }

        @Override
        public void setHeadOffsets(Location headOffsets) {
        }

        @Override
        public void home() throws Exception {
        }

        @Override
        public Actuator getLightActuator() {
            return null;
        }

        @Override
        public void ensureCameraVisible() {
        }

        @Override
        public boolean hasNewFrame() {
            return true;
        }

        public Location getUnitsPerPixel(Length z) {
            return new Location(LengthUnit.Millimeters, 1, 1, 0, 0).derive(null, null, z.getValue(), null);
        }

        @Override
        public Length getDefaultZ() {
            return new Length(0.0, LengthUnit.Millimeters);
        }

        @Override
        public boolean isShownInMultiCameraView() {
            return false;
        }

        @Override
        public FocusProvider getFocusProvider() {
            return null;
        }
    }
}
