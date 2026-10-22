package org.openpnp.vision.gpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.util.Random;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openpnp.util.OpenCvUtils;
import org.openpnp.vision.gpu.GpuCameraTransform.Input;

public class GpuCameraTransformTest {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    @BeforeAll
    public static void setup() {
        nu.pattern.OpenCV.loadLocally();
        Assumptions.assumeTrue(GpuRuntime.isAvailable(), "no Vulkan GPU");
    }

    private static byte[] yuyvFrame(int stride) {
        byte[] data = new byte[stride * HEIGHT];
        Random random = new Random(2);
        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x += 2) {
                int i = y * stride + x * 2;
                data[i] = (byte) random.nextInt(256);
                data[i + 1] = (byte) ((x * 255 / WIDTH + random.nextInt(8)) & 0xFF);
                data[i + 2] = (byte) random.nextInt(256);
                data[i + 3] = (byte) ((y * 255 / HEIGHT + random.nextInt(8)) & 0xFF);
            }
        }
        return data;
    }

    private static GpuBuffer upload(byte[] data) {
        GpuBuffer buffer = new GpuBuffer((data.length + 3) & ~3, true);
        buffer.map().put(data);
        return buffer;
    }

    private static Mat bgrImage() {
        Mat mat = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3);
        byte[] data = new byte[HEIGHT * WIDTH * 3];
        Random random = new Random(1);
        for (int y = 0, i = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                boolean square = ((x / 40) + (y / 40)) % 2 == 0;
                data[i++] = (byte) (x * 255 / WIDTH);
                data[i++] = (byte) (y * 255 / HEIGHT);
                data[i++] = (byte) ((square ? 200 : 40) + random.nextInt(16));
            }
        }
        mat.put(0, 0, data);
        return mat;
    }

    private static double meanDifference(Mat a, Mat b) {
        Mat diff = new Mat();
        Core.absdiff(a, b, diff);
        return Core.mean(diff.reshape(1)).val[0];
    }

    private static double maxDifference(Mat a, Mat b) {
        Mat diff = new Mat();
        Core.absdiff(a, b, diff);
        return Core.minMaxLoc(diff.reshape(1)).maxVal;
    }

    private static Mat xrgbToBgr(BufferedImage preview) {
        int[] pixels = ((DataBufferInt) preview.getRaster().getDataBuffer()).getData();
        byte[] bgr = new byte[pixels.length * 3];
        for (int i = 0; i < pixels.length; i++) {
            bgr[i * 3] = (byte) pixels[i];
            bgr[i * 3 + 1] = (byte) (pixels[i] >> 8);
            bgr[i * 3 + 2] = (byte) (pixels[i] >> 16);
        }
        Mat mat = new Mat(preview.getHeight(), preview.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, bgr);
        return mat;
    }

    @Test
    public void yuyvMatchesOpenCv() {
        int stride = WIDTH * 2 + 64;
        byte[] frame = yuyvFrame(stride);
        Mat yuyv = new Mat(HEIGHT, WIDTH, CvType.CV_8UC2);
        for (int y = 0; y < HEIGHT; y++) {
            yuyv.put(y, 0, java.util.Arrays.copyOfRange(frame, y * stride, y * stride + WIDTH * 2));
        }
        Mat expected = new Mat();
        Imgproc.cvtColor(yuyv, expected, Imgproc.COLOR_YUV2BGR_YUYV);
        try (GpuCameraTransform transform = new GpuCameraTransform(); GpuBuffer source = upload(frame)) {
            BufferedImage image = transform.render(transform.slots(source), 0, Input.Yuyv, WIDTH, HEIGHT, stride, value -> {
            });
            assertEquals(BufferedImage.TYPE_3BYTE_BGR, image.getType());
            assertEquals(0, maxDifference(expected, OpenCvUtils.toMat(image)));
        }
    }

    @Test
    public void whiteBalanceLutAppliesPerChannel() {
        Mat lut = new Mat(1, 256, CvType.CV_8UC3);
        byte[] table = new byte[256 * 3];
        for (int i = 0; i < 256; i++) {
            table[i * 3] = (byte) (255 - i);
            table[i * 3 + 1] = (byte) (i / 2);
            table[i * 3 + 2] = (byte) Math.min(255, i * 2);
        }
        lut.put(0, 0, table);
        Mat image = bgrImage();
        Mat expected = new Mat();
        Core.LUT(image, lut, expected);
        try (GpuCameraTransform transform = new GpuCameraTransform()) {
            transform.setTransform(null, null, lut);
            BufferedImage result = transform.apply(OpenCvUtils.toBufferedImage(image));
            assertEquals(0, maxDifference(expected, OpenCvUtils.toMat(result)));
        }
    }

    @Test
    public void grayStaysGray() {
        Mat gray = new Mat();
        Imgproc.cvtColor(bgrImage(), gray, Imgproc.COLOR_BGR2GRAY);
        try (GpuCameraTransform transform = new GpuCameraTransform()) {
            BufferedImage result = transform.apply(OpenCvUtils.toBufferedImage(gray));
            assertEquals(BufferedImage.TYPE_BYTE_GRAY, result.getType());
            assertEquals(0, maxDifference(gray, OpenCvUtils.toMat(result)));
        }
    }

    @Test
    public void previewDownscalesLikeAreaResize() {
        Mat image = bgrImage();
        BufferedImage source = OpenCvUtils.toBufferedImage(image);
        byte[] bytes = ((DataBufferByte) source.getRaster().getDataBuffer()).getData();
        for (int[] size : new int[][] { { 320, 240 }, { 213, 160 }, { 1000, 750 } }) {
            Mat expected = new Mat();
            Imgproc.resize(image, expected, new Size(size[0], size[1]), 0, 0,
                    size[0] < WIDTH ? Imgproc.INTER_AREA : Imgproc.INTER_LINEAR);
            BufferedImage preview = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_RGB);
            try (GpuCameraTransform transform = new GpuCameraTransform(); GpuBuffer upload = upload(bytes)) {
                transform.renderPreview(transform.slots(upload), 0, Input.Bgr, WIDTH, HEIGHT, WIDTH * 3, preview, value -> {
                });
            }
            double mean = meanDifference(expected, xrgbToBgr(preview));
            assertTrue(mean < 3, size[0] + "x" + size[1] + " mean difference " + mean);
        }
    }

    @Test
    public void previewRegionAtFullScaleIsExact() {
        Mat image = bgrImage();
        BufferedImage source = OpenCvUtils.toBufferedImage(image);
        byte[] bytes = ((DataBufferByte) source.getRaster().getDataBuffer()).getData();
        BufferedImage preview = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);
        try (GpuCameraTransform transform = new GpuCameraTransform(); GpuBuffer upload = upload(bytes)) {
            transform.renderPreview(transform.slots(upload), 0, Input.Bgr, WIDTH, HEIGHT, WIDTH * 3, preview, 123, 77,
                    200, 150, value -> {
                    });
        }
        Mat expected = image.submat(77, 227, 123, 323);
        assertEquals(0, meanDifference(expected, xrgbToBgr(preview)));
    }

    @Test
    public void zoomedPreviewMatchesUpscaledRegion() {
        Mat image = bgrImage();
        BufferedImage source = OpenCvUtils.toBufferedImage(image);
        byte[] bytes = ((DataBufferByte) source.getRaster().getDataBuffer()).getData();
        BufferedImage preview = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        try (GpuCameraTransform transform = new GpuCameraTransform(); GpuBuffer upload = upload(bytes)) {
            transform.renderPreview(transform.slots(upload), 0, Input.Bgr, WIDTH, HEIGHT, WIDTH * 3, preview, 100, 80,
                    100, 75, value -> {
                    });
        }
        Mat expected = new Mat();
        Imgproc.resize(image.submat(80, 155, 100, 200), expected, new Size(400, 300), 0, 0, Imgproc.INTER_LINEAR);
        double mean = meanDifference(expected.submat(4, 296, 4, 396), xrgbToBgr(preview).submat(4, 296, 4, 396));
        assertTrue(mean < 1.5, "mean difference " + mean);
    }

    @Test
    public void previewFollowsTheRemap() {
        Mat image = bgrImage();
        Mat mapX = new Mat(400, 500, CvType.CV_32FC1);
        Mat mapY = new Mat(400, 500, CvType.CV_32FC1);
        float[] xs = new float[400 * 500];
        float[] ys = new float[xs.length];
        for (int y = 0, i = 0; y < 400; y++) {
            for (int x = 0; x < 500; x++, i++) {
                xs[i] = WIDTH - 1 - (x * 1.1f + 20);
                ys[i] = y < 20 ? -1e6f : y * 1.05f;
            }
        }
        mapX.put(0, 0, xs);
        mapY.put(0, 0, ys);
        try (GpuCameraTransform transform = new GpuCameraTransform()) {
            transform.setTransform(mapX, mapY, null);
            BufferedImage full = transform.apply(OpenCvUtils.toBufferedImage(image));
            assertEquals(500, full.getWidth());
            assertEquals(400, full.getHeight());
            Mat expected = new Mat();
            Imgproc.remap(image, expected, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT);
            assertTrue(maxDifference(expected, OpenCvUtils.toMat(full)) <= 1);

            BufferedImage source = OpenCvUtils.toBufferedImage(image);
            byte[] bytes = ((DataBufferByte) source.getRaster().getDataBuffer()).getData();
            BufferedImage preview = new BufferedImage(250, 200, BufferedImage.TYPE_INT_RGB);
            try (GpuBuffer upload = upload(bytes)) {
                transform.renderPreview(transform.slots(upload), 0, Input.Bgr, WIDTH, HEIGHT, WIDTH * 3, preview, value -> {
                });
            }
            Mat halved = new Mat();
            Imgproc.resize(expected, halved, new Size(250, 200), 0, 0, Imgproc.INTER_AREA);
            double mean = meanDifference(halved, xrgbToBgr(preview));
            assertTrue(mean < 3, "mean difference " + mean);
        }
    }
}
