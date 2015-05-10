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

import org.openpnp.gui.JobPanel;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.util.MovableUtils;
import org.openpnp.vision.FiducialLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Select the right camera on startup and then disable the CameraPanel
 * while active. TODO: Disable the BoardLocation table while active.
 */
public class FiducialCheck implements Runnable {
    private static final Logger logger = LoggerFactory
            .getLogger(FiducialCheck.class);

    private final MainFrame mainFrame;
    private final JobPanel jobPanel;
    private final BoardLocation boardLocation;
    private final Board board;
    private final Camera camera;
    private FiducialLocator locator = new FiducialLocator();

    public FiducialCheck(MainFrame mainFrame, JobPanel jobPanel) {
        this.mainFrame = mainFrame;
        this.jobPanel = jobPanel;
        this.boardLocation = jobPanel.getSelectedBoardLocation();
        this.board = boardLocation.getBoard();
        this.camera = MainFrame.cameraPanel.getSelectedCamera();
        new Thread(this).start();
    }

    public void run() {
        try {
            Location location = locator.locateBoard(boardLocation);
            jobPanel.getSelectedBoardLocation().setLocation(location);
            jobPanel.refreshSelectedBoardRow();

            // And go there
            MovableUtils.moveToLocationAtSafeZ(camera, location, 1.0);
        }
        catch (Exception e) {
            e.printStackTrace();
            MessageBoxes.errorBox(mainFrame, "Process Error", e);
        }
    }
}
