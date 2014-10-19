/*
 	Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 	
 	This file is part of OpenPnP.
 	
	OpenPnP is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenPnP is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenPnP.  If not, see <http://www.gnu.org/licenses/>.
 	
 	For more information about OpenPnP visit http://openpnp.org
*/

package org.openpnp.gui.processes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.openpnp.gui.JobPanel;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.Point;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Head;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.Utils2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guides the user through the two point board location operation using
 * step by step instructions.
 * 
 * TODO: Select the right camera on startup and then disable the CameraPanel while active.
 * TODO: Disable the BoardLocation table while active.
 */
public class TwoPlacementBoardLocationProcess {
	private static final Logger logger = LoggerFactory.getLogger(TwoPlacementBoardLocationProcess.class);
	
	private final MainFrame mainFrame;
	private final JobPanel jobPanel;
	
	private int step = -1;
	private String[] instructions = new String[] {
		"<html><body>Move the camera's crosshairs to the center of an easily identifiable placement near the top left corner of the board. Click Next to continue.</body></html>",			
		"<html><body>Now select the placement you have targetted from the table below. Click Next to continue.</body></html>",			
		"<html><body>Next, move the camera's crosshairs to the center of a second placement near the bottom right corner of the board. Click Next to continue.</body></html>",			
		"<html><body>And select the placement you have targetted from the table below. Click Next to continue.</body></html>",
		"<html><body>The board's location and rotation has been set. Click Finish to position the camera at the board's origin, or Cancel to quit.</body></html>",
	};
	
	private Placement placementA, placementB;
	private Location visionA, visionB;
	
	public TwoPlacementBoardLocationProcess(MainFrame mainFrame, JobPanel jobPanel) {
		this.mainFrame = mainFrame;
		this.jobPanel = jobPanel;
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
		else if (step == 3) {
			stepResult = step4();
		}
		else if (step == 4) {
			stepResult = step5();
		}
		if (!stepResult) {
			return;
		}
		step++;
		if (step == 5) {
			mainFrame.hideInstructions();
		}
		else {
			String title = String.format("Set Board Location (%d / 5)", step + 1);
			mainFrame.showInstructions(
				title,
				instructions[step], 
				true, 
				true, 
				step == 4 ? "Finish" : "Next",
				cancelActionListener,
				proceedActionListener);
		}
	}
	
	private boolean step1() {
		visionA = MainFrame.cameraPanel.getSelectedCameraLocation();
		if (visionA == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please position the camera.");
			return false;
		}
		return true;
	}
	
	private boolean step2() {
		placementA = jobPanel.getSelectedPlacement();
		if (placementA == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please select a placement.");
			return false;
		}
		return true;
	}
	
	private boolean step3() {
		visionB = MainFrame.cameraPanel.getSelectedCameraLocation();
		if (visionB == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please position the camera.");
			return false;
		}
		return true;
	}
	
	/**
	 * Given two placements and Locations where they have been located in
	 * machine coordinates, return the machine coordinates and angle of the
	 * origin of the two placements.
	 * @param placement1
	 * @param placement2
	 * @param vision1
	 * @param vision2
	 * @return
	 */
	private static Location calculateAngleAndOffset(Location placement1, Location placement2, Location vision1, Location vision2, boolean mirror) {
        // If the placements are on the Bottom of the board we need to invert X
        if (mirror) {
            placement1 = placement1.invert(true, false, false, false);
            placement2 = placement2.invert(true, false, false, false);
        }
        
        Point pPlacement1 = placement1.convertToUnits(Configuration.get().getSystemUnits()).getXyPoint();
        Point pPlacement2 = placement2.convertToUnits(Configuration.get().getSystemUnits()).getXyPoint();
        Point pVision1 = vision1.convertToUnits(Configuration.get().getSystemUnits()).getXyPoint();
        Point pVision2 = vision2.convertToUnits(Configuration.get().getSystemUnits()).getXyPoint();

        double angle = Math.toDegrees(Math.atan2(pVision1.y - pVision2.y, pVision1.x - pVision2.x)
                - Math.atan2(pPlacement1.y - pPlacement2.y, pPlacement1.x - pPlacement2.x));
        
        Point rotatedPlacement = Utils2D.rotatePoint(pPlacement1, angle);
        
        Point offset = Utils2D.translatePoint(pVision1, -rotatedPlacement.x, -rotatedPlacement.y);

        return new Location(Configuration.get().getSystemUnits(), offset.getX(), offset.getY(), 0, angle);
	}
	
	private boolean step4() {
		placementB = jobPanel.getSelectedPlacement();
		if (placementB == null || placementB == placementA) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please select a second placement.");
			return false;
		}
		
		if (placementA.getSide() != placementB.getSide()) {
			MessageBoxes.errorBox(mainFrame, "Error", "Both placements must be on the same side of the board.");
			return false;
		}
		
		Location boardLocation = calculateAngleAndOffset(
		        placementA.getLocation(), 
		        placementB.getLocation(), 
		        visionA,
		        visionB,
		        placementA.getSide() == Side.Bottom);
        
		Location oldBoardLocation = jobPanel.getSelectedBoardLocation().getLocation();
		oldBoardLocation = oldBoardLocation.convertToUnits(boardLocation.getUnits());
		
		boardLocation = boardLocation.derive(null, null, oldBoardLocation.getZ(), null);

		jobPanel.getSelectedBoardLocation().setLocation(boardLocation);
		jobPanel.refreshSelectedBoardRow();
		
		return true;
	}
	
	private boolean step5() {
		MainFrame.machineControlsPanel.submitMachineTask(new Runnable() {
			public void run() {
				Head head = Configuration.get().getMachine().getHeads().get(0);
				try {
					Camera camera = MainFrame.cameraPanel
							.getSelectedCamera();
					Location location = jobPanel.getSelectedBoardLocation()
							.getLocation();
					MovableUtils.moveToLocationAtSafeZ(camera, location, 1.0);
				}
				catch (Exception e) {
					MessageBoxes.errorBox(mainFrame,
							"Move Error", e);
				}
			}
		});
		
		return true;
	}
	
	private void cancel() {
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
