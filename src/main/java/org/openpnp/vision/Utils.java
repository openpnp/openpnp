package org.openpnp.vision;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.util.*;
import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import javax.imageio.ImageIO;

import org.opencv.core.Core.*;
import org.opencv.core.*;
import javax.imageio.ImageIO;

import org.opencv.core.Core.*;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.highgui.Highgui;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Imgproc.*;
import org.opencv.utils.Converters;

import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Imgproc.*;
import org.opencv.utils.Converters.*;
import org.openpnp.model.Configuration;
import org.openpnp.model.Package;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.spi.Camera;
import com.Ostermiller.util.*;

public class Utils {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }



  public Utils() {;
	}


 private static final Logger logger = LoggerFactory.getLogger(Utils.class);
            
  private String imgPath;
  private String tplPath;
  private String path;
  
  public Double parseDouble(String s) 
  { if(s == null) return null; 
	try { return Double.valueOf(s); } catch(Exception e) { return null; }
  }
  public double parseDouble(String s,double def) {
	Double val = parseDouble(s);
	if(val==null) return def;
	try{ return Double.valueOf(s); } catch(Exception e) { return def; }
  } 
    

  public Location parseLoc(String str) {
        if(str==null) return null;
	String arr[] = StringHelper.split(str,"[");
	if(arr.length<=2) return null;
	arr = StringHelper.split(arr[1],"]");
	if(arr.length<=2) return null;
	arr = StringHelper.split(arr[0],":");
	Location loc = new Location(LengthUnit.Millimeters);
	Double x=null,y=null,c=0.;
	if(arr.length>=2) {
	  x=parseDouble(arr[0]);
	  y=parseDouble(arr[1]);
	if(arr.length> 2) {
	  c=parseDouble(arr[2],0.);
	}}
	if(x==null||y==null) return null;
	return loc.derive(x,y,0.,c);
  }

  public String callImg(String command, String argument, Part part, Mat img, Mat tpl) {

  if (path==null) {
	File file = new File(Configuration.get().getConfigurationDirectory(),"image.png");
	imgPath = file.getAbsolutePath();
	file = new File(Configuration.get().getConfigurationDirectory(),"templ.png");
	tplPath = file.getAbsolutePath();
	path = Configuration.get().getConfigurationDirectory().getAbsolutePath();
  }

  int n=0;
 
  if(argument==null) argument=new String("-h");
  if(img!=null) { n++;
	Highgui.imwrite(imgPath,img);
  	if(tmp!=null) { n++;
		Highgui.imwrite(tplPath,tpl);
  }}

  List<String> env = new ArrayList<String>();
  List<String> cmd = new ArrayList<String>() ;
  if(part!=null) {
  	cmd.add(command); cmd.add(argument); 
  	cmd.add(String.format("%d",n));
  	cmd.add("image.png");
  	cmd.add("templ.png");

  	env.add("part_id="+part.getId());
  	env.add("part_name="+part.getName());
  	env.add("part_speed="+part.getSpeed());
  	env.add("part_height="+part.getHeight().convertToUnits(LengthUnit.Millimeters).getValue());
  	if(part.getPackage() != null) 
  		env.add("part_package="+part.getPackage().getId());
  	else
  		env.add("part_package=");
	}
  
  try {
  ExecHelper execHelper = ExecHelper.exec((String[])cmd.toArray(),(String[])env.toArray(),Configuration.get().getConfigurationDirectory());
	if(execHelper.getStatus()!=0) return null;
	String err=execHelper.getError();
	if(err.length()>0) 
		logger.debug(String.format("Exec %s %s -> %s",command, argument, err));
	
	return execHelper.getOutput();
  } catch(Exception e) { return null; }
  }

}
