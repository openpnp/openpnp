/*
 * Copyright ( c) 2017, Cri.S <phone.cri@gmail.com> All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 1. Redistributions of source code must retain the
 * above copyright notice, this list of conditions and the following disclaimer. 2. Redistributions
 * in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software must display the
 * following acknowledgement: This product includes software developed by the <organization>. 4.
 * Neither the name of the <organization> nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES ( INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT ( INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.openpnp.vision.pipeline.stages;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.openpnp.vision.pipeline.CvPipeline;
import org.openpnp.vision.pipeline.CvStage;
import org.simpleframework.xml.Attribute;



public class SimpleBlobDetector extends CvStage {
    @Attribute(required=false)
    private double thresholdStep = 10.;
    @Attribute(required=false)
    private double thresholdMin = 50.;
    @Attribute(required=false)
    private double thresholdMax = 220.;
    @Attribute(required=false)
    private int repeatability = 2;
    @Attribute(required=false)
    private double distBetweenBlobs = 10.;
    @Attribute(required=false)
    private boolean color = true;
    @Attribute(required=false)
    private double colorValue = 0;
    @Attribute(required=false)
    private boolean area = true;
    @Attribute(required=false)
    private double areaMin = 25.;
    @Attribute(required=false)
    private double areaMax = 5000.;
    @Attribute(required=false)
    private boolean circularity = false;
    @Attribute(required=false)
    private double circularityMin = 0.80000001192092896;
    @Attribute(required=false)
    private double circularityMax = -1;
    @Attribute(required=false)
    private boolean inertia = true;
    @Attribute(required=false)
    private double inertiaRatioMin = 1.0000000149011612E-001;
    @Attribute(required=false)
    private double inertiaRatioMax = -1;
    @Attribute(required=false)
    private boolean convexity = true;
    @Attribute(required=false)
    private double convexityMin = 9.4999998807907104E-001;
    @Attribute(required=false)
    private double convexityMax = -1;

    public double getThresholdStep() {
        return thresholdStep;
    }

    public void setThresholdStep(double val) {
        thresholdStep = val;
    }

    public double getThresholdMin() {
        return thresholdMin;
    }

    public void setThresholdMin(double val) {
        thresholdMin = val;
    }

    public double getThresholdMax() {
        return thresholdMax;
    }

    public void setThresholdMax(double val) {
        thresholdMax = val;
    }

    public int getRepeatability() {
        return repeatability;
    }

    public void setRepeatability(int val) {
        repeatability = val;
    }

    public double getDistBetweenBlobs() {
        return distBetweenBlobs;
    }

    public void setDistBetweenBlobs(double val) {
        distBetweenBlobs = val;
    }

    public boolean getColor() {
        return color;
    }

    public void setColor(boolean val) {
        color = val;
    }

    public double getColorValue() {
        return colorValue;
    }

    public void setColorValue(double val) {
        colorValue = val;
    }

    public boolean getArea() {
        return area;
    }

    public void setArea(boolean val) {
        area = val;
    }

    public double getAreaMin() {
        return areaMin;
    }

    public void setAreaMin(double val) {
        areaMin = val;
    }

    public double getAreaMax() {
        return areaMax;
    }

    public void setAreaMax(double val) {
        areaMax = val;
    }

    public boolean getCircularity() {
        return circularity;
    }

    public void setCircularity(boolean val) {
        circularity = val;
    }

    public double getCircularityMin() {
        return circularityMin;
    }

    public void setCircularityMin(double val) {
        circularityMin = val;
    }

    public double getCircularityMax() {
        return circularityMax;
    }

    public void setCircularityMax(double val) {
        circularityMax = val;
    }

    public boolean getInertia() {
        return inertia;
    }

    public void setInertia(boolean val) {
        inertia = val;
    }

    public double getInertiaRatioMin() {
        return inertiaRatioMin;
    }

    public void setInertiaRatioMin(double val) {
        inertiaRatioMin = val;
    }

    public double getInertiaRatioMax() {
        return inertiaRatioMax;
    }


    public void setInertiaRatioMax(double val) {
        inertiaRatioMax = val;
    }

    public boolean getConvexity() {
        return convexity;
    }

    public void setConvexity(boolean val) {
        convexity = val;
    }

    public double getConvexityMin() {
        return convexityMin;
    }

    public void setConvexityMin(double val) {
        convexityMin = val;
    }

    public double getConvexityMax() {
        return convexityMax;
    }

    public void setConvexityMax(double val) {
        convexityMax = val;
    }

    private void writeToFile(File file, String data) throws Exception {
        FileOutputStream stream = new FileOutputStream(file);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
        outputStreamWriter.write(data);
        outputStreamWriter.close();
        stream.close();
    }

    public Result process(CvPipeline pipeline) throws Exception {
        Mat mat = pipeline.getWorkingImage();
        FeatureDetector blobDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
        File outputFile = File.createTempFile("SimpleBlobDetector", ".YAML");
        writeToFile(outputFile,
                "%YAML:1.0" // java
                        // parameter
                        // backdoor
                        + "\nthresholdStep: " + thresholdStep + "\nminThreshold: " + thresholdMin
                        + "\nmaxThreshold: " + thresholdMax + "\nminRepeatability: " + repeatability
                        + "\nminDistBetweenBlobs: " + distBetweenBlobs + "\nfilterByColor: "
                        + (color ? 1 : 0) + "\nblobColor: " + colorValue + "\nfilterByArea: "
                        + (area ? 1 : 0) + "\nminArea: " + areaMin + "\nmaxArea: "
                        + (areaMax < 0. ? 3.4028234663852886E+038 : areaMax)
                        + "\nfilterByCircularity: " + (circularity ? 1 : 0) + "\nminCircularity: "
                        + circularityMin + "\nmaxCircularity: "
                        + (circularityMax < 0. ? 3.4028234663852886E+038 : circularityMax)
                        + "\nfilterByInertia: " + (inertia ? 1 : 0) + "\nminInertiaRatio: "
                        + inertiaRatioMin + "\nmaxInertiaRatio: "
                        + (inertiaRatioMax < 0. ? 3.4028234663852886E+038 : inertiaRatioMax)
                        + "\nfilterByConvexity: " + (convexity ? 1 : 0) + "\nminConvexity: "
                        + convexityMin + "\nmaxConvexity: "
                        + (convexityMax < 0. ? 3.4028234663852886E+038 : convexityMax) + "\n");
        blobDetector.read(outputFile.getAbsolutePath());
        outputFile.delete();
        MatOfKeyPoint kpMat = new MatOfKeyPoint();
        blobDetector.detect(mat, kpMat);
        List<KeyPoint> keypoints = kpMat.toList();
        kpMat.release();
        return new Result(null, keypoints);
    }
}
