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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.List;

import org.openpnp.gui.JobPanel;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.Board.Side;
import org.openpnp.spi.Camera;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.TravellingSalesman;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

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
    private String[] instructions = new String[] {
            "<html><body>Select two or more (four or more is better) easily identifiable placements from the placements table. They should be near the corners of the board. Click Next to continue and the camera will move near one of the selected placements.</body></html>",
            "<html><body>Now, manually jog the camera's crosshairs over the center of %s. Try to be as precise as possible. Click Next to continue to the next placement.</body></html>",
            "<html><body>The board's location and rotation has been set. Click Finish to position the camera at the board's origin, or Cancel to quit.</body></html>",};

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
        // to find the fiducials.
        boardLocation.setPlacementTransform(null);

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
            mainFrame.showInstructions(title, String.format(instructions[step], placementId), true, true,
                    step == 2 ? "Finish" : "Next", cancelActionListener, proceedActionListener);
        }
    }

    private boolean step1() {
        placements = jobPanel.getJobPlacementsPanel().getSelections();
        nPlacements = placements.size();
        if (nPlacements < 2) {
            MessageBoxes.errorBox(mainFrame, "Error", "Please select at least two placements.");
            return false;
        }
        
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
        
        placements = tsm.getTravel();

        idxPlacement = 0;
        placementId = placements.get(idxPlacement).getId();
        expectedLocations.add(placements.get(idxPlacement).getLocation()
                .invert(boardSide==Side.Bottom, false, false, false));
        //Move the camera near the placement's location
        UiUtils.submitUiMachineTask(() -> {
            Location location = Utils2D.calculateBoardPlacementLocation(boardLocation,
                    placements.get(idxPlacement).getLocation() );
            MovableUtils.moveToLocationAtSafeZ(camera, location);
        });
        return true;
    }

    private boolean step2() {
        Location measuredLocation = camera.getLocation();
        if (measuredLocation == null) {
            MessageBoxes.errorBox(mainFrame, "Error", "Please position the camera.");
            return false;
        }
        measuredLocations.add(measuredLocation);
        idxPlacement++;
        if (idxPlacement<nPlacements) {
            placementId = placements.get(idxPlacement).getId();
            expectedLocations.add(placements.get(idxPlacement).getLocation()
                    .invert(boardSide==Side.Bottom, false, false, false));
            //Move the camera near the placement's expected location
            UiUtils.submitUiMachineTask(() -> {
                Location location = Utils2D.calculateBoardPlacementLocation(boardLocation,
                        placements.get(idxPlacement).getLocation() );
                MovableUtils.moveToLocationAtSafeZ(camera, location);
            });
            step--;
        } else {
            //All the placements have been visited, so calculate the transform
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

            boardLocation.setLocation(newBoardLocation);

            //Need to set transform again because setting the location clears the transform - shouldn't the 
            //BoardLocation.setPlacementTransform method perform the above calculations and set the location
            //itself since it already has all the needed information???
            boardLocation.setPlacementTransform(tx);

            jobPanel.refreshSelectedRow();           

            //Check for out-of-nominal conditions - these limits probably should be user definable values
            double scalingTolerance = 0.05; //unitless
            double shearingTolerance = 0.10; //unitless
            double boardLocationTolerance = 5.0; //mm
            
            Utils2D.AffineInfo ai = Utils2D.affineInfo(tx);
            Logger.info("Placement results: " + ai);
            double boardOffset = newBoardLocation.convertToUnits(LengthUnit.Millimeters).getLinearDistanceTo(savedBoardLocation);
            Logger.info("Board origin offset distance: " + boardOffset + " mm");
            if ((Math.abs(ai.xScale-1) > scalingTolerance) || (Math.abs(ai.yScale-1) > scalingTolerance) ||
                    (Math.abs(ai.xShear) > shearingTolerance) || (boardOffset > boardLocationTolerance)) {
                MessageBoxes.errorBox(mainFrame, "Error", "Results were outside of nominal tolerance limits, please try again with a different set of placements.");
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
        });

        return true;
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
