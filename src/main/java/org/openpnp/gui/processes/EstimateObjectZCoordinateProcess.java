/*
 * Copyright (C) 2021 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken@att.net>
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.SwingUtilities;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.CameraView;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.spi.base.AbstractCamera;

/**
 * Guides the user with step by step instructions on using a camera to estimate an object's
 * Z coordinate.
 * 
 */
public class EstimateObjectZCoordinateProcess {
    private final MainFrame mainFrame;
    private final CameraView cameraView;
    private final Camera camera;
    private final boolean fixedCamera;
    
    private java.awt.Point pixelPoint1;
    private java.awt.Point pixelPoint2;
    private Location observationLocation1;
    private Location observationLocation2;
    private String estimatedZStr = "unavailable";

    private int step = -1;
    private String[] moveableCameraInstructions = new String[] {
            "<html><body>Jog the camera so that an easily identifiable feature of the object " +
                    "(such as a sharp corner) is in the camera's field-of-view (FOV). It is " +
                    "better for the feature to be towards the edge of the FOV rather than " +
                    "centered. When ready, click the mouse on the feature to capture its " +
                    "apparent position." +
                    "</body></html>",
            "<html><body>Now jog the camera in X and/or Y so that the same feature is visible in " +
                    "another part of the camera's FOV. The farther away from the original " +
                    "position, the better.  When ready, click the mouse on the feature to " +
                    "capture its new apparent position and estimate its Z coordinate." +
                    "</body></html>",
            "<html><body>The estimated Z coordinate of the feature is <b>%s</b>. Click Cancel if " +
                    "finished or Again to perform another measurment." +
                    "</body></html>",};
    private String[] fixedCameraInstructions = new String[] {
            "<html><body>Jog the nozzle so that an easily identifiable feature of the object " +
                    "(such as a sharp corner) is in the camera's field-of-view (FOV). It is " +
                    "better for the feature to be towards the edge of the FOV rather than " +
                    "centered. When ready, click the mouse on the feature to capture its " +
                    "apparent position." +
                    "</body></html>",
            "<html><body>Now jog the nozzle in X and/or Y so that the same feature is visible in " +
                    "another part of the camera's FOV. The farther away from the original " +
                    "position, the better.  When ready, click the mouse on the feature to " +
                    "capture its new apparent position and estimate its Z coordinate." +
                    "</body></html>",
            "<html><body>The estimated Z coordinate of the feature is <b>%s</b>. Click Cancel if " +
                    "finished or Again to perform another measurment." +
                    "</body></html>",};

    public EstimateObjectZCoordinateProcess(MainFrame mainFrame, CameraView cameraView)
            throws Exception {
        this.mainFrame = mainFrame;
        this.cameraView = cameraView;
        camera = cameraView.getCamera();
        fixedCamera = camera.getHead() == null;
        
        cameraView.addMouseListener(mouseListener);
        
        mainFrame.getCameraViews().saveCurrentlySelectedViews();
        mainFrame.getCameraViews().setSelectedCamera(camera);
        
        advance();
    }

    /**
     * Advances through the steps and displays the appropriate instructions for each step
     */
    private void advance() {
        boolean stepResult = true;
        if (step == 0) {
            stepResult = step1();
        }
        else if (step == 1) {
            stepResult = step2();
        }

        if (!stepResult) {
            return;
        }
        step++;
        if (step > 2) {
            cancel();
        }
        else {
            mainFrame.showInstructions("Instructions to Estimate an Object's Z Coordinate",
                    String.format(fixedCamera ? fixedCameraInstructions[step]
                            : moveableCameraInstructions[step], estimatedZStr),
                    true, step == 2, "Again", cancelActionListener, proceedActionListener);
        }
    }

    
    /**
     * Action to take when transitioning from step 0 to step 1
     * 
     * @return true if the action was successful and the state machine should move to the next step
     */
    private boolean step1() {
        return true;
    }

    /**
     * Action to take when transitioning from step 1 to step 2
     * 
     * @return true if the action was successful and the state machine should move to the next step
     */
    private boolean step2() {
        Location deltaLocation = observationLocation2.subtract(observationLocation1);
        LengthUnit units = deltaLocation.getUnits();
        int deltaPixelsX = pixelPoint2.x - pixelPoint1.x;
        int deltaPixelsY = pixelPoint2.y - pixelPoint1.y;
        Location observedUnitsPerPixel = new Location(units,
                Math.abs(deltaLocation.getX()/deltaPixelsX),
                Math.abs(deltaLocation.getY()/deltaPixelsY),
                0.0,
                0.0);
        try {
            estimatedZStr = ((AbstractCamera) camera).
                    estimateZCoordinateOfObject(observedUnitsPerPixel).toString();
        }
        catch (Exception ex) {
             estimatedZStr = "unavailable (due to " + ex.getMessage() + ")";
        }
        return true;
    }

    /**
     * Clean-up when the process is cancelled
     */
    private void cancel() {
        cameraView.removeMouseListener(mouseListener);

        mainFrame.hideInstructions();
        
        mainFrame.getCameraViews().restoreSelectedViews();

    }

    /**
     * Process Proceed button clicks
     */
    private final ActionListener proceedActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (step >= 2) {
                step = -1;
            }
            advance();
        }
    };

    /**
     * Process Cancel button clicks
     */
    private final ActionListener cancelActionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            cancel();
        }
    };
    
    /**
     * Process mouse clicks
     */
    private MouseListener mouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.isPopupTrigger() || e.isShiftDown() || SwingUtilities.isRightMouseButton(e) ||
                    e.getClickCount() == 2) {
                // skip everything that's not a simple mouse click
                return;
            }
            if (step == 0) {
                // capture the first point
                pixelPoint1 = cameraView.getCameraViewCenterPixelsFromXy(e.getX(), e.getY());
                observationLocation1 = fixedCamera ? MainFrame.get().getMachineControls().
                        getSelectedNozzle().getLocation() : camera.getLocation();
            }
            else if (step == 1) {
                // capture the second point
                pixelPoint2 = cameraView.getCameraViewCenterPixelsFromXy(e.getX(), e.getY());
                observationLocation2 = fixedCamera ? MainFrame.get().getMachineControls().
                        getSelectedNozzle().getLocation() : camera.getLocation();
            }
            advance();
        }

    };


}
