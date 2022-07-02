package org.openpnp.vision.pipeline.stages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.model.Location;
import org.openpnp.util.Utils2D;
import org.openpnp.vision.FluentCv;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.openpnp.vision.pipeline.Property;
import org.openpnp.vision.pipeline.Stage;
import org.simpleframework.xml.Attribute;

/**
 * Finds the smallest rotated rectangle that encloses pixels that fall within the given range.
 * Input should be a grayscale image. 
 */
@Stage(description="Finds the smallest rotated rectangle that encloses pixels that fall within the given range.\n"
        + "Input should be a grayscale image.")
public class MinAreaRect extends CvStage {
    @Attribute
    @Property(description = "Threshold minimum grayscale value of the pixel to be considered \"set\".")
    private int thresholdMin;

    @Attribute
    @Property(description = "Threshold mayimum grayscale value of the pixel to be considered \"set\".")
    private int thresholdMax;

    @Attribute(required = false)
    @Property(description = "Expected angle of the rectangular hull to be detected.")
    private double expectedAngle = 0;

    @Attribute(required = false)
    @Property(description = "Detect the left edge of the rectangle (rotated to expectedAngle).")
    private boolean leftEdge = true;

    @Attribute(required = false)
    @Property(description = "Detect the right edge of the rectangle (rotated to expectedAngle).")
    private boolean rightEdge = true;

    @Attribute(required = false)
    @Property(description = "Detect the top edge of the rectangle (rotated to expectedAngle).")
    private boolean topEdge = true;

    @Attribute(required = false)
    @Property(description = "Detect the bottom edge of the rectangle (rotated to expectedAngle).")
    private boolean bottomEdge = true;

    @Attribute(required = false)
    @Property(description = "Display the detection result diagnostics.")
    private boolean diagnostics = false;

    public int getThresholdMin() {
        return thresholdMin;
    }

    public void setThresholdMin(int thresholdMin) {
        this.thresholdMin = thresholdMin;
    }

    public int getThresholdMax() {
        return thresholdMax;
    }

    public void setThresholdMax(int thresholdMax) {
        this.thresholdMax = thresholdMax;
    }

    public double getExpectedAngle() {
        return expectedAngle;
    }

    public void setExpectedAngle(double expectedAngle) {
        this.expectedAngle = expectedAngle;
    }

    public boolean isLeftEdge() {
        return leftEdge;
    }

    public void setLeftEdge(boolean leftEdge) {
        this.leftEdge = leftEdge;
    }

    public boolean isRightEdge() {
        return rightEdge;
    }

    public void setRightEdge(boolean rightEdge) {
        this.rightEdge = rightEdge;
    }

    public boolean isBottomEdge() {
        return bottomEdge;
    }

    public void setTopEdge(boolean topEdge) {
        this.topEdge = topEdge;
    }

    public boolean isDiagnostics() {
        return diagnostics;
    }

    public void setBottomEdge(boolean bottomEdge) {
        this.bottomEdge = bottomEdge;
    }

    public boolean isTopEdge() {
        return topEdge;
    }

    public void setDiagnostics(boolean diagnostics) {
        this.diagnostics = diagnostics;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    @Attribute(required = false)
    @Property(description = "determines the pipeline property name under which this stage is controlled by the vision operation. "
            + "If set, these will override some of the properties configured here. Use \"MinAreaRect\" for default control.")
    private String propertyName = "MinAreaRect";


    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        if (pipeline.getWorkingColorSpace() != FluentCv.ColorSpace.Gray){
            throw new Exception(String.format("%s is not compatible with %s colorspace. Only Grey colorspace is supported.", MinAreaRect.class.getSimpleName(), pipeline.getWorkingColorSpace()));
        }
        Mat mat = pipeline.getWorkingImage();
        Point center = new Point(mat.cols()*0.5, mat.rows()*0.5);
        double expectedAngle = getExpectedAngle();
        boolean leftEdge = isLeftEdge();
        boolean rightEdge = isRightEdge();
        boolean topEdge = isTopEdge();
        boolean bottomEdge = isBottomEdge();

        if (!propertyName.isEmpty()) {

            center = getPossiblePipelinePropertyOverride(center, pipeline, 
                    propertyName + ".center", Point.class, org.opencv.core.Point.class, 
                    Location.class);

            expectedAngle = getPossiblePipelinePropertyOverride(expectedAngle, pipeline, 
                    propertyName + ".expectedAngle", Double.class, Integer.class);

            leftEdge = getPossiblePipelinePropertyOverride(leftEdge, pipeline, 
                    propertyName + ".leftEdge");
            rightEdge = getPossiblePipelinePropertyOverride(rightEdge, pipeline, 
                    propertyName + ".rightEdge");
            topEdge = getPossiblePipelinePropertyOverride(topEdge, pipeline, 
                    propertyName + ".topEdge");
            bottomEdge = getPossiblePipelinePropertyOverride(bottomEdge, pipeline, 
                    propertyName + ".bottomEdge");
        }

        List<Point> points = new ArrayList<>();
        byte[] rowData = new byte[mat.cols()];
        final int cols = mat.cols();
        final int rows = mat.rows();
        // Only take the set pixels that are the first or last pixel in BOTH their horizontal AND vertical 
        // scan-lines. These few pixels are sufficient to stake out the convex hull of the subject. 
        int[] firstRowVertical = new int[cols]; 
        int[] lastRowVertical = new int[cols];
        // -1 means uninitialized.
        Arrays.fill(firstRowVertical, -1);
        for (int row = 0; row < rows; row++) {
            mat.get(row, 0, rowData);
            int firstColHorizontal = -1;
            int lastColHorizontal = -1;
            for (int col = 0; col < cols; col++) {
                int pixel = ((int) rowData[col]) & 0xff;
                if (pixel >= thresholdMin && pixel <= thresholdMax) {
                    if (firstColHorizontal < 0) {
                        firstColHorizontal = col;
                    }
                    lastColHorizontal = col;
                }
            }
            if (firstColHorizontal >= 0) {
                if (firstRowVertical[firstColHorizontal] < 0) {
                    firstRowVertical[firstColHorizontal] = row;
                }
                if (firstRowVertical[lastColHorizontal] < 0) {
                    firstRowVertical[lastColHorizontal] = row;
                }
                lastRowVertical[firstColHorizontal] = row;
                lastRowVertical[lastColHorizontal] = row;
            }
        }
        if (diagnostics) {
            mat.setTo(new Scalar(0, 0, 0));
        }
        for (int col = 0; col < cols; col++) {
            if (firstRowVertical[col] >= 0) {
                points.add(new Point(col, firstRowVertical[col]));
                points.add(new Point(col, lastRowVertical[col]));
                if (diagnostics) {
                    byte[] pixelData = new byte[] { -1 };
                    mat.put(firstRowVertical[col], col, pixelData);
                    mat.put(lastRowVertical[col], col, pixelData);
                }
            }
        }
        if (points.isEmpty()) {
            return null;
        }
        RotatedRect r;
        if (leftEdge && rightEdge && bottomEdge && topEdge) {
            // All edges, use OpenCv method.
            MatOfPoint2f pointsMat = new MatOfPoint2f(points.toArray(new Point[points.size()]));
            r = Imgproc.minAreaRect(pointsMat);
            pointsMat.release();

            // Rotate nearest to expectedAngle in 90° steps.
            // Note, the angles in RotatedRect are left-handed (we reverse the sign).
            int steps90 = (int) Math.round((expectedAngle + r.angle)/90);
            if (steps90 != 0) {
                if ((steps90 & 1) != 0) {
                    // Odd 90° rotation, must swap size
                    r = new RotatedRect(
                            r.center, 
                            new Size(r.size.height, r.size.width), 
                            Utils2D.angleNorm(r.angle - steps90*90, 180));
                }
                else {
                    r = new RotatedRect(
                            r.center, 
                            r.size, 
                            Utils2D.angleNorm(r.angle - steps90*90, 180));
                }
            }
        }
        else {
            // Partial edges, use own method.
            // Translate points to center relative.
            for (Point p : points) {
                p.x -= center.x;
                p.y -= center.y;
            }
            // Detect the minimum area edges.
            r = minAreaEdges(points, leftEdge, rightEdge, topEdge, bottomEdge, 
                    expectedAngle, 45, 
                    cols, rows);
            // Translate rotated rect back to mat coordinates. 
            if (r != null) {
                r = new RotatedRect(
                        new Point(center.x + r.center.x, center.y + r.center.y), 
                        r.size, 
                        r.angle);
            }
        }
        return new Result(null, r);
    }

    private final static int stepsPerSearch = 18;
    private final static double angularResolution = 0.01;

    public static RotatedRect minAreaEdges(List<Point> points,
            boolean leftEdge, boolean rightEdge,
            boolean topEdge, boolean bottomEdge, 
            double expectedAngle, double searchAngle,  
            int cols, int rows) {
        // Search the best angle.
        double bestArea = Double.POSITIVE_INFINITY;
        double bestAngle = Double.NaN;
        RotatedRect bestRect = null;
        final double unbounded = Math.hypot(cols, rows);
        final double step = searchAngle/stepsPerSearch;
        final double da = Math.toRadians(step);
        for (double angle = Math.toRadians(expectedAngle - searchAngle), 
                angleMax = Math.toRadians(angleMax = expectedAngle + searchAngle); 
                angle < angleMax; 
                angle += da) {
            // Note this is the OpenCv 2D left-handed coordinate system, Y pointing down.
            double s = Math.sin(angle);
            double c = Math.cos(angle);
            double x0 = Double.POSITIVE_INFINITY;
            double x1 = Double.NEGATIVE_INFINITY;
            double y0 = Double.POSITIVE_INFINITY;
            double y1 = Double.NEGATIVE_INFINITY;
            for (Point p : points) {
                // Rotate the point to neutral from detection angle.
                double x = c*p.x + -s*p.y;
                double y = s*p.x + c*p.y;
                if (x0 > x) {
                    x0 = x;
                }
                if (x1 < x) {
                    x1 = x;
                }
                if (y0 > y) {
                    y0 = y;
                }
                if (y1 < y) {
                    y1 = y;
                }
            }
            // Enlarge unbounded edges to the maximum.
            if (!leftEdge) {
                x0 = -unbounded;
            }
            if (!rightEdge) {
                x1 = +unbounded;
            }
            if (!topEdge) {
                y0 = -unbounded;
            }
            if (!bottomEdge) {
                y1 = +unbounded;
            }
            // calculate the area
            double area = (x1 - x0)*(y1 - y0);
            if (bestArea > area) {
                bestArea = area;
                double dx = (x0 + x1)/2;
                double dy = (y0 + y1)/2;
                double px = c*dx + s*dy;
                double py = -s*dx + c*dy;
                bestAngle = Math.toDegrees(angle);
                bestRect = new RotatedRect(
                        new Point(px, py), 
                        new Size(x1 - x0, y1 - y0), 
                        -bestAngle);
            }
        }
        if (step > angularResolution) {
            // Refine in recursion.
            return minAreaEdges(points, leftEdge, rightEdge, topEdge, bottomEdge, 
                    bestAngle, step, cols, rows);
        }
        else {
            return bestRect;
        }
    }
}
