package org.openpnp.vision.pipeline.stages;



import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;



/**
 * GrabCut algorithm was designed by Carsten Rother, Vladimir Kolmogorov & Andrew Blake from
 * Microsoft Research Cambridge, UK. in their paper, "GrabCut": interactive foreground extraction
 * using iterated graph cuts . An algorithm was needed for foreground extraction with minimal user
 * interaction, and the result was GrabCut.
 */
public class GrabCut extends CvStage {

    @Attribute
    private int sideSquare = 50;
    @Attribute
    private int backGroundOriginX = 50;
    @Attribute
    private int backGroundOriginY = 50;



    public int getSideSquare() {
        return sideSquare;
    }

    public void setSideSquare(int sideSquare) {
        this.sideSquare = sideSquare;
    }

    public int getBackGroundOriginX() {
        return backGroundOriginX;
    }

    public void setBackGroundOriginX(int backGroundOriginX) {
        this.backGroundOriginX = backGroundOriginX;
    }

    public int getBackGroundOriginY() {
        return backGroundOriginY;
    }

    public void setBackGroundOriginY(int backGroundOriginY) {
        this.backGroundOriginY = backGroundOriginY;
    }

    @Override
    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        Point p1 = new Point(getBackGroundOriginX() - getSideSquare(),
                getBackGroundOriginY() - getSideSquare());
        Point p2 = new Point(getBackGroundOriginX() + getSideSquare(),
                getBackGroundOriginY() + getSideSquare());
        Rect rect = new Rect(p1, p2);



        Mat mask = new Mat();
        Mat finalMask = new Mat();
        Mat bgModel = new Mat();
        Mat fgModel = new Mat();

        Imgproc.grabCut(mat, mask, rect, bgModel, fgModel, 1, Imgproc.GC_INIT_WITH_RECT);

        bgModel.release();
        fgModel.release();

        Mat fg_mask = mask.clone();
        Mat pfg_mask = mask.clone();
        Mat source = new Mat(1, 1, CvType.CV_8U, new Scalar(3.0));
        Core.compare(mask, source, pfg_mask, Core.CMP_EQ);
        source = new Mat(1, 1, CvType.CV_8U, new Scalar(1.0));
        Core.compare(mask, source, fg_mask, Core.CMP_EQ);
        Core.bitwise_or(pfg_mask, fg_mask, finalMask);
        Mat fg_foreground = new Mat(mat.size(), mat.type(), new Scalar(0, 0, 0));
        Mat pfg_foreground = new Mat(mat.size(), mat.type(), new Scalar(0, 0, 0));
        mat.copyTo(fg_foreground, fg_mask);
        mat.copyTo(pfg_foreground, pfg_mask);

        Core.bitwise_or(fg_foreground, pfg_foreground, mat);

        fg_mask.release();
        finalMask.release();
        pfg_foreground.release();
        fg_foreground.release();

        return null;
    }
}

