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
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.spi.Camera;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Guides the user through the two point board location operation using
 * step by step instructions.
 * 
 * TODO: Disable the BoardLocation table while active.
 */
public class TwoPlacementBoardLocationProcess {
	private static final Logger logger = LoggerFactory.getLogger(TwoPlacementBoardLocationProcess.class);
	
	private final MainFrame mainFrame;
	private final JobPanel jobPanel;
	private final Camera camera;
	
	private int step = -1;
	private String[] instructions = new String[] {
        "<html><body>Select an easily identifiable placement from the table below. It should be near the left edge of the board. Click Next to continue.</body></html>",          
		"<html><body>Now, line up the camera's crosshairs with the center of the selected placement. Click Next to continue.</body></html>",			
        "<html><body>Next, select a second placement from the table below. It should be near the right edge of the board. Click Next to continue.</body></html>",
		"<html><body>Finally, line up the camera's crosshairs with the center of the selected placement. Click Next to continue.</body></html>",			
		"<html><body>The board's location and rotation has been set. Click Finish to position the camera at the board's origin, or Cancel to quit.</body></html>",
	};
	
	private Placement placementA, placementB;
	private Location actualLocationA, actualLocationB;
	
	public TwoPlacementBoardLocationProcess(MainFrame mainFrame, JobPanel jobPanel) throws Exception {
		this.mainFrame = mainFrame;
		this.jobPanel = jobPanel;
		this.camera = MainFrame
				.machineControlsPanel
				.getSelectedTool()
				.getHead()
				.getDefaultCamera();
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
        placementA = jobPanel.getJobPlacementsPanel().getSelection();
        if (placementA == null) {
            MessageBoxes.errorBox(mainFrame, "Error", "Please select a placement.");
            return false;
        }
        return true;
    }
    
	private boolean step2() {
		actualLocationA = camera.getLocation();
		if (actualLocationA == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please position the camera.");
			return false;
		}
		return true;
	}
	
	private boolean step3() {
        placementB = jobPanel.getJobPlacementsPanel().getSelection();
        if (placementB == null || placementB == placementA) {
            MessageBoxes.errorBox(mainFrame, "Error", "Please select a second placement.");
            return false;
        }
        
        if (placementA.getSide() != placementB.getSide()) {
            MessageBoxes.errorBox(mainFrame, "Error", "Both placements must be on the same side of the board.");
            return false;
        }
		return true;
	}
	
	private boolean step4() {
        actualLocationB = camera.getLocation();
        if (actualLocationB == null) {
            MessageBoxes.errorBox(mainFrame, "Error", "Please position the camera.");
            return false;
        }
        
        // Calculate the angle and offset from the results
        BoardLocation boardLocation = jobPanel.getSelectedBoardLocation();
        Location idealLocationA = Utils2D.calculateBoardPlacementLocation(boardLocation, placementA.getLocation());
        Location idealLocationB = Utils2D.calculateBoardPlacementLocation(boardLocation, placementB.getLocation());
        Location location = Utils2D.calculateAngleAndOffset2(
                idealLocationA, 
                idealLocationB, 
                actualLocationA,
                actualLocationB);
        
        location = boardLocation.getLocation().addWithRotation(location);
        location = location.derive(
                null, 
                null, 
                boardLocation.getLocation().convertToUnits(location.getUnits()).getZ(), 
                null);
        
		jobPanel.getSelectedBoardLocation().setLocation(location);
		jobPanel.refreshSelectedBoardRow();
		
		return true;
	}
	
	private boolean step5() {
		UiUtils.submitUiMachineTask(() -> {
			Location location = jobPanel
					.getSelectedBoardLocation()
					.getLocation();
			MovableUtils.moveToLocationAtSafeZ(camera, location, 1.0);
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
