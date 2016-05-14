/**
 * FiducialTest code provided by Cri S. Demonstrates round fiducial finding using contours.
 */

package org.openpnp.experiments;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.KeyStroke;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class FiducialTest {
    static {
        nu.pattern.OpenCV.loadShared();
        System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
    }

    public static void showResult(String name, Mat img) {
        // Imgproc.resize(img, img, new Size(640, 480));
        MatOfByte matOfByte = new MatOfByte();
        Highgui.imencode(".jpg", img, matOfByte);
        byte[] byteArray = matOfByte.toArray();
        BufferedImage bufImage = null;
        try {
            InputStream in = new ByteArrayInputStream(byteArray);
            bufImage = ImageIO.read(in);
            final JFrame frame = new JFrame(name);
            frame.getContentPane().add(new JLabel(new ImageIcon(bufImage)));
            frame.pack();
            frame.setVisible(true);
            // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "EXIT");
            frame.getRootPane().getActionMap().put("EXIT", new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    // frame.dispose();
                    System.exit(0);
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void showResult(Mat img) {
        showResult("", img);
    }

    static private Mat dst;

    // triangle, square and diamont code removed
    static public Point fiducial(Mat gray, int min, int max) throws Exception {
        final List<MatOfPoint> contours = new ArrayList<>();
        // final List<Rect> box = new ArrayList<>();
        final List<RotatedRect> box = new ArrayList<>();
        final List<Double> dist = new ArrayList<>();
        final Mat dummy = new Mat();
        // final Mat gray= OpenCvUtils.toMat(image,Highgui.IMREAD_GRAYSCALE);
        // prepare image
        if (gray.empty())
            return null;
        final Point ret = new Point(gray.width() / 2, gray.height() / 2);
        final Mat bw = new Mat();
        Imgproc.GaussianBlur(gray, gray, new Size(7, 7), 0);
        Imgproc.Canny(gray, bw, 0, 50, 5, false);
        Imgproc.threshold(bw, bw, 100, 255, 0);
        showResult("Canny", bw);
        // scan blobs
        Imgproc.findContours(bw, contours, dummy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        // get dimension, in java bit different

        final MatOfPoint2f temp = new MatOfPoint2f();
        for (int i = 0; i < contours.size(); i++) {
            temp.fromList(contours.get(i).toList());
            box.add(Imgproc.minAreaRect(temp));
        }

        // filter blob
        for (int i = contours.size(); i-- > 0;) {
            double a, l, t, f, x, y;
            temp.fromList(contours.get(i).toList());
            f = box.get(i).size.width / box.get(i).size.height;
            l = Imgproc.arcLength(temp, true);
            a = Imgproc.contourArea(contours.get(i));
            t = (a / (Math.PI * Math.pow(box.get(i).size.width / 2, 2)));
            if (f <= 0.8 || f >= 1.2 || t >= 1.2 || t <= 0.8 || box.get(i).size.width < min
                    || box.get(i).size.width > max || a < 100.) {
                contours.remove(i);
                continue;
            }

            final MatOfPoint2f approx = new MatOfPoint2f();
            final MatOfPoint matrix = new MatOfPoint();
            Imgproc.approxPolyDP(temp, approx, l * 0.02, true);
            approx.convertTo(matrix, CvType.CV_32S); // in java different
            // matrix.fromList(temp.toList()); // gives lot of trouble

            if (approx.toList().size() <= 6) // java thing
            {
                contours.remove(i);
                continue;
            }
            if (!Imgproc.isContourConvex(matrix)) {
                contours.remove(i);
                continue;
            }

            x = (box.get(i).center.x - ret.x);
            y = (box.get(i).center.y - ret.y);
            dist.add(0, x * x + y * y); // in java different
        }
        // return blob
        if (contours.size() != 0) {
            int i = 0; // holds nearest blob
            Rect r;
            // search nearest blob
            for (int j = 1; j < contours.size(); j++)
                if (dist.get(j) < dist.get(i))
                    i = j;
            r = Imgproc.boundingRect(contours.get(i)); // java thing
            // >>>>>>>>>>> this is for debug output, not for function
            Imgproc.drawContours(dst, contours, i, new Scalar(0, 0, 255), 2); // java
                                                                              // thing
            Core.circle(dst, ret, 4, new Scalar(0, 255, 255), -1);
            Core.circle(dst, new Point(r.x + r.width / 2, r.y + r.height / 2), 4,
                    new Scalar(0, 0, 255), -1);
            showResult("found", dst);
            // <<<<<<<<<<< end of debug
            // do java problem, it don't have supixel resulution, as cv version
            ret.x = r.x + r.width / 2 - ret.x;
            ret.y = r.y + r.height / 2 - ret.y;
            ret.y *= -1; // adj coord system
            return ret;
        }
        // no blob found
        return null;
    }

    public static void main(String[] args) throws Exception {
        int min = 0, max = 999999;
        args = new String[] {"",
                "/Users/jason/.openpnp/org.openpnp.machine.reference.vision.OpenCvVisionProvider/camera_41173395697117969.png",
                "/Users/jason/Desktop/result.png"};
        if (args.length > 2)
            ;
        else {
            System.out.println("Usage: <prg> <image> <result> [ <min-pixel> <max-pixel> ]");
            return;
        }
        if (args.length > 4) {
            min = Integer.valueOf(args[3]);
            max = Integer.valueOf(args[4]);
        }
        // System.out.println("Usage: <prg> " + args[1]+" "+args[2]);
        Mat src = Highgui.imread(args[1], 0);
        dst = Highgui.imread(args[1], 1);
        // showResult(dst);
        Imgproc.cvtColor(src, dst, Imgproc.COLOR_GRAY2BGR);

        Point p = fiducial(src, min, max);
        try {
            System.out.println("Point: " + p.x + " " + p.y);
            Highgui.imwrite(args[2], dst);
            // showResult("dst",dst);
        }
        catch (Exception ex) {
            ;
        }

        return;
    }

}
