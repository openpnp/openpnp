package org.openpnp.vision.gpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.gpu.GpuCameraTransform.Input;
import org.openpnp.vision.gpu.GpuImageOps.ColorConversion;

public class GpuRecordingTest {
    @BeforeAll
    public static void setup() {
        nu.pattern.OpenCV.loadLocally();
        Assumptions.assumeTrue(GpuRuntime.hasByteStorage() && GpuRuntime.hasArrayIndexing(),
                "no Vulkan GPU with 8-bit storage");
    }

    private static Mat colorImage(int rows, int cols) {
        Mat mat = new Mat(rows, cols, CvType.CV_8UC3);
        Core.randu(mat, 0, 256);
        Imgproc.GaussianBlur(mat, mat, new Size(5, 5), 0);
        Imgproc.circle(mat, new Point(cols / 2, rows / 2), rows / 4, new Scalar(230, 240, 250), -1);
        return mat;
    }

    private static long idleTimeline() {
        long value = GpuRuntime.completed();
        GpuRuntime.await(value, GpuCompute.TIMEOUT_NS);
        return value;
    }

    private static Mat chain(GpuImage src) {
        GpuImage masked = GpuImageOps.maskCircle(src, src.cols() / 2, src.rows() / 2, src.rows() / 3, false);
        GpuImage gray = GpuImageOps.convertColor(masked, ColorConversion.Bgr2Gray);
        GpuImage blurred = GpuImageOps.gaussianBlur(gray, 5);
        GpuImage binary = GpuImageOps.threshold(blurred, 100, false);
        Mat result = binary.download();
        for (GpuImage image : new GpuImage[] { masked, gray, blurred, binary }) {
            image.release();
        }
        return result;
    }

    @Test
    public void chainSubmitsOnceAndMatchesSeparateSubmits() {
        Mat mat = colorImage(240, 320);
        GpuImage src = GpuImage.upload(mat);
        Mat separate = chain(src);
        long before = idleTimeline();
        Mat recorded;
        try (GpuRecording recording = GpuRecording.open()) {
            recorded = chain(src);
        }
        assertEquals(before + 1, idleTimeline());
        src.release();
        Mat diff = new Mat();
        Core.absdiff(separate, recorded, diff);
        assertEquals(0, Core.countNonZero(diff));
    }

    @Test
    public void repeatedRecordingsReuseTheirProgram() {
        Mat mat = colorImage(120, 160);
        GpuImage src = GpuImage.upload(mat);
        GpuImage first;
        try (GpuRecording recording = GpuRecording.open()) {
            first = GpuImageOps.threshold(src, 80, false);
        }
        GpuBuffer buffer = first.buffer();
        first.release();
        for (int i = 0; i < 5; i++) {
            GpuImage again;
            try (GpuRecording recording = GpuRecording.open()) {
                again = GpuImageOps.threshold(src, 80 + i, false);
            }
            assertTrue(buffer == again.buffer());
            again.release();
        }
        src.release();
    }

    @Test
    public void heldImagesGetFreshBuffers() {
        GpuImage src = GpuImage.upload(colorImage(60, 80));
        GpuImage a;
        GpuImage b;
        try (GpuRecording recording = GpuRecording.open()) {
            a = GpuImageOps.threshold(src, 50, false);
        }
        try (GpuRecording recording = GpuRecording.open()) {
            b = GpuImageOps.threshold(src, 200, false);
        }
        assertTrue(a.buffer() != b.buffer());
        assertTrue(Core.countNonZero(a.download().reshape(1)) > Core.countNonZero(b.download().reshape(1)));
        a.release();
        b.release();
        src.release();
    }

    @Test
    public void readbackSubmitsTheRecording() {
        Mat gray = new Mat(50, 70, CvType.CV_8UC1, new Scalar(0));
        Imgproc.rectangle(gray, new Rect(10, 5, 30, 20), new Scalar(200), -1);
        GpuImage src = GpuImage.upload(gray);
        int[] extremes;
        try (GpuRecording recording = GpuRecording.open()) {
            GpuImage blurred = GpuImageOps.gaussianBlur(src, 3);
            extremes = GpuImageOps.rowExtremes(blurred, 160, 255);
            blurred.release();
        }
        assertArrayEquals(new int[] { -1, -1, 0, 0 }, new int[] { extremes[0], extremes[1], extremes[2], extremes[3] });
        assertEquals(11, extremes[10 * 4]);
        assertEquals(38, extremes[10 * 4 + 1]);
        src.release();
    }

    @Test
    public void rowExtremesMatchCpu() {
        Mat gray = new Mat(97, 131, CvType.CV_8UC1);
        Core.randu(gray, 0, 256);
        GpuImage src = GpuImage.upload(gray);
        int[] extremes = GpuImageOps.rowExtremes(src, 120, 200);
        src.release();
        byte[] row = new byte[gray.cols()];
        for (int y = 0; y < gray.rows(); y++) {
            gray.get(y, 0, row);
            int first = -1;
            int last = -1;
            int n = 0;
            int sum = 0;
            for (int x = 0; x < row.length; x++) {
                int v = row[x] & 0xFF;
                if (v >= 120 && v <= 200) {
                    first = first < 0 ? x : first;
                    last = x;
                    n++;
                    sum += x;
                }
            }
            assertArrayEquals(new int[] { first, last, n, sum },
                    new int[] { extremes[y * 4], extremes[y * 4 + 1], extremes[y * 4 + 2], extremes[y * 4 + 3] });
        }
    }

    @Test
    public void settleImageMatchesOpenCv() {
        Mat mat = colorImage(480, 640);
        BufferedImage image = OpenCvUtils.toBufferedImage(mat);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        try (GpuCameraTransform transform = new GpuCameraTransform();
                GpuBuffer upload = new GpuBuffer(data.length, true)) {
            upload.map().put(data);
            for (int divisor = 1; divisor <= 6; divisor++) {
                for (Rect crop : new Rect[] { new Rect(0, 0, 640 - 640 % divisor, 480 - 480 % divisor),
                        new Rect(100, 40, divisor * 60, divisor * 50) }) {
                    Mat gpu = transform.renderSettle(transform.slots(upload), 0, Input.Bgr, 640, 480, 640 * 3,
                            crop.x, crop.y, crop.width, crop.height, divisor, value -> {
                            });
                    Mat gray = new Mat();
                    Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY);
                    Mat cpu = gray.submat(crop);
                    if (divisor > 1) {
                        Mat resized = new Mat();
                        Imgproc.resize(cpu, resized, new Size(cpu.cols() / divisor, cpu.rows() / divisor),
                                1.0 / divisor, 1.0 / divisor);
                        cpu = resized;
                    }
                    Mat diff = new Mat();
                    Core.absdiff(cpu, gpu, diff);
                    assertEquals(0, Core.countNonZero(diff), "divisor " + divisor + " crop " + crop);
                }
            }
        }
    }

    @Test
    public void recordedFrameMatchesRender() {
        Mat mat = colorImage(120, 160);
        BufferedImage image = OpenCvUtils.toBufferedImage(mat);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        try (GpuCameraTransform transform = new GpuCameraTransform();
                GpuBuffer upload = new GpuBuffer(data.length, true)) {
            upload.map().put(data);
            GpuBuffer[] slots = transform.slots(upload);
            BufferedImage rendered = transform.render(slots, 0, Input.Bgr, 160, 120, 480, value -> {
            });
            GpuImage recorded;
            try (GpuRecording recording = GpuRecording.open()) {
                recorded = transform.record(slots, 0, Input.Bgr, 160, 120, 480);
            }
            Mat diff = new Mat();
            Core.absdiff(OpenCvUtils.toMat(rendered), recorded.download(), diff);
            recorded.release();
            assertEquals(0, Core.countNonZero(diff.reshape(1)));
        }
    }
}
