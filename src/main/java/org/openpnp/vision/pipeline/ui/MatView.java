package org.openpnp.vision.pipeline.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.awt.image.BufferedImage;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
//import java.awt.geom.*;
import java.awt.BasicStroke;
import org.pmw.tinylog.Logger;

import org.opencv.core.Mat;
import org.openpnp.util.OpenCvUtils;

public class MatView extends JComponent {
    private BufferedImage image;
    private ArrayList<Point> coords;
    private int markSize = 7;
    private JTextPane rangeTextPane;
    private int shapeType = 0;
    private int[] colorRange;
    private Polygon polygon;
    private ArrayList<Polygon> polygons;
    
    public MatView() {
        coords = new ArrayList<Point>();
        polygons = new ArrayList<Polygon>();
        polygon = new Polygon();
        setBackground(Color.black);
        addMouseListener(mouseListener);
        clearColorRange();
    }

    public void setMat(Mat mat) {
        if (mat == null || mat.empty()) {
            image = null;
        }
        else {
            image = OpenCvUtils.toBufferedImage(mat);
        }
        repaint();
    }
    
    public void setTextPane(JTextPane rangeTextPane) {
      this.rangeTextPane = rangeTextPane;
    }

    public int getMarkSize() {
      return this.markSize;
    }

    public void setMarkSize(int markSize) {
      this.markSize = markSize;
    }
    
    public void setShapeType(int shapeType) {
      this.shapeType = shapeType;
    }
    
    public void clearColorRange() {
      coords.clear();
      polygon = new Polygon();
      polygons.clear();
      colorRange = new int[] {255, 0, 255, 0, 255, 0};
    }

    public void clearPoly() {
    
    }
    public void setColorRange(Color rgb) {
      int  red = rgb.getRed();
      int  green = rgb.getGreen();
      int  blue = rgb.getBlue();

      colorRange[0] = blue  < colorRange[0] ? blue  : colorRange[0];
      colorRange[1] = blue  > colorRange[1] ? blue  : colorRange[1];
      colorRange[2] = green < colorRange[2] ? green : colorRange[2];
      colorRange[3] = green > colorRange[3] ? green : colorRange[3];
      colorRange[4] = red   < colorRange[4] ? red   : colorRange[4];
      colorRange[5] = red   > colorRange[5] ? red   : colorRange[5];
    }
    
    public void updateColorRange() {
      // reset limits
      colorRange = new int[] {255, 0, 255, 0, 255, 0};
      // do points
      Color rgb;
      for (Point ps : coords) {
        rgb = new Color(image.getRGB(ps.x, ps.y), true);
        setColorRange(rgb);
      }
      /**
        polygon color range
        We will scan the bbox area of the polygon for pixels inside and add these pixel colors to colorRange
      */
      for (Polygon poly : polygons) {
        Rectangle bbox = poly.getBounds();
        if (poly.npoints == 2) {
          // special case : polygon is a straight line. Make it 
        }
        for (int x = bbox.x; x < bbox.x + bbox.width;  x++) {
          for (int y=bbox.y; y < bbox.y + bbox.height; y++) {
            if (poly.contains(x,y)) {
              rgb = new Color(image.getRGB(x, y), true);
              setColorRange(rgb);
            }
          }
        }
      }
      rangeTextPane.setText(
        String.format("BGR: %03d:%03d, %03d:%03d, %03d:%03d",
          colorRange[0], colorRange[1],
          colorRange[2], colorRange[3],
          colorRange[4], colorRange[5]
        )
      );
     }
    
    public Point scalePointInv(Point p, boolean inv) {
        if (image == null) {
            return new Point(0, 0);
        }
        
        Insets ins = getInsets();
        double sourceWidth = image.getWidth();
        double sourceHeight = image.getHeight();
        double destWidth = getWidth() - ins.left - ins.right;
        double destHeight = getHeight() - ins.top - ins.bottom;

        double widthRatio = sourceWidth / destWidth;
        double heightRatio = sourceHeight / destHeight;

        double scaledHeight, scaledWidth;

        if (heightRatio > widthRatio) {
            double aspectRatio = sourceWidth / sourceHeight;
            scaledHeight = destHeight;
            scaledWidth = (scaledHeight * aspectRatio);
        }
        else {
            double aspectRatio = sourceHeight / sourceWidth;
            scaledWidth = destWidth;
            scaledHeight = (scaledWidth * aspectRatio);
        }

        int imageX = (int) (ins.left + (destWidth / 2) - (scaledWidth / 2));
        int imageY = (int) (ins.top + (destHeight / 2) - (scaledHeight / 2));
        
        int x, y;
        if (!inv) {
          x = (int) ((p.x - imageX) * (sourceWidth / scaledWidth));
          y = (int) ((p.y - imageY) * (sourceHeight / scaledHeight));
          x = Math.max(x, 0);
          x = Math.min(x, image.getWidth() - 1);
          y = Math.max(y, 0);
          y = Math.min(y, image.getHeight() - 1);
        
        } else {
          x = (int) (p.x / (sourceWidth / scaledWidth) + imageX);
          y = (int) (p.y / (sourceHeight / scaledHeight) + imageY);
        }
        
        return new Point(x, y);
    }
    public Point scalePoint(Point p) {
        return scalePointInv(p, false);
    }

    private MouseListener mouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.isPopupTrigger() || image == null) {
              return;
          }
          // coordinate of pane point clicked
          Point p = new Point(e.getX(),e.getY());
          // scaled coordinates of image pixel corresponding to point clicked
          Point ps = scalePoint(p);
          // double click
          if (e.getClickCount() == 2) {
            // double click
          }
          // add a new polygon
          if (polygons.size() == 0 || e.isShiftDown()) {
            polygon = new Polygon();
            polygons.add(polygon);
          }
          if (SwingUtilities.isRightMouseButton(e)) {
            // clear everything on right click
            clearColorRange();
            rangeTextPane.setText("BGR: :, :, :; XY: , ");
          } else {
            
            if(shapeType == 0) {
              // single points
              coords.add(ps);
              
            } else if (shapeType == 1) {
              // polygons
              polygon.addPoint(ps.x, ps.y);
            }
          }
          updateColorRange();
          rangeTextPane.setText(rangeTextPane.getText() + String.format("; XY: %d, %d", ps.x, ps.y));
          repaint();
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }
    };

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) {
            return;
        }

        Insets ins = getInsets();
        double sourceWidth = image.getWidth();
        double sourceHeight = image.getHeight();
        double destWidth = getWidth() - ins.left - ins.right;
        double destHeight = getHeight() - ins.top - ins.bottom;

        /**
         * We want to fit both axes in the given destWidth and destHeight while maintaining the
         * aspect ratio. If the frame is smaller in either or both axes than the original will need
         * to be scaled to fill the space as completely as possible while still maintaining the
         * aspect ratio. 1. Determine the source size of the image: sourceWidth, sourceHeight. 2.
         * Determine the max size each axis can be: destWidth, destHeight. 3. Calculate how much
         * each axis needs to be scaled to fit. 4. Use the larger of the two and scale the opposite
         * axis by the aspect ratio + the scaling ratio.
         */

        double widthRatio = sourceWidth / destWidth;
        double heightRatio = sourceHeight / destHeight;

        double scaledHeight, scaledWidth;

        if (heightRatio > widthRatio) {
            double aspectRatio = sourceWidth / sourceHeight;
            scaledHeight = destHeight;
            scaledWidth = (scaledHeight * aspectRatio);
        }
        else {
            double aspectRatio = sourceHeight / sourceWidth;
            scaledWidth = destWidth;
            scaledHeight = (scaledWidth * aspectRatio);
        }

        int imageX = (int) (ins.left + (destWidth / 2) - (scaledWidth / 2));
        int imageY = (int) (ins.top + (destHeight / 2) - (scaledHeight / 2));

        Graphics2D g2d = (Graphics2D) g;

        g2d.drawImage(image, imageX, imageY, (int) scaledWidth, (int) scaledHeight, null);

        g2d.setColor(Color.green);
        g2d.setStroke(new BasicStroke(1));
        
        Point p;
        // single points
        for (Point ps : coords) {
          p = scalePointInv(ps, true);
          g2d.drawOval(p.x - markSize/2, p.y - markSize/2, markSize, markSize);
        }
        // polygons
        for (Polygon poly : polygons) {
          switch (poly.npoints) {
            case 0:
              break;
            case 1:
              // draw a circle
              // convert the center to the left,top corner of the oval
              p = scalePointInv(new Point(poly.xpoints[0],poly.ypoints[0]),true);
              g2d.drawOval(p.x - 1, p.y - 1, 2, 2);
              break;
            default:
              int[] xp = new int[poly.npoints];
              int[] yp = new int[poly.npoints];
              Polygon tempoly = new Polygon();
              for (int i=0; i < poly.npoints; i++) {
                p = scalePointInv(new Point(poly.xpoints[i],poly.ypoints[i]), true);
                tempoly.addPoint(p.x, p.y); 
              }
              // now paint the polygon
              g2d.draw(tempoly);
              break;
          }
        }
        g2d.dispose();
    }
}
