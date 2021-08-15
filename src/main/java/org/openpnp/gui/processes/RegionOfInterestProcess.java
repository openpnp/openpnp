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
import org.I18n.I18n;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.gui.components.reticle.Reticle;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.RegionOfInterest;
import org.openpnp.spi.Camera;
import org.openpnp.util.UiUtils;

/**
 * Guides the user through the three point region of interest operation using step by step instructions.
 */
public class RegionOfInterestProcess {
    private final MainFrame mainFrame;
    private String processTitle;
    private final Camera camera;
    private final CameraView cameraView; 

    private int step = -1;
    private String[] instructions = new String[] {
            I18n.gettext("<html><body>Click on what will become the upper left corner of your region of interest. Click again to reset and retry. Click Next to continue.</body></html>"),
            I18n.gettext("<html><body>Now, click on what will become the upper right corner of your region of interest. Click again to reset and retry. Click Next to continue.</body></html>"),
            I18n.gettext("<html><body>Next, click on what will become the lower left corner of your region of interest. Click again to reset and retry. Click Next to continue.</body></html>"),
            I18n.gettext("<html><body>Finally, click to toggle beween a rectangular and parallelogrammatic region. Click Next to continue.</body></html>"),
            I18n.gettext("<html><body>The region of interest has been defined. Click Finish to accept it, or Cancel to quit.</body></html>"),};

    private Map<Integer, Point> regionStakeout = new HashMap<>();
    private boolean rectify = false;
    private Point mouseLastPos = null;
    private int mouseClickCount = 0;

    private static final String PROCESS_RETICLE_KEY = "PROCESS_RETICLE_KEY";

    RegionOfInterest regionOfInterest = null;

    public RegionOfInterestProcess(MainFrame mainFrame, Camera camera, String processTitle)
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
                    if (step ==  0) {
                        // draw cross hairs moving with the mouse, for the moment we're assuming a 90° aligned region of interest
                        // so these help to align with content. 
                        // TODO: perhaps rotate that with the Reticle 
                        g2d.drawLine(0, mouseLastPos.y, viewPortWidth-1, mouseLastPos.y);
                        g2d.drawLine(mouseLastPos.x, viewPortHeight-1, mouseLastPos.x, 0);
                    }
                    else if (step == 1) {
                        // draw cross hairs anchored at point 0, rotating with the mouse
                        g2d.drawLine(regionStakeout.get(0).x, regionStakeout.get(0).y, mouseLastPos.x, mouseLastPos.y);
                        int dx = mouseLastPos.x - regionStakeout.get(0).x;
                        int dy = mouseLastPos.y - regionStakeout.get(0).y;
                        // draw normal on both sides
                        g2d.drawLine(regionStakeout.get(0).x-dy, regionStakeout.get(0).y+dx, regionStakeout.get(0).x+dy, regionStakeout.get(0).y-dx);
                        // draw normal on both sides
                        g2d.drawLine(mouseLastPos.x-dy, mouseLastPos.y+dx, mouseLastPos.x+dy, mouseLastPos.y-dx);
                    }
                    else if (step ==  2) {
                        // draw width of ROI and height both normalized and not 
                        int dx = regionStakeout.get(1).x - regionStakeout.get(0).x;
                        int dy = regionStakeout.get(1).y - regionStakeout.get(0).y;
                        // draw parallelogram
                        g2d.drawLine(regionStakeout.get(0).x, regionStakeout.get(0).y, regionStakeout.get(1).x, regionStakeout.get(1).y);
                        g2d.drawLine(mouseLastPos.x, mouseLastPos.y, mouseLastPos.x+dx, mouseLastPos.y+dy);
                        g2d.drawLine(mouseLastPos.x, mouseLastPos.y, regionStakeout.get(0).x, regionStakeout.get(0).y);
                        g2d.drawLine(mouseLastPos.x+dx, mouseLastPos.y+dy, regionStakeout.get(1).x, regionStakeout.get(1).y);
                        // also draw the normal as a guide
                        g2d.setColor(Color.red);
                        g2d.drawLine(regionStakeout.get(0).x-dy, regionStakeout.get(0).y+dx, regionStakeout.get(0).x+dy, regionStakeout.get(0).y-dx);
                    }
                    else if (step ==  3) {
                        rectify = (mouseClickCount % 2) == 1; 
                        drawRegion(g2d);
                    }
                    else {
                        drawRegion(g2d);
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
                    int dx1 = regionStakeout.get(1).x - regionStakeout.get(0).x;
                    int dy1 = regionStakeout.get(1).y - regionStakeout.get(0).y;
                    int dx2 = regionStakeout.get(2).x - regionStakeout.get(0).x;
                    int dy2 = regionStakeout.get(2).y - regionStakeout.get(0).y;
                    double d = Math.sqrt((double)(dx1*dx1+dy1*dy1));
                    // normal unit vector (90° clockwise, as Y points downwards) 
                    double nx = -dy1/d;
                    double ny = dx1/d;
                    // dot product is height
                    double h = nx*dx2 + ny*dy2;
                    int x2 = (int)Math.round(regionStakeout.get(0).x + h*nx);
                    int y2 = (int)Math.round(regionStakeout.get(0).y + h*ny);
                    // draw rectangle
                    g2d.setColor(Color.red);
                    g2d.drawLine(regionStakeout.get(0).x, regionStakeout.get(0).y, regionStakeout.get(1).x, regionStakeout.get(1).y);
                    g2d.drawLine(regionStakeout.get(0).x, regionStakeout.get(0).y, x2, y2);
                    g2d.drawLine(x2, y2, x2+dx1, y2+dy1);
                    g2d.drawLine(x2+dx1, y2+dy1, regionStakeout.get(1).x, regionStakeout.get(1).y);
                    g2d.drawArc(x2-20, y2-20, 40, 40, (int)Math.round(Math.atan2(dy2, -dx2)*180/Math.PI), -90);
                }
                else {
                    // draw parallelogram
                    int dx = regionStakeout.get(1).x - regionStakeout.get(0).x;
                    int dy = regionStakeout.get(1).y - regionStakeout.get(0).y;
                    g2d.drawLine(regionStakeout.get(0).x, regionStakeout.get(0).y, regionStakeout.get(1).x, regionStakeout.get(1).y);
                    g2d.drawLine(regionStakeout.get(0).x, regionStakeout.get(0).y, regionStakeout.get(2).x, regionStakeout.get(2).y);
                    g2d.drawLine(regionStakeout.get(2).x, regionStakeout.get(2).y, regionStakeout.get(2).x+dx, regionStakeout.get(2).y+dy);
                    g2d.drawLine(regionStakeout.get(2).x+dx, regionStakeout.get(2).y+dy, regionStakeout.get(1).x, regionStakeout.get(1).y);
                }
            }
        });

        advance();
    }

    private MouseAdapter locationClickedListener = new MouseAdapter() {
        @Override
        public void mousePressed(final MouseEvent e) {
            if (step < 3) {
                if (mouseClickCount % 2 == 0) { 
                    regionStakeout.put(step, e.getPoint());
                }
                else {
                    regionStakeout.remove(step);
                }
            }
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
        step++;
        if (step == 5) {
            saveResults();
            cleanup();
        }
        else {
            String title = String.format("%s (%d / 5)", processTitle, step + 1);
            mainFrame.showInstructions(title, instructions[step], true, true,
                    step == 4 ? "Finish" : "Next", cancelActionListener, proceedActionListener);
        }
    }

    private boolean saveResults() {
        // calculate the Locations from pixels
        regionOfInterest = new RegionOfInterest(
                cameraView.getCameraViewCenterOffsetsFromXy(regionStakeout.get(0).x, regionStakeout.get(0).y),
                cameraView.getCameraViewCenterOffsetsFromXy(regionStakeout.get(1).x, regionStakeout.get(1).y),
                cameraView.getCameraViewCenterOffsetsFromXy(regionStakeout.get(2).x, regionStakeout.get(2).y),
                rectify);
        UiUtils.messageBoxOnException(() -> {
            setResult(regionOfInterest);
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

    public void setResult(RegionOfInterest roi) {
    }
    public RegionOfInterest getRegionOfInterest() {
        return regionOfInterest;
    }
}
