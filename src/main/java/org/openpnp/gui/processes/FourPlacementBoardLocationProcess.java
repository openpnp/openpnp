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
 *
 * Change Log:
 * 03/10/2012 Ami: Add four points best fit algorithm.
 * - Takes the two angles of the two opposing corners (the diagonals) from the placements and compare it to the indicated values.
 * - These are the starting point for the binary search, to find the lowest error.
 * - Each iteration the mid-point angle is also taken, and all three are evaluated.
 * - Offset is re-calculated after rotation and averaged
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
public class FourPlacementBoardLocationProcess {
	private static final Logger logger = LoggerFactory.getLogger(FourPlacementBoardLocationProcess.class);
	
	private final MainFrame mainFrame;
	private final JobPanel jobPanel;
	
	private int step = -1;
	private String[] instructions = new String[] {
		"<html><body>Pick an easily identifiable placement near the TOP-LEFT corner of the board. Select it in the table below and move the camera's crosshairs to it's center location. Click Next to continue.</body></html>",
		"<html><body>Next, pick another placement on the BOTTOM-RIGHT corner of the board, select it in the table below and move the camera's crosshairs to it's center location. Click Next to continue.</body></html>",
		"<html><body>And now, pick another placement on the TOP-RIGHT corner of the board, select it in the table below and move the camera's crosshairs to it's center location. Click Next to continue.</body></html>",
		"<html><body>Last, pick another placement on the BOTTOM-LEFT corner of the board, select it in the table below and move the camera's crosshairs to it's center location. Click Next to continue.</body></html>",
		"<html><body>The board's location and rotation has been set. Click Finish to position the camera at the board's origin, or Cancel to quit.</body></html>",
	};
	
	private Placement placementA, placementB, placementC, placementD;
	private Location placementLocationA, placementLocationB, placementLocationC, placementLocationD;
	
	public FourPlacementBoardLocationProcess(MainFrame mainFrame, JobPanel jobPanel) {
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
		placementLocationA = MainFrame.cameraPanel.getSelectedCameraLocation();
		if (placementLocationA == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please position the camera.");
			return false;
		}
		placementA = jobPanel.getJobPlacementsPanel().getSelection();
		if (placementA == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please select a placement.");
			return false;
		}
		return true;
	}
	
	
	
	private boolean step2() {
		placementLocationB = MainFrame.cameraPanel.getSelectedCameraLocation();
		if (placementLocationB == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please position the camera.");
			return false;
		}
		placementB = jobPanel.getJobPlacementsPanel().getSelection();
		if (placementB == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please select a placement.");
			return false;
		}
		return true;
	}


	private boolean step3() {
		placementLocationC = MainFrame.cameraPanel.getSelectedCameraLocation();
		if (placementLocationC == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please position the camera.");
			return false;
		}
		placementC = jobPanel.getJobPlacementsPanel().getSelection();
		if (placementC == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please select a placement.");
			return false;
		}
		return true;
	}
	
	private boolean step4() {
		placementLocationD = MainFrame.cameraPanel.getSelectedCameraLocation();
		if (placementLocationD == null) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please position the camera.");
			return false;
		}
		placementD = jobPanel.getJobPlacementsPanel().getSelection();
		if (placementD == null || placementD == placementC) {
			MessageBoxes.errorBox(mainFrame, "Error", "Please select a second placement.");
			return false;
		}
		
		if ((placementA.getSide() != placementB.getSide()) || (placementC.getSide() != placementD.getSide())){
			MessageBoxes.errorBox(mainFrame, "Error", "Both placements must be on the same side of the board.");
			return false;
		}
		
		// Get the Locations we'll be using and convert to system units.
		Location boardLocationA = placementLocationA.convertToUnits(Configuration.get().getSystemUnits());
		Location placementLocationA = placementA.getLocation().convertToUnits(Configuration.get().getSystemUnits());
		Location boardLocationB = placementLocationB.convertToUnits(Configuration.get().getSystemUnits());
		Location placementLocationB = placementB.getLocation().convertToUnits(Configuration.get().getSystemUnits());

		Location boardLocationC = placementLocationC.convertToUnits(Configuration.get().getSystemUnits());
		Location placementLocationC = placementC.getLocation().convertToUnits(Configuration.get().getSystemUnits());
		Location boardLocationD = placementLocationD.convertToUnits(Configuration.get().getSystemUnits());
		Location placementLocationD = placementD.getLocation().convertToUnits(Configuration.get().getSystemUnits());

		// If the placements are on the Bottom of the board we need to invert X
		if (placementA.getSide() == Side.Bottom) {
//			boardLocationA = boardLocationA.invert(true, false, false, false);
			placementLocationA = placementLocationA.invert(true, false, false, false);
//			boardLocationB = boardLocationB.invert(true, false, false, false);
			placementLocationB = placementLocationB.invert(true, false, false, false);
		}
		if (placementC.getSide() == Side.Bottom) {
//			boardLocationA = boardLocationA.invert(true, false, false, false);
			placementLocationC = placementLocationC.invert(true, false, false, false);
//			boardLocationB = boardLocationB.invert(true, false, false, false);
			placementLocationD = placementLocationD.invert(true, false, false, false);
		}
		logger.debug(String.format("locate"));
		logger.debug(String.format("%s - %s", boardLocationA,
				placementLocationA));
		logger.debug(String.format("%s - %s", boardLocationB,
				placementLocationB));
		logger.debug(String.format("%s - %s", boardLocationC,
				placementLocationC));
		logger.debug(String.format("%s - %s", boardLocationD,
				placementLocationD));

		double x1 = placementLocationA.getX();
		double y1 = placementLocationA.getY();
		double x2 = placementLocationB.getX();
		double y2 = placementLocationB.getY();
		// Center of the placement points used for rotation
		double centerX = (x1+x2)/2;
		double centerY = (y1+y2)/2;

		// Calculate the expected angle between the two coordinates, based
		// on their locations in the placement.
		double expectedAngle = Math.atan2(y1 - y2, x1 - x2);
		expectedAngle = Math.toDegrees(expectedAngle);
		logger.debug("expectedAngle A-B " + expectedAngle);
	
		// Then calculate the actual angle between the two coordinates,
		// based on the captured values.
		x1 = boardLocationA.getX();
		y1 = boardLocationA.getY();
		x2 = boardLocationB.getX();
		y2 = boardLocationB.getY();
		double indicatedAngle = Math.atan2(y1 - y2, x1 - x2);
		indicatedAngle = Math.toDegrees(indicatedAngle);
		logger.debug("indicatedAngle A-B " + indicatedAngle);
	
		// Subtract the difference and we have the angle that the board
		// is rotated by.
		double angleAB = indicatedAngle - expectedAngle ;	// this is the rotation angle to be done
		logger.debug("angle A-B " + angleAB);

		

		// Now do the same for C-D
		x1 = placementLocationC.getX();
		y1 = placementLocationC.getY();
		x2 = placementLocationD.getX();
		y2 = placementLocationD.getY();
		centerX += (x1+x2)/2;
		centerY	+= (y1+y2)/2;
		// Calculate the expected angle between the two coordinates, based
		// on their locations in the placement.
		expectedAngle = Math.atan2(y1 - y2, x1 - x2);
		expectedAngle = Math.toDegrees(expectedAngle);
		logger.debug("expectedAngle C-D " + expectedAngle);

		// Then calculate the actual angle between the two coordinates,
		// based on the captured values.
		x1 = boardLocationC.getX();
		y1 = boardLocationC.getY();
		x2 = boardLocationD.getX();
		y2 = boardLocationD.getY();
		indicatedAngle = Math.atan2(y1 - y2, x1 - x2);
		indicatedAngle = Math.toDegrees(indicatedAngle);
		logger.debug("indicatedAngle C-D " + indicatedAngle);

		// Subtract the difference and we have the angle that the board
		// is rotated by.
		double angleCD = indicatedAngle - expectedAngle ;	// this is the rotation angle to be done
		logger.debug("angle C-D " + angleCD);
		

		Point center = new Point(centerX/2,centerY/2);	// This is the center point of the four board used for rotation

		
		double dxAB = 0, dxCD = 0, dxMP = 0;
		double dyAB = 0, dyCD = 0, dyMP = 0;


		// Now we do binary search n-times between AngleAB and AngleCD to find lowest error
		// This is up to as good as we want, I prefer for-loop than while-loop.
		for(int i = 0; i< 50;++i)
		{
		    // use angleAB to calculate the displacement necessary to get placementLocation to boardLocation.
		    // Each point will have slightly different value.
		    // Then we can tell the error resulted from using this angleAB.
		    Point A = new Point(placementLocationA.getX(),placementLocationA.getY());
		    A = Utils2D.rotateTranslateCenterPoint(A, angleAB,0,0,center);

		    Point B = new Point(placementLocationB.getX(),placementLocationB.getY());
		    B = Utils2D.rotateTranslateCenterPoint(B, angleAB,0,0,center);

		    Point C = new Point(placementLocationC.getX(),placementLocationC.getY());
		    C = Utils2D.rotateTranslateCenterPoint(C, angleAB,0,0,center);


		    Point D = new Point(placementLocationD.getX(),placementLocationD.getY());
		    D = Utils2D.rotateTranslateCenterPoint(D, angleAB,0,0,center);

		    double dA = (boardLocationA.getX() - A.getX());
		    double dB = (boardLocationB.getX() - B.getX());
		    double dC = (boardLocationC.getX() - C.getX());
		    double dD = (boardLocationD.getX() - D.getX());

		    // Take the average of the four
		    dxAB = (dA + dB + dC + dD)/4;
		    double errorAB = Math.abs(dxAB- dA) + Math.abs(dxAB- dB) + Math.abs(dxAB- dC) + Math.abs(dxAB- dD);

		     dA = (boardLocationA.getY() - A.getY());
		     dB = (boardLocationB.getY() - B.getY());
		     dC = (boardLocationC.getY() - C.getY());
		     dD = (boardLocationD.getY() - D.getY());

		    // Take the average of the four
		    dyAB = (dA + dB + dC + dD)/4;
		    errorAB += Math.abs(dyAB- dA) + Math.abs(dyAB- dB) + Math.abs(dyAB- dC) + Math.abs(dyAB- dD); // Accumulate the error


		    // Now do the same using angleCD, find the error caused by angleCD
		     A = new Point(placementLocationA.getX(),placementLocationA.getY());
		    A = Utils2D.rotateTranslateCenterPoint(A, angleCD,0,0,center);

		     B = new Point(placementLocationB.getX(),placementLocationB.getY());
		    B = Utils2D.rotateTranslateCenterPoint(B, angleCD,0,0,center);

		     C = new Point(placementLocationC.getX(),placementLocationC.getY());
		    C = Utils2D.rotateTranslateCenterPoint(C, angleCD,0,0,center);

		     D = new Point(placementLocationD.getX(),placementLocationD.getY());
		    D = Utils2D.rotateTranslateCenterPoint(D, angleCD,0,0,center);

		     dA = (boardLocationA.getX() - A.getX());
		     dB = (boardLocationB.getX() - B.getX());
		     dC = (boardLocationC.getX() - C.getX());
		     dD = (boardLocationD.getX() - D.getX());

		    // Take the average of the four
		    dxCD = (dA + dB + dC + dD)/4;
		    double errorCD = Math.abs(dxCD- dA) + Math.abs(dxCD- dB) + Math.abs(dxCD- dC) + Math.abs(dxCD- dD);

		     dA = (boardLocationA.getY() - A.getY());
		     dB = (boardLocationB.getY() - B.getY());
		     dC = (boardLocationC.getY() - C.getY());
		     dD = (boardLocationD.getY() - D.getY());

		    // Take the average of the four
		    dyCD = (dA + dB + dC + dD)/4;
		    errorCD += Math.abs(dyCD- dA) + Math.abs(dyCD- dB) + Math.abs(dyCD- dC) + Math.abs(dyCD- dD); // Accumulate the error


		    // Now take the mid-point between the two angles, 
		    // and do the same math
		    double angleMP = (angleAB + angleCD)/2;	// MP = mid-point angle between angleAB and angleCD
		    double deltaAngle = Math.abs(angleAB - angleCD);

		    A = new Point(placementLocationA.getX(),placementLocationA.getY());
		    A = Utils2D.rotateTranslateCenterPoint(A, angleMP,0,0,center);

		     B = new Point(placementLocationB.getX(),placementLocationB.getY());
		    B = Utils2D.rotateTranslateCenterPoint(B, angleMP,0,0,center);

		     C = new Point(placementLocationC.getX(),placementLocationC.getY());
		    C = Utils2D.rotateTranslateCenterPoint(C, angleMP,0,0,center);

		     D = new Point(placementLocationD.getX(),placementLocationD.getY());
		    D = Utils2D.rotateTranslateCenterPoint(D, angleMP,0,0,center);

		     dA = (boardLocationA.getX() - A.getX());
		     dB = (boardLocationB.getX() - B.getX());
		     dC = (boardLocationC.getX() - C.getX());
		     dD = (boardLocationD.getX() - D.getX());

		    // Take the average of the four
		     dxMP = (dA + dB + dC + dD)/4;
		     double errorMP = Math.abs(dxMP- dA) + Math.abs(dxMP- dB) + Math.abs(dxMP- dC) + Math.abs(dxMP- dD);

		     dA = (boardLocationA.getY() - A.getY());
		     dB = (boardLocationB.getY() - B.getY());
		     dC = (boardLocationC.getY() - C.getY());
		     dD = (boardLocationD.getY() - D.getY());

		    // Take the average of the four
		    dyMP = (dA + dB + dC + dD)/4;
		    errorMP += Math.abs(dyMP- dA) + Math.abs(dyMP- dB) + Math.abs(dyMP- dC) + Math.abs(dyMP- dD); // Accumulate the error


		    // This is gradient descend searching for local minima (hopefully) between angleAB and angle CD
		    
		    logger.debug(String.format("Error AB=%g vs MP=%g vs CD=%g ", errorAB, errorMP, errorCD));
		    if(errorAB < errorCD)
		    {
			if(errorMP > errorAB)	// ok, so no local minima between AB and CD, let's search beyond AB
			{
			    angleMP = angleAB; // use as temporary, this is MP of the previous cycle

			    // Beyond means both ways
			    if(angleAB > angleCD)
				angleAB += deltaAngle;
			    else
				angleAB -= deltaAngle;
			    angleCD = angleMP;
			}
			else
			{
			    // Local minima is for sure between AB and CD,
			    // best bet it's between MP and AB
			    // otherwise next step it will look on the other side (angleCD + deltaAngle)
			    angleCD = angleMP;
			}
		    }
		    else
		    {
			if(errorMP > errorCD) // ok so no local minima between AB and CD, let's search beyond CD
			{
			    angleMP = angleCD; // use as temporary, this is MP of the previous cycle

			    // Beyond means both ways
			    if(angleCD > angleAB)
				angleCD += deltaAngle;
			    else
				angleCD -= deltaAngle;
			    angleAB = angleCD;
			}
			else
			{
			    // local minima is between AB and CD,
			    // best bet it's between MP and CD, so set AB to the mid point (MP)
			    // otherwise next step it will look on the other side (angleCD + deltaAngle)
			    angleAB = angleMP;
			}
		    }
		    
		}

		double angle = (angleAB + angleCD)/2;	// take the average
		logger.debug("angle final " + angle);
		// Take the average of the four 
		//double dx = (dxAB + dxCD)/2;
		//double dy = (dyAB + dyCD)/2;
		double dx = dxMP;
		double dy = dyMP;

		
		logger.debug(String.format("dx %f, dy %f", dx, dy));
		Location boardLocation = new Location(Configuration.get()
				.getSystemUnits(), dx, dy, 0, angle ); 
		
		Location oldBoardLocation = jobPanel.getSelectedBoardLocation().getLocation();
		oldBoardLocation = oldBoardLocation.convertToUnits(boardLocation.getUnits());
		
        boardLocation = boardLocation.derive(null, null, oldBoardLocation.getZ(), null);

		jobPanel.getSelectedBoardLocation().setLocation(boardLocation);
		// TODO: Set Board center point when center points are finished.
//		jobPanel.getSelectedBoardLocation().setCenter(center);
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
