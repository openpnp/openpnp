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

package org.openpnp.machine.reference.camera;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.glu.GLU;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.openpnp.Configuration;
import org.openpnp.Job;
import org.openpnp.Job.JobBoard;
import org.openpnp.LengthUnit;
import org.openpnp.Location;
import org.openpnp.Outline;
import org.openpnp.Part;
import org.openpnp.Part.FeederLocation;
import org.openpnp.Placement;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.spi.Head;
import org.openpnp.util.LengthUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jogamp.opengl.util.awt.Screenshot;


@SuppressWarnings("serial")
public class SimulatorCamera extends AbstractCamera implements Runnable, GLEventListener, ReferenceCamera {
	private static GLU glu = new GLU();
	/*
	 * At about 50mm we had .00087" x .00083" per pixel
	 * So, for 640x480 that would give us a visible area of
	 * .5568" x 0.3984"
	 * 
	 * This equates to a 16.0998 viewing angle.
	 * 
	 * Okay, so what we need is:
	 * An object focalLength (50mm) from the camera should be in focus and .022mm per pixel X and .021mm per pixel Y.
	 * Or 92x62 pixels for 0805.
	 * In other words, 
	 *  
	 * 
	 * 
	 */
	private Configuration configuration;
	private Job job;
	
	private int width, height;
	/**
	 * focalLength +/- focalRange is the distance in Z that the image is in focus.
	 */
	private double focalLengthInMm, focalRangeInMm;
	private double mmPerPixelX, mmPerPixelY;
	
	private double offsetX, offsetY, offsetZ;
	
	private double fieldOfVision;
	
	private GLPbuffer buffer;
	
	private double x, y, z;
	
	public SimulatorCamera() {
	}
	
	@Override
	public void configure(Node n) throws Exception {
		this.focalLengthInMm = Double.parseDouble(Configuration.getAttribute(n, "focalLengthInMm"));
		this.focalRangeInMm = Double.parseDouble(Configuration.getAttribute(n, "focalRangeInMm"));
		this.mmPerPixelX = Double.parseDouble(Configuration.getAttribute(n, "mmPerPixelX"));
		this.mmPerPixelY = Double.parseDouble(Configuration.getAttribute(n, "mmPerPixelY"));
		this.offsetX = Double.parseDouble(Configuration.getAttribute(n, "offsetX"));
		this.offsetY = Double.parseDouble(Configuration.getAttribute(n, "offsetY"));
		this.offsetZ = Double.parseDouble(Configuration.getAttribute(n, "offsetZ"));
		this.fieldOfVision = Double.parseDouble(Configuration.getAttribute(n, "fieldOfVision"));
		this.width = Integer.parseInt(Configuration.getAttribute(n, "width"));
		this.height = Integer.parseInt(Configuration.getAttribute(n, "height"));
		
    	GLProfile glp = GLProfile.getDefault();
    	GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
    	GLCapabilities glc = new GLCapabilities(glp);
    	buffer = factory.createGLPbuffer(null, glc, null, width, height, null);
		
		new Thread(this).start();
	}

	@Override
	public void prepareJob(Configuration configuration, Job job)
			throws Exception {
		this.configuration = configuration;
		this.job = job;
	}

	public void run() {
    	GLContext context = buffer.getContext();
    	context.makeCurrent();
    	buffer.addGLEventListener(this);
		
		while (true) {
			if (listeners.size() > 0) {
				broadcastCapture(generateFrame());
			}
			try {
				Thread.sleep(1000 / 10);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public BufferedImage capture() {
		return generateFrame();
	}
	
	private synchronized BufferedImage generateFrame() {
		if (head != null) {
			x = head.getX();
			y = head.getY();
			z = head.getZ();
		}
		else {
			x = y = z = 0;
		}
		
		x += offsetX;
		y += offsetY;
		z += offsetZ;
		
		buffer.display();
    	BufferedImage img = Screenshot.readToBufferedImage(
    			buffer.getWidth() / 2 - width / 2, 
    			buffer.getHeight() / 2 - height / 2, 
    			width, 
    			height, 
    			false);
    	
    	return img;
	}
	
    public void display(GLAutoDrawable gLDrawable) {
        final GL2 gl = gLDrawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT  | GL.GL_DEPTH_BUFFER_BIT);

        gl.glLoadIdentity();
        
        glu.gluLookAt(x, y, z * -1, x, y, -100, 0, 1, 0);
        
        // TODO this entire thing can be loaded into a display list when the job is set
        
        // draw the table
        gl.glBegin(GL2.GL_POLYGON);
        	gl.glColor3f(0.4f, 0.4f, 0.4f);
	        gl.glVertex3f(0, 0, -100);
	        gl.glVertex3f(0, 600, -100);
	        gl.glVertex3f(400, 600, -100);
	        gl.glVertex3f(400, 0, -100);
	    gl.glEnd();
	    
	    
		if (job != null && configuration != null) {
			// draw the boards
			for (JobBoard jobBoard : job.getBoards()) {
				Outline o = jobBoard.getBoard().getOutline();
				Location l = jobBoard.getLocation();
				
				l = LengthUtil.convertLocation(l, LengthUnit.Millimeters);
				
				o = LengthUtil.convertOutline(o, LengthUnit.Millimeters);
				
				gl.glPushMatrix();
					gl.glTranslated(l.getX(), l.getY(), l.getZ() * -1);
					gl.glRotated(l.getRotation(), 0, 0, -1);
					gl.glColor3f(0.183f, 0.527f, 0.070f);
					drawPolygon(gl, o);
				gl.glPopMatrix();
			}
			
			// draw the placements
			// TODO figure out a way to show which parts have been placed
			// unplaced parts will be a barely visible outline and the placed ones will be white polys
			// probably need to create model info in the Job or retain a reference to the JobProcessor 
			for (JobBoard jobBoard : job.getBoards()) {
				for (Placement placement : jobBoard.getBoard().getPlacements()) {
					Outline o = placement.getPart().getPackage().getOutline();
					Location boardLocation = jobBoard.getLocation();
					Location placementLocation = placement.getLocation();

					boardLocation = LengthUtil.convertLocation(boardLocation, LengthUnit.Millimeters);
					placementLocation = LengthUtil.convertLocation(placementLocation, LengthUnit.Millimeters);
					
					o = LengthUtil.convertOutline(o, LengthUnit.Millimeters);

					gl.glPushMatrix();
						gl.glTranslated(boardLocation.getX(), boardLocation.getY(), 0);
						gl.glRotated(boardLocation.getRotation(), 0, 0, -1);
						gl.glTranslated(placementLocation.getX(), placementLocation.getY(), boardLocation.getZ() * -1 + 1);
						gl.glRotated(placementLocation.getRotation(), 0, 0, -1);
						gl.glColor3f(1f, 1f, 1f);
						drawLines(gl, o);
					gl.glPopMatrix();
				}
			}
			
			// draw the parts in feeders
			for (Part part : configuration.getParts()) {
				for (FeederLocation fl : part.getFeederLocations()) {
					Outline o = part.getPackage().getOutline();
					Location l = fl.getLocation();
					
					l = LengthUtil.convertLocation(l, LengthUnit.Millimeters);

					o = LengthUtil.convertOutline(o, LengthUnit.Millimeters);
					
					gl.glPushMatrix();
						gl.glTranslated(l.getX(), l.getY(), l.getZ() * -1);
						gl.glRotated(l.getRotation(), 0, 0, -1);
						gl.glColor3f(1f, 0f, 0f);
						drawPolygon(gl, o);
					gl.glPopMatrix();
				}
			}
		}
    }
    
    private void drawPolygon(GL2 gl, Outline o) {
        gl.glBegin(GL2.GL_POLYGON);
        for (Point2D.Double point : o.getPoints()) {
        	gl.glVertex3d(point.getX(), point.getY(), 0);
        }
        gl.glEnd();
    }
 
    private void drawLines(GL2 gl, Outline o) {
        gl.glBegin(GL2.GL_LINE_LOOP);
        for (Point2D.Double point : o.getPoints()) {
        	gl.glVertex3d(point.getX(), point.getY(), 0);
        }
        gl.glEnd();
    }
 
    public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height) {
        GL2 gl = gLDrawable.getGL().getGL2();
        if (height <= 0) {
            height = 1;
        }

        float h = (float) width / (float) height;
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(fieldOfVision, h, 0.1, 1000);
        
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
    }
 
    public void init(GLAutoDrawable gLDrawable) {
        GL2 gl = gLDrawable.getGL().getGL2();
        gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClearDepth(1.0f);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        
        gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);					// Set Line Antialiasing
    	gl.glEnable(GL2.GL_BLEND);							// Enable Blending
    	gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);			// Type Of Blending To Use
    	
//    	gl.glEnable(GL2.GL_FOG);
        gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
        gl.glHint(GL2.GL_FOG_HINT, GL2.GL_NICEST);  /*  per pixel   */
        gl.glFogf(GL2.GL_FOG_START, 45f);
        gl.glFogf(GL2.GL_FOG_END, 100.0f);
    	float fogColor[] = new float[] {0.0f, 0.0f, 0.0f, 1.0f};    	
        gl.glFogfv (GL2.GL_FOG_COLOR, fogColor, 0);
        gl.glFogf(GL2.GL_FOG_DENSITY, 1f);
    }
    
    public void dispose(GLAutoDrawable gLDrawable) {
        // do nothing
    }
}
