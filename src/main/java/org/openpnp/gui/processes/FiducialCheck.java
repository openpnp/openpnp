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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.openpnp.gui.JobPanel;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Footprint;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.model.Board.Side;
import org.openpnp.model.Placement.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.VisionProvider;
import org.openpnp.spi.VisionProvider.TemplateMatch;
import org.openpnp.util.IdentifiableList;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.Utils2D;
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
            // TODO: handle bottom
            
            // Find the fids in the board
            IdentifiableList<Placement> fiducials = getFiducials();
            
            // Find the two that are most distant from each other
            List<Placement> mostDistant = getMostDistantCouple(fiducials);
            
            Placement a = mostDistant.get(0);
            Placement b = mostDistant.get(1);
            
            Location aLoc = a.getLocation();
            Location bLoc = b.getLocation();
                    
            if (boardLocation.getSide() == Side.Bottom) {
                aLoc = aLoc.invert(true, false, false, false);
                bLoc = bLoc.invert(true, false, false, false);
            }

            // Run the fiducial check on each and get their actual locations
            Location aVisionLoc = getFiducialLocation(a);
            Location bVisionLoc = getFiducialLocation(b);
            
            // Calculate the angle and offset from the results
            Location boardLocation = Utils2D.calculateAngleAndOffset(
                    a.getLocation(), 
                    b.getLocation(), 
                    aVisionLoc,
                    bVisionLoc);
            
            // Set the new location
            Location oldBoardLocation = jobPanel.getSelectedBoardLocation().getLocation();
            oldBoardLocation = oldBoardLocation.convertToUnits(boardLocation.getUnits());
            
            boardLocation = boardLocation.derive(null, null, oldBoardLocation.getZ(), null);

            jobPanel.getSelectedBoardLocation().setLocation(boardLocation);
            jobPanel.refreshSelectedBoardRow();

            // And go there
            MovableUtils.moveToLocationAtSafeZ(camera, boardLocation, 1.0);
        }
        catch (Exception e) {
            MessageBoxes.errorBox(mainFrame, "Process Error", e);
        }
    }

    /**
     * Given a placement containing a fiducial, attempt to find the fiducial
     * using the vision system. The function first move the camera to the
     * ideal location of the fiducial based on the board location. It then
     * performs a template match against a template generated from the
     * fiducial's footprint. These steps are performed twice to "home in"
     * on the fiducial. Finally, the location is returned. If the fiducial
     * was not able to be located with any degree of certainty the function
     * returns null.
     * @param fid
     * @return
     * @throws Exception
     */
    private Location getFiducialLocation(Placement fid) throws Exception {
        mainFrame.showInstructions("Locating " + fid.getId(), null, false, false, null, null, null);
        
        // Create the template
        BufferedImage template = createTemplate(fid);
        
        // Move to where we expect to find the fid
        Location location = Utils2D.calculateBoardPlacementLocation(
                boardLocation.getLocation(), boardLocation.getSide(),
                fid.getLocation());
        MovableUtils.moveToLocationAtSafeZ(camera, location, 1.0);
        
        for (int i = 0; i < 2; i++) {
            // Wait for camera to settle
            Thread.sleep(500);
            // Perform vision operation
            location = getBestTemplateMatch(template);
            if (location == null) {
                return null;
            }
            logger.debug("fiducial located at {}", location);
            // Move to where we actually found the fid
            MovableUtils.moveToLocationAtSafeZ(camera, location, 1.0);        
        }
        
        return location;
    }
    
    /**
     * Given a List of Placements, find the two that are the most distant from
     * each other.
     * @param fiducials
     * @return
     */
    private List<Placement> getMostDistantCouple(List<Placement> fiducials) {
        if (fiducials.size() < 2) {
            return null;
        }
        Placement maxA = null, maxB = null;
        double max = 0;
        for (Placement a : fiducials) {
            for (Placement b : fiducials) {
                if (a == b) {
                    continue;
                }
                double d = Math.abs(a.getLocation().getLinearDistanceTo(b.getLocation()));
                if (d > max) {
                    maxA = a;
                    maxB = b;
                    max = d;
                }
            }
        }
        ArrayList<Placement> results = new ArrayList<Placement>();
        results.add(maxA);
        results.add(maxB);
        System.out.println(results + " "+ max);
        return results;
    }
    
    private Location getBestTemplateMatch(BufferedImage template) throws Exception {
        VisionProvider visionProvider = camera.getVisionProvider();
        
        List<TemplateMatch> matches = visionProvider.getTemplateMatches(template);
        
        if (matches.isEmpty()) {
            return null;
        }
        
        // getTemplateMatches returns results in order of score, but we're
        // more interested in the result closest to the expected location
        Collections.sort(matches, new Comparator<TemplateMatch>() {
            @Override
            public int compare(TemplateMatch o1, TemplateMatch o2) {
                double d1 = o1.location.getLinearDistanceTo(camera.getLocation()); 
                double d2 = o2.location.getLinearDistanceTo(camera.getLocation()); 
                return Double.compare(d1, d2);
            }
        });

        return matches.get(0).location;
    }
        
    /**
     * Create a template image based on a Placement's footprint. The image
     * will be scaled to match the dimensions of the current camera.
     * @param fid
     * @return
     */
    private BufferedImage createTemplate(Placement fid) {
        Footprint footprint = fid.getPart().getPackage().getFootprint();
        Shape shape = footprint.getShape();
        Location upp = camera.getUnitsPerPixel();
        
        // Determine the scaling factor to go from Outline units to
        // Camera units.
        Length l = new Length(1, footprint.getUnits());
        l = l.convertToUnits(upp.getUnits());
        double unitScale = l.getValue();

        // Create a transform to scale the Shape by
        AffineTransform tx = new AffineTransform();
        
        // First we scale by units to convert the units and then we scale
        // by the camera X and Y units per pixels to get pixel locations.
        tx.scale(unitScale, unitScale);
        tx.scale(1.0 / upp.getX(), 1.0 / upp.getY());
        
        // Transform the Shape and draw it out.
        shape = tx.createTransformedShape(shape);
        
        Rectangle2D bounds = shape.getBounds2D();
        
        double width = bounds.getWidth() + 1;
        double height = bounds.getHeight() + 1;
        BufferedImage template = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) template.getGraphics();
        
        g2d.setStroke(new BasicStroke(1f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.white);
        // center the drawing
        g2d.translate(width / 2, height / 2);
        g2d.fill(shape);
        
        g2d.dispose();
        
        return template;
    }

    private IdentifiableList<Placement> getFiducials() {
        IdentifiableList<Placement> fiducials = new IdentifiableList<Placement>();
        for (Placement placement : board.getPlacements()) {
            if (placement.getType() == Type.Fiducial
                    && placement.getSide() == boardLocation.getSide()) {
                fiducials.add(placement);
            }
        }
        return fiducials;
    }
}
