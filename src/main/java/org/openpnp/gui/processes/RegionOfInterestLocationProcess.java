/*
 * Copyright (C) 2020 <mark@makr.zone>
 * inspired by TwoPlacementBoardLocationProcess by
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

package org.openpnp.gui.processes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.reticle.Reticle;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.RegionOfInterestLocation;
import org.openpnp.spi.Camera;
import org.openpnp.util.UiUtils;


/**
 * Guides the user through the three point region of interest operation using step by step instructions.
 */
public class RegionOfInterestLocationProcess {
    private final MainFrame mainFrame;
    private String processTitle;
    private final Camera camera;
    private final CameraView cameraView; 

    private String[] instructions = new String[] {
            "",
            "<html><body>Camera position can be offset with jog or by selecting position in the camera view. Click Next to continue.</body></html>",
            "<html><body>Click on what will become the upper left corner of your region of interest. Click again to reset and retry. Click Next to continue.</body></html>",
            "<html><body>Now, click on what will become the upper right corner of your region of interest. Click again to reset and retry. Click Next to continue.</body></html>",
            "<html><body>Next, click on what will become the lower left corner of your region of interest. Click again to reset and retry. Click Next to continue.</body></html>",
            "<html><body>Finally, click to toggle beween a rectangular and parallelogrammatic region. Click Next to continue.</body></html>",
            "<html><body>The region of interest has been defined. Click Finish to accept it, or Cancel to quit.</body></html>"};
    
    private enum ROIStep {
        Init,
        CameraOffset,
        UpperLeft,
        UpperRight,
        LowerLeft,
        SelectMode,
        Complete,
        Save
    };
    
    final List<ROIStep> stepSequence = Arrays.asList(ROIStep.values());
    

    private int stepIndex = 0;
    private ROIStep step =  ROIStep.Init;

    private Map<ROIStep, Point> regionStakeout = new HashMap<>();
    private boolean rectify = false;
    private Point mouseLastPos = null;
    private int mouseClickCount = 0;

    private static final String PROCESS_RETICLE_KEY = "PROCESS_RETICLE_KEY";

    RegionOfInterestLocation regionOfInterestLocation= null;
    

    public RegionOfInterestLocationProcess(MainFrame mainFrame, Camera camera, String processTitle )
            throws Exception {
        this.mainFrame = mainFrame;
        this.processTitle = processTitle;
        // setup the process
        this.camera = camera;
        SwingUtilities.invokeLater(() -> {
            MainFrame.get().getCameraViews().ensureCameraVisible(camera);
        });
        this.cameraView = MainFrame.get()
                .getCameraViews()
                .getCameraView(this.camera);
        this.cameraView.addMouseListener(locationClickedListener);
        this.cameraView.addMouseMotionListener(locationClickedListener);
        this.cameraView.flash();

        this.cameraView.setReticle(PROCESS_RETICLE_KEY, new Reticle() {

            @Override
            public void draw(Graphics2D g2d, LengthUnit cameraUnitsPerPixelUnits,
                    double cameraUnitsPerPixelX, double cameraUnitsPerPixelY, double viewPortCenterX,
                    double viewPortCenterY, int viewPortWidth, int viewPortHeight, double rotation) {
                if (mouseLastPos != null) {
                    g2d.setColor(Color.orange);
                    g2d.setStroke(new BasicStroke(1.0f));
                    final ROIStep step = stepSequence.get(stepIndex);
                    switch(step) {
                    case UpperLeft: {
                        // draw cross hairs moving with the mouse, for the moment we're assuming a 90° aligned region of interest
                        // so these help to align with content. 
                        // TODO: perhaps rotate that with the Reticle 
                        g2d.drawLine(0, mouseLastPos.y, viewPortWidth-1, mouseLastPos.y);
                        g2d.drawLine(mouseLastPos.x, viewPortHeight-1, mouseLastPos.x, 0);
                    }
                    break;
                    case UpperRight: {
                        // draw cross hairs anchored at point 0, rotating with the mouse
                        g2d.drawLine(regionStakeout.get(ROIStep.UpperLeft).x, regionStakeout.get(ROIStep.UpperLeft).y, mouseLastPos.x, mouseLastPos.y);
                        int dx = mouseLastPos.x - regionStakeout.get(ROIStep.UpperLeft).x;
                        int dy = mouseLastPos.y - regionStakeout.get(ROIStep.UpperLeft).y;
                        // draw normal on both sides
                        g2d.drawLine(regionStakeout.get(ROIStep.UpperLeft).x-dy, regionStakeout.get(ROIStep.UpperLeft).y+dx, regionStakeout.get(ROIStep.UpperLeft).x+dy, regionStakeout.get(ROIStep.UpperLeft).y-dx);
                        // draw normal on both sides
                        g2d.drawLine(mouseLastPos.x-dy, mouseLastPos.y+dx, mouseLastPos.x+dy, mouseLastPos.y-dx);
                    }
                    break;
                    case LowerLeft: {
                        // draw width of ROI and height both normalized and not 
                        int dx = regionStakeout.get(ROIStep.UpperRight).x - regionStakeout.get(ROIStep.UpperLeft).x;
                        int dy = regionStakeout.get(ROIStep.UpperRight).y - regionStakeout.get(ROIStep.UpperLeft).y;
                        // draw parallelogram
                        g2d.drawLine(regionStakeout.get(ROIStep.UpperLeft).x, regionStakeout.get(ROIStep.UpperLeft).y, regionStakeout.get(ROIStep.UpperRight).x, regionStakeout.get(ROIStep.UpperRight).y);
                        g2d.drawLine(mouseLastPos.x, mouseLastPos.y, mouseLastPos.x+dx, mouseLastPos.y+dy);
                        g2d.drawLine(mouseLastPos.x, mouseLastPos.y, regionStakeout.get(ROIStep.UpperLeft).x, regionStakeout.get(ROIStep.UpperLeft).y);
                        g2d.drawLine(mouseLastPos.x+dx, mouseLastPos.y+dy, regionStakeout.get(ROIStep.UpperRight).x, regionStakeout.get(ROIStep.UpperRight).y);
                        // also draw the normal as a guide
                        g2d.setColor(Color.red);
                        g2d.drawLine(regionStakeout.get(ROIStep.UpperLeft).x-dy, regionStakeout.get(ROIStep.UpperLeft).y+dx, regionStakeout.get(ROIStep.UpperLeft).x+dy, regionStakeout.get(ROIStep.UpperLeft).y-dx);
                    }
                    break;
                    case SelectMode :{
                        rectify = (mouseClickCount % 2) == 1; 
                        drawRegion(g2d);
                    }
                    break;
                    default: 
                        drawRegion(g2d);
                        break;
                    }
                    
                }
                g2d.setColor(Color.yellow);
                g2d.setStroke(new BasicStroke(2));
                for (Point p : regionStakeout.values()) {
                    g2d.drawOval(p.x-3, p.y-3, 6, 6);
                }
            }

            protected void drawRegion(Graphics2D g2d) {
                if (rectify) {
                    // calculate rectified region
                    int dx1 = regionStakeout.get(ROIStep.UpperRight).x - regionStakeout.get(ROIStep.UpperLeft).x;
                    int dy1 = regionStakeout.get(ROIStep.UpperRight).y - regionStakeout.get(ROIStep.UpperLeft).y;
                    int dx2 = regionStakeout.get(ROIStep.LowerLeft).x - regionStakeout.get(ROIStep.UpperLeft).x;
                    int dy2 = regionStakeout.get(ROIStep.LowerLeft).y - regionStakeout.get(ROIStep.UpperLeft).y;
                    double d = Math.sqrt((double)(dx1*dx1+dy1*dy1));
                    // normal unit vector (90° clockwise, as Y points downwards) 
                    double nx = -dy1/d;
                    double ny = dx1/d;
                    // dot product is height
                    double h = nx*dx2 + ny*dy2;
                    int x2 = (int)Math.round(regionStakeout.get(ROIStep.UpperLeft).x + h*nx);
                    int y2 = (int)Math.round(regionStakeout.get(ROIStep.UpperLeft).y + h*ny);
                    // draw rectangle
                    g2d.setColor(Color.red);
                    g2d.drawLine(regionStakeout.get(ROIStep.UpperLeft).x, regionStakeout.get(ROIStep.UpperLeft).y, regionStakeout.get(ROIStep.UpperRight).x, regionStakeout.get(ROIStep.UpperRight).y);
                    g2d.drawLine(regionStakeout.get(ROIStep.UpperLeft).x, regionStakeout.get(ROIStep.UpperLeft).y, x2, y2);
                    g2d.drawLine(x2, y2, x2+dx1, y2+dy1);
                    g2d.drawLine(x2+dx1, y2+dy1, regionStakeout.get(ROIStep.UpperRight).x, regionStakeout.get(ROIStep.UpperRight).y);
                    g2d.drawArc(x2-20, y2-20, 40, 40, (int)Math.round(Math.atan2(dy2, -dx2)*180/Math.PI), -90);
                }
                else {
                    // draw parallelogram
                    int dx = regionStakeout.get(ROIStep.UpperRight).x - regionStakeout.get(ROIStep.UpperLeft).x;
                    int dy = regionStakeout.get(ROIStep.UpperRight).y - regionStakeout.get(ROIStep.UpperLeft).y;
                    g2d.drawLine(regionStakeout.get(ROIStep.UpperLeft).x, regionStakeout.get(ROIStep.UpperLeft).y, regionStakeout.get(ROIStep.UpperRight).x, regionStakeout.get(ROIStep.UpperRight).y);
                    g2d.drawLine(regionStakeout.get(ROIStep.UpperLeft).x, regionStakeout.get(ROIStep.UpperLeft).y, regionStakeout.get(ROIStep.LowerLeft).x, regionStakeout.get(ROIStep.LowerLeft).y);
                    g2d.drawLine(regionStakeout.get(ROIStep.LowerLeft).x, regionStakeout.get(ROIStep.LowerLeft).y, regionStakeout.get(ROIStep.LowerLeft).x+dx, regionStakeout.get(ROIStep.LowerLeft).y+dy);
                    g2d.drawLine(regionStakeout.get(ROIStep.LowerLeft).x+dx, regionStakeout.get(ROIStep.LowerLeft).y+dy, regionStakeout.get(ROIStep.UpperRight).x, regionStakeout.get(ROIStep.UpperRight).y);
                }
            }
        });

        advance();
    }


    private MouseAdapter locationClickedListener = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            switch (step) {
            case UpperLeft:
            case UpperRight:
            case LowerLeft:
                if (mouseClickCount % 2 == 0) { 
                    regionStakeout.put(step, e.getPoint());
                }
                else {
                    regionStakeout.remove(step);
                }
            };
            cameraView.flash();
            mouseClickCount++;
            cameraView.repaint();
        }
        @Override
        public void mouseMoved(MouseEvent e) {
            if (mouseClickCount % 2 == 0) { 
                mouseLastPos = e.getPoint();
            }
            cameraView.repaint();
        }
    };

    private void cleanup() {
        mainFrame.hideInstructions();
        cameraView.removeMouseListener(locationClickedListener);
        cameraView.removeMouseMotionListener(locationClickedListener);
        cameraView.removeReticle(PROCESS_RETICLE_KEY);
    }

    private void advance() {
        boolean stepResult = true;
        mouseClickCount = 0;
        stepIndex += 1;
        step = stepSequence.get(stepIndex);

        if (step == ROIStep.Save) {
            saveResults();
            cleanup();
        } else {
            String title = String.format("%s (%d / %d)", processTitle, stepIndex, stepSequence.size()-1);
            
            mainFrame.showInstructions(title, instructions[stepIndex], true, true,
                    step == ROIStep.Complete ? "Finish" : "Next", cancelActionListener, proceedActionListener);
            
        }
    }

    private boolean saveResults() {
        Location location = camera.getLocation();
        // calculate the Locations from pixels
        regionOfInterestLocation = new RegionOfInterestLocation(
                cameraView.getCameraViewCenterOffsetsFromXy(regionStakeout.get(ROIStep.UpperLeft).x, regionStakeout.get(ROIStep.UpperLeft).y),
                cameraView.getCameraViewCenterOffsetsFromXy(regionStakeout.get(ROIStep.UpperRight).x, regionStakeout.get(ROIStep.UpperRight).y),
                cameraView.getCameraViewCenterOffsetsFromXy(regionStakeout.get(ROIStep.LowerLeft).x, regionStakeout.get(ROIStep.LowerLeft).y),
                rectify,
                location);
        UiUtils.messageBoxOnException(() -> {
            setResult(regionOfInterestLocation);
        });
        return true;
    }

    private void cancel() {
        cleanup();
    }

    private final ActionListener proceedActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            advance();
        }
    };

    private final ActionListener cancelActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            cancel();
        }
    };

    public void setResult(RegionOfInterestLocation roi) {
    }
    public RegionOfInterestLocation getRegionOfInterestPosition() {
        return regionOfInterestLocation;
    }
}
