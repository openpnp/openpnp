package org.openpnp.machine.reference.vision.roborealm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;

public class RR_API
{
  // default read and write socket timeout
  public final static int DEFAULT_TIMEOUT = 60000;

  // the port number to listen on ... needs to match that used in RR interface
  public final static int SERVER_PORTNUM = 6060;

  // indicates that the application is connected to RoboRealm Server
  boolean connected = false;

  // holds the previously read data size
  int lastDataTop = 0;

  // holds the previously read data buffer
  int lastDataSize = 0;

  // contains the read/write socket timeouts
  int timeout = DEFAULT_TIMEOUT;

  // general buffer for data manipulation and socket reading
  byte buffer[] = new byte[4096];

  // our instance of our primitive XML parser
  XML xml = new XML();

  // socket based reader and writer objects
  BufferedInputStream  bufferedReader;
  BufferedOutputStream bufferedWriter;

  // out main socket handle
  Socket handle;

  public int width=0, height=0;

  /******************************************************************************/
  /* Text string manipulation routines */
  /******************************************************************************/

  public RR_API()
  {
  }

  /*
  Generalized string replace routine used in escaping strings
  to the appropriate XML string. Java only has the character
  replace routine as apposed to string replace.
  */
  private String replace(String txt, String src, String dest)
  {
    if (txt==null) return new String("");
    int i,j;
    int len=src.length();
    StringBuffer sb=new StringBuffer(txt.length());

    j=0;
    while ((i=txt.indexOf(src,j))>=0)
    {
        sb.append(txt.substring(j,i));
        sb.append(dest);
        i+=len;
        j=i;
    }
    sb.append(txt.substring(j));

    return sb.toString();
  }

  /*
  Escapes strings to be included in XML message. This can be accomplished by a
  sequence of replace statements.
    & -> &amp;
    " -> &quote;
    < -> &lt;
    > -> &gt;
  */
  private String escape(String txt)
  {
    txt = replace(txt, "&", "&amp;");
    txt = replace(txt, "\"", "&quote;");
    txt = replace(txt, "<", "&lt;");
    txt = replace(txt, ">", "&gt;");
    return txt;
  }

  /******************************************************************************/
  /* Socket Routines */
  /******************************************************************************/

  /* Initiates a socket connection to the RoboRealm server */
  public boolean connect(String hostname, int port)
  {
    connected=false;

    try
    {
      handle = new Socket(hostname, port);

      handle.setSoTimeout(timeout);

      bufferedReader = new BufferedInputStream(handle.getInputStream());
      bufferedWriter = new BufferedOutputStream(handle.getOutputStream());
    }
    catch (IOException e2)
    {
      //Unable to open connection to RoboRealm port 6060
      return false;
    }

    connected=true;

    return true;
  }

  /* close the socket handle */
  public void disconnect()
  {
    try
    {
      if (connected)
        handle.close();
    }
    catch (IOException e)
    {
    }
  }

  // cause the roborealm application to close
  public boolean close()
  {
    if (!connected) return false;

    if (send("<request><close/></request>"))
    {
      // read in variable length
      String buffer;
      if ((buffer = readMessage())!=null)
      {
        return buffer.equals("<response>ok</response>");
      }
    }

    return false;
  }

  // sends a String over the socket port to RoboRealm
  private boolean send(String txt)
  {
    try
    {
      bufferedWriter.write(txt.getBytes(), 0, txt.length());
      bufferedWriter.flush();
    }
    catch (IOException e)
    {
      return false;
    }
    return true;
  }

  /*
  Buffered socket image read. Since we don't know how much data was read from a
  previous socket operation we have to add in any previously read information
  that may still be in our buffer. We detect the end of XML messages by the
  </response> tag but this may require reading in part of the image data that
  follows a message. Thus when reading the image data we have to move previously
  read data to the front of the buffer and continuing reading in the
  complete image size from that point.
  */

  public int readImageData(byte pixels[], int len)
  {
    int num;

    // check if we have any information left from the previous read
    num = lastDataSize-lastDataTop;
    if (num>len)
    {
      System.arraycopy(pixels, lastDataTop, buffer, 0, len);
      lastDataTop+=num;
      return num;
    }
    System.arraycopy(pixels, lastDataTop, buffer, 0, num);
    len-=num;
    lastDataSize=lastDataTop=0;

    // then keep reading until we're read in the entire image length
    do
    {
      int res;
      try
      {
        res = bufferedReader.read(pixels, num, len);
      }
      catch (IOException e)
      {
        return 0;
      }

      if (res<0)
      {
        lastDataSize=lastDataTop=0;
        return -1;
      }
      num+=res;
      len-=res;
    }
    while (len>0);

    return num;
  }

  /* If an image is too large for the provided buffer the rest of the data needs
  to be skipped so we can continue to interact with the XML API. This routine
  will remove that additional data from the socket*/
  public int skipData(int len)
  {
    int num;

    // check if we have any information left from the previous read
    num = lastDataSize-lastDataTop;
    if (num>len)
    {
      lastDataTop+=num;
      return num;
    }
    len-=num;
    lastDataSize=lastDataTop=0;

    try
    {
      bufferedReader.skip(len);
    }
    catch (IOException e)
    {
      return 0;
    }

    return num+len;
  }

  /* Read's in an XML message from the RoboRealm Server. The message is always
  delimited by a </response> tag. We need to keep reading in information until
  this tag is seen. Sometimes this will accidentally read more than needed
  into the buffer such as when the message is followed by image data. We
  need to keep this information for the next readImage call.*/
  private String readMessage()
  {
    int num=0;
    byte delimiter[] = "</response>".getBytes();
    int top=0;
    int i;

    // read in blocks of data looking for the </response> delimiter
    while (true)
    {
      int res;
      try
      {
        res = bufferedReader.read(buffer, num, 4096-num);
      }
      catch (IOException e)
      {
        System.out.println(e.getMessage());
        return null;
      }

      if (res<0)
      {
        lastDataSize=lastDataTop=0;
        return null;
      }

      lastDataSize=num+res;
      for (i=num;i<num+res;i++)
      {
        if (buffer[i]==delimiter[top])
        {
          top++;
          if (top>=delimiter.length)
          {
            num=i+1;
            buffer[num]=0;
            lastDataTop=num;
            return new String(buffer, 0, num);
          }
        }
        else
          top=0;
      }
      num+=res;
    }
  }

  /******************************************************************************/
  /* API Routines */
  /******************************************************************************/

  /* Returns the current image dimension */
  public Dimension getDimension()
  {
    if (!connected) return null;

    if (send("<request><get_dimension/></request>"))
    {
      // read in variable length
      String buffer;
      if ((buffer = readMessage())!=null)
      {
        if (xml.parse(buffer))
        {
          return new Dimension(xml.getInt("response.width"), xml.getInt("response.height"));
        }
      }
    }

    return null;
  }

  /*
  Returns the current processed image as a Java image.
  */

  public int[] getImage(String name)
  {
    if (!connected) return null;
    if (name==null) name="";

    // create the message request
    if (send("<request><get_image>"+escape(name)+"</get_image></request>"))
    {
      String buffer;
      // read in response which contains image information
      if ((buffer=readMessage())!=null)
      {
        // parse image width and height
        xml.parse(buffer);
        int len = xml.getInt("response.length");
        width = xml.getInt("response.width");
        height = xml.getInt("response.height");
        // ensure that we have enough room in pixels
        byte pixels[] = new byte[len];
        // actual image data follows the message
        if (readImageData(pixels, len)==len)
        {
          //DataBuffer db = new DataBufferByte(pixels, width*height*3, 0);
          //WritableRaster raster = Raster.createWritableRaster(BufferedImage.TYPE_3BYTE_BGR, db, null);
          //return new BufferedImage(ColorModel.getRGBdefault(), raster, false, null);
          int pixelInts[] = new int[width*height];
          int l = width*height*3;
          int i,j;
          for (j=i=0;i<l;i+=3,j++)
            pixelInts[j]=((pixels[i]&255)<<16)|((pixels[i+1]&255)<<8)|(pixels[i+2]&255);

          return pixelInts;
        }
      }
    }

    return null;
  }

  /*
  Returns the current processed image.
    pixels  - output - contains RGB 8 bit byte.
    width - output - contains grabbed image width
    height - output - contains image height
    len - input - maximum size of pixels to read
  */

  public Dimension getImage(byte pixels[], int len)
  {
    return getImage((String)"processed", pixels, len);
  }

  /*
  Returns the named image.
    name - input - name of image to grab. Can be source, processed, or marker name.
    pixels  - output - contains RGB 8 bit byte.
    width - output - contains grabbed image width
    height - output - contains image height
    len - input - maximum size of pixels to read
  */

  public Dimension getImage(String name, byte pixels[], int max)
  {
    if (!connected) return null;
    if (name==null) name="";

    // create the message request
    if (send("<request><get_image>"+escape(name)+"</get_image></request>"))
    {
      String buffer;
      // read in response which contains image information
      if ((buffer=readMessage())!=null)
      {
        // parse image width and height
        xml.parse(buffer);
        int len = xml.getInt("response.length");
        int width = xml.getInt("response.width");
        int height = xml.getInt("response.height");
        // ensure that we have enough room in pixels
        if (len>max)
        {
          skipData(len);
          return null;
        }

        // actual image data follows the message
        if (readImageData(pixels, len)==len)
          return new Dimension(width, height);
      }
    }

    return null;
  }

  /*
  Sets the current source image.
    pixels  - input - contains RGB 8 bit byte.
    width - input - contains grabbed image width
    height - input - contains image height
  */

  public boolean setImage(byte pixels[], int width, int height)
  {
    return setImage(null, pixels, width, height);
  }

  /*
  Sets the current source image.
    name - input - the name of the image to set. Can be source or marker name
    pixels  - input - contains RGB 8 bit byte.
    width - input - contains grabbed image width
    height - input - contains image height
  */

  public boolean setImage(String name, byte pixels[], int width, int height)
  {
    if (!connected) return false;
    if (name==null) name="";

    // setup the message request
    if (send("<request><set_image><source>"+escape(name)+"</source><width>"+width+"</width><height>"+height+"</height></set_image></request>"))
    {
      // send the RGB triplet pixels after message
      try
      {
        bufferedWriter.write(pixels, 0, width*height*3);
      }
      catch (IOException e)
      {
        return false;
      }

      // read message response
      String buffer;
      if ((buffer = readMessage())!=null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }
    return false;
  }

  /*
  Returns the value of the specified variable.
    name - input - the name of the variable to query
    result - output - contains the current value of the variable
    max - input - the maximum size of what the result can hold
  */

  public String getVariable(String name)
  {
    if (!connected) return null;
    if ((name==null)||(name.length()==0)) return null;

    if (send("<request><get_variable>"+escape(name)+"</get_variable></request>"))
    {
      // read in variable length
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        if (xml.parse(buffer))
        {
          return xml.getFirst();
        }
      }
    }

    return null;
  }

  /*
  Returns the value of the specified variables.
    name - input - the names of the variable to query
    result - output - contains the current values of the variables
    max - input - the maximum size of what the result can hold
  */

  public Vector getVariables(String names)
  {
    if (!connected) return null;
    if ((names==null)||(names.length()==0)) return null;

    if (send("<request><get_variables>"+escape(names)+"</get_variables></request>"))
    {
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        return xml.parseVector(buffer);
      }
    }

    return null;
  }

  /*
  Sets the value of the specified variable.
    name - input - the name of the variable to set
    value - input - contains the current value of the variable to be set
  */

  public boolean setVariable(String name, String value)
  {
    if (!connected) return false;
    if ((name==null)||(name.length()==0)) return false;

    if (send("<request><set_variable><name>"+escape(name)+"</name><value>"+escape(value)+"</value></set_variable></request>"))
    {
      // read in confirmation
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }

    return false;
  }

  /*
  Sets the value of the specified variables.
    names - input - the name of the variable to set
    values - input - contains the current value of the variable to be set
  */

  public boolean setVariables(String names[], String values[], int num)
  {
    if (!connected) return false;
    if ((names==null)||(values==null)||(names[0].length()==0)) return false;

    int j=0;
    int i;

    StringBuffer sb = new StringBuffer();

    // create request message
    sb.append("<request><set_variables>");
    for (i=0;(i<num);i++)
    {
      sb.append("<variable><name>");
      sb.append(escape(names[i]));
      sb.append("</name><value>");
      sb.append(escape(values[i]));
      sb.append("</value></variable>");
    }
    sb.append("</set_variables></request>");

    // send that message to RR Server
    if (send(sb.toString()))
    {
      // read in confirmation
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }

    return false;
  }

  /*
  Deletes the specified variable
    name - input - the name of the variable to delete
  */

  public boolean deleteVariable(String name)
  {
    if (!connected) return false;
    if ((name==null)||(name.length()==0)) return false;

    if (send("<request><delete_variable>"+escape(name)+"</delete_variable></request>"))
    {
      // read in variable length
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }

    return false;
  }

  /*
  Executes the provided image processing pipeline
    source - the XML .robo file string
  */

  public boolean execute(String source)
  {
    if (!connected) return false;
    if ((source==null)||(source.length()==0)) return false;

    //send the string
    if (send("<request><execute>"+escape(source)+"</execute></request>"))
    {
      // read in result
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }
    return false;
  }

  /*
  Executes the provided .robo file. Note that the file needs to be on the machine
  running RoboRealm. This is similar to pressing the 'open program' button in the
  main RoboRealm dialog.
    filename - the XML .robo file to run
  */
  public boolean loadProgram(String filename)
  {
    if (!connected) return false;
    if ((filename==null)||(filename.length()==0)) return false;

    if (send("<request><load_program>"+escape(filename)+"</load_program></request>"))
    {
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }

    return false;
  }

  /*
  Loads an image into RoboRealm. Note that the image needs to exist
  on the machine running RoboRealm. The image format must be one that
  RoboRealm using the freeimage.dll component supports. This includes
  gif, pgm, ppm, jpg, png, bmp, and tiff. This is
  similar to pressing the 'load image' button in the main RoboRealm
  dialog.
    name - name of the image. Can be "source" or a marker name,
    filename - the filename of the image to load
  */
  public boolean loadImage(String name, String filename)
  {
    if (!connected) return false;

    if ((filename==null)||(filename.length()==0)) return false;
    if ((name==null)||(name.length()==0)) name="source";

    if (send("<request><load_image><filename>"+escape(filename)+"</filename><name>"+escape(name)+"</name></load_image></request>"))
    {
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }

    return false;
  }

  /*
  Saves the specified image in RoboRealm to disk. Note that the filename is relative
  to the machine that is running RoboRealm. The image format must be one that
  RoboRealm using the freeimage.dll component supports. This includes
  gif, pgm, ppm, jpg, png, bmp, and tiff. This is
  similar to pressing the 'save image' button in the main RoboRealm
  dialog.
    name - name of the image. Can be "source","processed", or a marker name,
    filename - the filename of the image to save
  */
  public boolean saveImage(String source, String filename)
  {
    if (!connected) return false;

    if ((filename==null)||(filename.length()==0)) return false;
    if ((source==null)||(source.length()==0)) source="processed";

    // create the save image message
    if (send("<request><save_image><filename>"+escape(filename)+"</filename><source>"+escape(source)+"</source></save_image></request>"))
    {
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }

    return false;
  }

  /*
  Sets the current camera driver. This can be used to change the current viewing camera
  to another camera installed on the same machine. Note that this is a small delay
  when switching between cameras. The specified name needs only to partially match
  the camera driver name seen in the dropdown picklist in the RoboRealm options dialog.
  For example, specifying "Logitech" will select any installed Logitech camera including
  "Logitech QuickCam PTZ".
  */
  public boolean setCamera(String name)
  {
    if (!connected) return false;
    if ((name==null)||(name.length()==0)) return false;

    // create the save image message
    if (send("<request><set_camera>"+escape(name)+"</set_camera></request>"))
    {
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }

    return false;
  }

  /*
  This routine provides a way to stop processing incoming video. Some image processing
  tasks can be very CPU intensive and you may only want to enable processing when
  required but otherwise not process any incoming images to release the CPU for other
  tasks. The run mode can also be used to processing individual frames or only run
  the image processing pipeline for a short period. This is similar to pressing the
  "run" button in the main RoboRealm dialog.
    mode - can be toggle, on, off, once, or a number of frames to process
    */
  public boolean run(String mode)
  {
    if (!connected) return false;
    if ((mode==null)||(mode.length()==0)) return false;

    // create the save image message
    if (send("<request><run>"+escape(mode)+"</run></request>"))
    {
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }

    return false;
  }

  /*
  There is often a need to pause your own Robot Controller program to wait for
  RoboRealm to complete its task. The eaisest way to accomplish this is to wait
  on a specific variable that is set to a specific value by RoboRealm. Using the
  waitVariable routine you can pause processing and then continue when a variable
  changes within RoboRealm.
    name - name of the variable to wait for
    value - the value of that variable which will cancel the wait
    timeout - the maximum time to wait for the variable value to be set
  */

  public boolean waitVariable(String name, String value, int timeout)
  {
    if (timeout==0) timeout=100000000;

    if (!connected) return false;
    if ((name==null)||(name.length()==0)) return false;

    if (send("<request><wait_variable><name>"+escape(name)+"</name><value>"+escape(value)+"</value><timeout>"+timeout+"</timeout></wait_variable></request>"))
    {
      try
      {
        handle.setSoTimeout(timeout);
      }
      catch (SocketException e)
      {
        return false;
      }
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        try
        {
          handle.setSoTimeout(DEFAULT_TIMEOUT);
        }
        catch (SocketException e)
        {
          return false;
        }
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
      try
      {
        handle.setSoTimeout(DEFAULT_TIMEOUT);
      }
      catch (SocketException e)
      {
        return false;
      }
    }

    return false;
  }

  /*
  If you are rapdily grabbing images you will need to wait inbetween each
  get_image for a new image to be grabbed from the video camera. The wait_image
  request ensures that a new image is available to grab. Without this routine
  you may be grabbing the same image more than once.
  */

  public boolean waitImage(int timeout)
  {
    if (!connected) return false;

    if (send("<request><wait_image><timeout>"+timeout+"</timeout></wait_image></request>"))
    {
      String buffer;
      if ((buffer=readMessage())!=null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }

    return false;
  }


  /* Pauses RoboRealm processing to ensure a stable state while quering variables */
  public boolean pause()
  {
    if (send("<request><pause></pause></request>"))
    {
      String buffer;
      if ((buffer = readMessage()) != null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }

    return false;
  }

  /* Resumes RoboRealm processing after a pause */
  public boolean resume()
  {
    if (send("<request><resume></resume></request>"))
    {
      String buffer;
      if ((buffer = readMessage()) != null)
      {
        if (buffer.equals("<response>ok</response>"))
          return true;
      }
    }

    return false;
  }

  //////////////////////////////////// Basic Image Load/Save routines ////////////////////////
  // Utility routine to save a basic PPM
  public boolean savePPM(String filename, byte buffer[], int width, int height)
  {
    try
    {
      FileOutputStream fos = new FileOutputStream(new File(filename));
      String header = "P6\n"+width+" "+height+"\n255\n";
      fos.write(header.getBytes(), 0, header.length());
      fos.write(buffer, 0, width*height*3);
      fos.close();
    }
    catch (Exception e)
    {
      return false;
    };

    return true;
  }

  private String readLine(FileInputStream fis)
  {
    StringBuffer sb = new StringBuffer();
    while (true)
    {
      try
      {
        int c = fis.read();
        if (c=='\n')
        {
          if (sb.charAt(0)!='#')
            return sb.toString();

          sb.setLength(0);
        }
        sb.append((char)c);
      }
      catch (Exception e)
      {
        return null;
      }
    }
  }

  // Utility routine to load a basic PPM. Note that this routine does NOT handle
  // comments and is only included as a quick example.
  public Dimension loadPPM(String filename, byte buffer[], int max)
  {
    int width=0, height=0;

    try
    {
      FileInputStream fis = new FileInputStream(new File(filename));

      // read in P6 header skipping comments
      String header = readLine(fis);
      if (!header.equals("P6")) return null;

      // read in width height header skipping comments
      String size = readLine(fis);
      int ind = size.indexOf(' ');
      if (ind<0) return null;
      width = Integer.parseInt(size.substring(0, ind));
      height = Integer.parseInt(size.substring(ind+1));

      if ((width*height*3)>max) return null;
      fis.read(buffer, 0, width*height*3);
      fis.close();
    }
    catch (Exception e)
    {
      return null;
    };

    return new Dimension(width, height);
  }
}
