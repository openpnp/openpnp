/*

Copyright (C) 2016 AV (CS) <phone.cri@gmail.com>
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of AV nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package org.openpnp.machine.reference.feeder;

import javax.swing.Action;
import javax.swing.AbstractAction;

import java.awt.event.ActionEvent;
import org.openpnp.gui.support.Icons;



import org.openpnp.model.Identifiable;
import org.openpnp.model.Named;
import org.openpnp.model.Board;
import org.openpnp.model.Placement;
import org.openpnp.util.IdentifiableList;
import org.openpnp.ConfigurationListener;


import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.lang.String;

import bsh.Interpreter;

import javax.imageio.ImageIO;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.LengthUnit;
import org.simpleframework.xml.*;
import org.simpleframework.xml.core.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.*;
import org.openpnp.model.Configuration;


import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceFeeder;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.spi.*;
import org.openpnp.machine.reference.feeder.
  wizards.ReferenceFeederConfigurationWizard;

import org.openpnp.spi.base.SimplePropertySheetHolder;






public class Bank extends ReferenceFeeder
{
  private final static Logger logger = LoggerFactory.getLogger (Bank.class);
  private final double cam_x = 10.0;	// mm
  private final double cam_y = 10.0;	// mm

   @Element (required = true)
  private Location offset = new Location (LengthUnit.Millimeters);

   @ElementList (required = false)
  protected IdentifiableList < Feeder > feeders = new IdentifiableList <> ();

  private ReferenceMachine machine;

   @Override public Location getPickLocation () throws Exception
  {
    return location;
  }


  //@SuppressWarnings("unused")
  //@Commit
  private void commit ()
  {
    boolean ena = enabled;
    //if(!ena) mapLoc(false);
      setEnabled (false);
      setEnabled (ena);
  }

  public Bank ()
  {
    Configuration.get ().addListener (new ConfigurationListener.Adapter () {
				      @Override
				      public void
				      configurationLoaded (Configuration
							   configuration)
				      throws Exception
				      {
				      commit ();
				      }
				      });
  }


  @Override public void setEnabled (boolean enabled)
  {
    ReferenceMachine machine =
      (ReferenceMachine) Configuration.get ().getMachine ();
    String id = getId () + ":";
    int i = -1;
    if (this.enabled != enabled) {
      //mapLoc(this.enabled=enabled);         // this stage
    for (Feeder fdr:feeders) {	// deeper stages
	if (fdr.getClass () == this.getClass ()) {	// this class
	  if (this.enabled)
	    ((Bank) fdr).commit ();
	  else {
	    enabled = ((Bank) fdr).enabled;
	    fdr.setEnabled (false);
	    ((Bank) fdr).enabled = enabled;
	  }
	}
      }
    }
    if (!enabled) {
    for (Feeder fdr:feeders) {
	machine.removeFeeder (fdr);
      }
    for (Feeder fdr:machine.getFeeders ()) {
	if (fdr.getName ().startsWith (id)) {
	  machine.removeFeeder (fdr);
	}
      }
    }

    if (enabled) {
    for (Feeder fdr:feeders) {
	if (machine.getFeeder (fdr.getId ()) != fdr)
	  try {
	  machine.addFeeder (fdr);
	  }
	catch (Exception e) {;
	}
      }
    }
  }


  @Override public void feed (Nozzle nozzle) throws Exception
  {
    if (getId ().length () == 0) {	// unused feeders
      setEnabled (true);
      feeders.clear ();
      for (Board b:Configuration.get ().getBoards ())
      {
      for (Placement p:b.getPlacements ()) {
	for (Feeder fdr:machine.getFeeders ()) {
	    if (!fdr.getPart ().toString ().equals (p.getPart ().toString ())) {
	      feeders.add (fdr);
	    }
	  }
	}
      }
      setEnabled (false);
      return;
    }
    // vars
    if (nozzle == null) {
      nozzle = machine.getDefaultHead ().getDefaultNozzle ();
    }
    Camera camera = nozzle.getHead ().getDefaultCamera ();
    double mmx = camera.getUnitsPerPixel ().getX ();
    double mmy = camera.getUnitsPerPixel ().getY ();
    boolean ena = enabled;
    // move camera
    nozzle.moveToSafeZ ();
    camera.moveTo (location.derive (null, null, Double.NaN, 0.0));
    camera.moveToSafeZ ();
    // updte feeders
    setEnabled (false);
    // changed from RTAG to barcode and to bufferedImage
    // removed visual feedback because of api problems (requested)
    int w = (int) Math.floor (mmx * cam_x + 0.5);
    int h = (int) Math.floor (mmy * cam_y + 0.5);
    BufferedImage image = camera.settleAndCapture ();
    image = camera.settleAndCapture ().getSubimage ((image.getWith () -
						     w) / 2,
						    (image.getHeight () -
						     h) / 2, w, h);
    String ids =
      new MultiFormatReader ().
      decode (new
	      BinaryBitmap (new
			    HybridBinarizer (new
					     BufferedImageLuminanceSource
					     (image)))).getText ();
    // id now contains value
    Feeder feeder = null;
    int n = 0;
    if (ids != null) {
    for (String id:ids.split ("|")) {
	logger.debug ("found feeder {} at bank {}", id, getId ());
      for (Feeder fdr:machine.getFeeders ()) {
	  if (id.equals (fdr.getId ())) {
	    feeder = fdr;
	    break;
	  }
	}
	if (feeder != null) {
	  logger.info ("found feeder {} at bank {}", feeder.getName (),
		       getName ());
	  // register feeder
	}
	else {
	  // removed change directory by request

	  Interpreter i = new Interpreter ();
	  i.set ("offset", offset);
	  i.set ("location", location);
	  i.set ("name", getName ());
	  i.set ("camera", camera);
	  i.set ("machine", machine);
	  i.set ("config", Configuration.get ());
	  i.set ("nozzle", nozzle);
	  i.set ("id", getId ());
	  if (id.startsWith ("@")) {
	    id = id.substring (1) + ".bsh";
	    i.source (id);
	  }
	  else
	    i.eval (id);
	  feeder = (Feeder) i.get ("feeder");
	  System.setProperty ("user.dir", dir);
	}
	if (feeder != null) {
	  double x = offset.getZ () * n;
	  double y = offset.getRotation () * n;
	  ((ReferenceFeeder) feeder).
	    setLocation (location.add (offset.derive (null, null, 0.0, 0.0))
			 .add (offset.derive (x, y, 0.0, 0.0))
			 .addWithRotation (((ReferenceFeeder) feeder).
					   getLocation ())
			 .subtract (((ReferenceFeeder)
				     feeder).getLocation ())
	    );
	  feeders.add (feeder);
	  feeder.setEnabled (enabled);
	}
	n++;
	feeder = null;
      }
    }
    setEnabled (ena);		// restore enabled
  }

  @Override public Wizard getConfigurationWizard () {
    return new ReferenceFeederConfigurationWizard (this);
    //return null;
  }

  @Override public String getPropertySheetHolderTitle () {
    return getClass ().getSimpleName () + " " + getName ();
  }

  @Override public PropertySheetHolder[]getChildPropertySheetHolders () {
    ArrayList < PropertySheetHolder > children = new ArrayList <> ();
    children.add (new SimplePropertySheetHolder ("feeders", feeders));
    return children.toArray (new PropertySheetHolder[] {
			     }
    );
  }


  @Override public PropertySheet[]getPropertySheets () {
    return new PropertySheet[] {
    new PropertySheetWizardAdapter (getConfigurationWizard ())};
  }

  @Override public Action[]getPropertySheetHolderActions () {
    return null;
  }


}
