/*
 * Copyright (C) 2011, 2020 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken@att.net>
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import org.openpnp.gui.JobPanel;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Board.Side;
import org.openpnp.spi.Camera;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.TravellingSalesman;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * Guides the user through the multi-placement board location operation using step by step instructions.
 * 
 * TODO: Disable the BoardLocation table while active.
 */
public class MultiPlacementBoardLocationProcess {
    private final MainFrame mainFrame;
    private final JobPanel jobPanel;
    private final Camera camera;

    private int step = -1;
    private String[] instructionsAuto = new String[] {
            I18n.gettext("<html><body>Select two or more (four or more is better) easily identifiable placements in the placements table. They should be near the corners of the board. Click Next to continue and the camera will move near one of the selected placements.</body></html>"),
            I18n.gettext("<html><body>Now, manually jog the camera's crosshairs over the center of %s. Try to be as precise as possible. Click Next to continue to the next placement.</body></html>"),
            I18n.gettext("<html><body>The board's location and rotation have been set. Click Finish to position the camera at the board's origin, or Cancel to reject the changes.</body></html>"),};

    private String[] instructionsManual = new String[] {
            I18n.gettext("<html><body>Select two or more (four or more is better) easily identifiable placements in the placements table. They should be near the corners of the board. Click Next to continue.</body></html>"),
            I18n.gettext("<html><body>Now, manually jog the camera's crosshairs over the center of %s. Try to be as precise as possible. Click Next to continue to the next placement.</body></html>"),
            I18n.gettext("<html><body>The board's location and rotation have been set. Click Finish to position the camera at the board's origin, or Cancel to reject the changes.</body></html>"),};

    private String placementId;
    private List<Placement> placements;
    private List<Location> expectedLocations;
    private List<Location> measuredLocations;
    private int nPlacements;
    private int idxPlacement = 0;
    private BoardLocation boardLocation;
    private Side boardSide;
    private Location savedBoardLocation;
    private AffineTransform savedPlacementTransform;
    private MultiPlacementBoardLocationProperties props;
    private boolean autoMove;

    public static class MultiPlacementBoardLocationProperties {
        private double scalingTolerance = 0.05; //unitless
        private double shearingTolerance = 0.05; //unitless
        protected Length boardLocationTolerance = new Length(5.0, LengthUnit.Millimeters);
        private boolean autoMoveForAllPlacements = true;
    }
    
    public MultiPlacementBoardLocationProcess(MainFrame mainFrame, JobPanel jobPanel)
            throws Exception {
        this.mainFrame = mainFrame;
        this.jobPanel = jobPanel;
        this.camera =
                MainFrame.get().getMachineControls().getSelectedTool().getHead().getDefaultCamera();
        
        placementId = "";
        expectedLocations = new ArrayList<Location>();
        measuredLocations = new ArrayList<Location>();
        
        boardLocation = jobPanel.getSelection();
        boardSide = boardLocation.getSide();
        
        //Save the current board location and transform in case it needs to be restored
        savedBoardLocation = boardLocation.getLocation();
        savedPlacementTransform = boardLocation.getPlacementTransform();
        
        // Clear the current transform so it doesn't potentially send us to the wrong spot
        // to find the placements.
        boardLocation.setPlacementTransform(null);

        props = (MultiPlacementBoardLocationProperties) Configuration.get().getMachine().
                    getProperty("MultiPlacementBoardLocationProperties");
        
        if (props == null) {
            props = new MultiPlacementBoardLocationProperties();
            Configuration.get().getMachine().
                setProperty("MultiPlacementBoardLocationProperties", props);
        }
        
        autoMove = props.autoMoveForAllPlacements;
        if (props.autoMoveForAllPlacements) {
            Logger.info("Auto move is enabled for all placements.  To disable auto move " +
                    "for the first two placements, change auto-move-for-all-placements to false in " +
                    "MultiPlacementBoardLocationProperties section of machine.xml ");
        }
        else {
            Logger.info("Auto move is disabled for the first two placements.  To enable auto move " +
                    "for all placements, change auto-move-for-all-placements to true in " +
                    "MultiPlacementBoardLocationProperties section of machine.xml ");
        }
        Logger.trace("Board location tolerance = " + props.boardLocationTolerance);
        Logger.trace("Board scaling tolerance = " + props.scalingTolerance);
        Logger.trace("Board shearing tolerance = " + props.shearingTolerance);
        
        advance();
    }

    private void advance() {
        boolean stepResult = true;
        if (step == 0) {
            stepResult = step1();
        }
        else if (step == 1) {
            stepResult = step2();
        }
        else if (step == 2) {
            stepResult = step3();
        }

        if (!stepResult) {
            return;
        }
        step++;
        if (step == 3) {
            mainFrame.hideInstructions();
        }
        else {
            String title = String.format("Set Board Location (%d / 3)", step + 1);
            mainFrame.showInstructions(title, String.format(autoMove ? instructionsAuto[step] : instructionsManual[step], placementId), true, true,
                    step == 2 ? "Finish" : "Next", cancelActionListener, proceedActionListener);
        }
    }

    private boolean step1() {
        //Get the placements selected by the user
        placements = jobPanel.getJobPlacementsPanel().getSelections();
        nPlacements = placements.size();
        if (nPlacements < 2) {
            MessageBoxes.errorBox(mainFrame, "Error", I18n.gettext("Please select at least two placements."));
            return false;
        }
        
        if (autoMove) {
            //Optimize the visit order of the placements
            placements = optimizePlacementOrder(placements);

            //Move the camera near the first placement's location
            UiUtils.submitUiMachineTask(() -> {
                Location location = Utils2D.calculateBoardPlacementLocation(boardLocation,
                        placements.get(0).getLocation() );
                MovableUtils.moveToLocationAtSafeZ(camera, location);
                MovableUtils.fireTargetedUserAction(camera);
            });
        }
        
        //Get ready for the first placement
        idxPlacement = 0;
        placementId = placements.get(0).getId();
        expectedLocations.add(placements.get(0).getLocation()
                .invert(boardSide==Side.Bottom, false, false, false));
        
        return true;
    }

    private boolean step2() {
        //Save the result of the current placement measurement
        Location measuredLocation = camera.getLocation();
        if (measuredLocation == null) {
            MessageBoxes.errorBox(mainFrame, "Error", I18n.gettext("Please position the camera."));
            return false;
        }
        measuredLocations.add(measuredLocation);
        
        //Move on the the next placement
        idxPlacement++;
        
        if (idxPlacement<nPlacements) {
            //There are more placements to be measured
            
            //If auto move is turned-off and we have measured two placements, turn auto move
            //back on for the rest of the placements.  
            if (!autoMove && (idxPlacement == 2)) {
                //Set an interim board location so that auto move can be used
                setBoardLocationAndPlacementTransform();
                
                //Clear the placement transform so we don't mix results with different transforms
                boardLocation.setPlacementTransform(null);

                //Turn-on auto move
                autoMove = true;
                
                //Remove the first two placements from the list since they have already been visited
                placements.remove(1);
                placements.remove(0);
                idxPlacement -= 2;
                nPlacements -= 2;
                
                //and then optimize the visit order of the remaining placements
                placements = optimizePlacementOrder(placements);
            }

            //Get ready for the next placement
            placementId = placements.get(idxPlacement).getId();
            expectedLocations.add(placements.get(idxPlacement).getLocation()
                    .invert(boardSide==Side.Bottom, false, false, false));
            
            if (autoMove) {
                //Move the camera near the next placement's expected location
                UiUtils.submitUiMachineTask(() -> {
                    Location location = Utils2D.calculateBoardPlacementLocation(boardLocation,
                            placements.get(idxPlacement).getLocation() );
                    MovableUtils.moveToLocationAtSafeZ(camera, location);
                    MovableUtils.fireTargetedUserAction(camera);
                });
            }
            
            //keep repeating step2 until all placements have been measured
            step--;
        } else {
            //All the placements have been visited, so set final board location and placement transform
            setBoardLocationAndPlacementTransform();
            
            //Refresh the job panel so that the new board location is visible
            jobPanel.refreshSelectedRow();           
            
            //Check the results to make sure they are valid
            double boardOffset = boardLocation.getLocation().convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(savedBoardLocation);
            Logger.info("Board origin offset distance: " + boardOffset + " mm");
           
            Utils2D.AffineInfo ai = Utils2D.affineInfo(boardLocation.getPlacementTransform());
            Logger.info("Placement affine transform: " + ai);
            
            String errString = "";
            if (Math.abs(ai.xScale-1) > props.scalingTolerance) {
                errString += "x scaling = " + String.format("%.5f", ai.xScale) + " which is outside the expected range of [" +
                        String.format("%.5f", 1-props.scalingTolerance) + ", " + String.format("%.5f", 1+props.scalingTolerance) + "], ";
            }
            if (Math.abs(ai.yScale-1) > props.scalingTolerance) {
                errString += "y scaling = " + String.format("%.5f", ai.yScale) + " which is outside the expected range of [" +
                        String.format("%.5f", 1-props.scalingTolerance) + ", " + String.format("%.5f", 1+props.scalingTolerance) + "], ";
            }
            if (Math.abs(ai.xShear) > props.shearingTolerance) {
                errString += "x shearing = " + String.format("%.5f", ai.xShear) + " which is outside the expected range of [" +
                        String.format("%.5f", -props.shearingTolerance) + ", " + String.format("%.5f", props.shearingTolerance) + "], ";
            }
            if (boardOffset > props.boardLocationTolerance.convertToUnits(LengthUnit.Millimeters).getValue()) {
                errString += "the board origin moved " + String.format("%.4f", boardOffset) +
                        "mm which is greater than the allowed amount of " +
                        String.format("%.4f", props.boardLocationTolerance.convertToUnits(LengthUnit.Millimeters).getValue()) + "mm, ";
            }
            if (errString.length() > 0) {
                errString = errString.substring(0, errString.length()-2); //strip off the last comma and space
                MessageBoxes.errorBox(mainFrame, "Error", "Results invalid because " + errString + "; double check to ensure you are " +
                        "jogging the camera to the correct placements.  Other potential remidies include " +
                        "setting the initial board X, Y, Z, and Rotation in the Boards panel; using a different set of placements; " +
                        "or changing the allowable tolerances in the MultiPlacementBoardLocationProperties section of machine.xml.");
                cancel();
                return false;
            }
            
        }
        return true;
    }

    private boolean step3() {
        UiUtils.submitUiMachineTask(() -> {
            Location location = jobPanel.getSelection().getLocation();
            MovableUtils.moveToLocationAtSafeZ(camera, location);
            MovableUtils.fireTargetedUserAction(camera);
        });

        return true;
    }

    private Location setBoardLocationAndPlacementTransform() {
        AffineTransform tx = Utils2D.deriveAffineTransform(expectedLocations, measuredLocations);
        
        //Set the transform
        boardLocation.setPlacementTransform(tx);
        
        // Compute the compensated board location
        Location origin = new Location(LengthUnit.Millimeters);
        if (boardSide == Side.Bottom) {
            origin = origin.add(boardLocation.getBoard().getDimensions().derive(null, 0., 0., 0.));
        }
        Location newBoardLocation = Utils2D.calculateBoardPlacementLocation(boardLocation, origin);
        newBoardLocation = newBoardLocation.convertToUnits(boardLocation.getLocation().getUnits());
        newBoardLocation = newBoardLocation.derive(null, null, boardLocation.getLocation().getZ(), null);

        //Set the board's new location
        boardLocation.setLocation(newBoardLocation);

        //Need to set transform again because setting the location clears the transform - shouldn't the 
        //BoardLocation.setPlacementTransform method perform the above calculations and set the location
        //itself since it already has all the needed information???
        boardLocation.setPlacementTransform(tx);
        
        return newBoardLocation;
    }

    private List<Placement> optimizePlacementOrder(List<Placement> placements) {
        // Use a traveling salesman algorithm to optimize the path to visit the placements
        TravellingSalesman<Placement> tsm = new TravellingSalesman<>(
                placements, 
                new TravellingSalesman.Locator<Placement>() { 
                    @Override
                    public Location getLocation(Placement locatable) {
                        return Utils2D.calculateBoardPlacementLocation(boardLocation, locatable.getLocation());
                    }
                }, 
                // start from current camera location
                camera.getLocation(),
                // and end at the board origin
                boardLocation.getLocation());

        // Solve it using the default heuristics.
        tsm.solve();
        
        return tsm.getTravel();
    }

    private void cancel() {
        //Restore the old settings
        boardLocation.setLocation(savedBoardLocation);
        boardLocation.setPlacementTransform(savedPlacementTransform);
        jobPanel.refreshSelectedRow();
        
        mainFrame.hideInstructions();
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
}
