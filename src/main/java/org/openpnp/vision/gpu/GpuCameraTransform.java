package org.openpnp.vision.gpu;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.LongConsumer;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.openpnp.vision.gpu.GpuPipeline.Binding;

/**
 * Converts camera frames on the GPU and applies a camera's white balance LUT and composite
 * transform remap, either at full resolution or downscaled for a preview.
 */
public class GpuCameraTransform implements AutoCloseable {
    public enum Input {
        Yuyv,
        Bgr,
        Gray
    }

    private static final int OUTPUT_PACKED = 0;
    private static final int OUTPUT_XRGB = 1;
    private static final int PARAMS_SIZE = 48;
    private static final int MAX_BOX = 4;
    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(5);

    private static final GpuCache<List<Integer>, GpuPipeline> pipelines = new GpuCache<>(10, TimeUnit.MINUTES);

    private final GpuCache<TargetKey, Target> targets = new GpuCache<>(1, TimeUnit.MINUTES);
    private GpuBuffer map;
    private GpuBuffer lut;
    private final GpuBuffer unused;
    private GpuBuffer upload;
    private int mapWidth;
    private int mapHeight;
    private boolean closed;

    private static final class TargetKey {
        final GpuBuffer source;
        final Input input;
        final int output;
        final int dstWidth;
        final int dstHeight;
        final int box;

        TargetKey(GpuBuffer source, Input input, int output, int dstWidth, int dstHeight, int box) {
            this.source = source;
            this.input = input;
            this.output = output;
            this.dstWidth = dstWidth;
            this.dstHeight = dstHeight;
            this.box = box;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TargetKey)) {
                return false;
            }
            TargetKey k = (TargetKey) o;
            return source == k.source && input == k.input && output == k.output && dstWidth == k.dstWidth
                    && dstHeight == k.dstHeight && box == k.box;
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(source), input, output, dstWidth, dstHeight, box);
        }
    }

    // A recorded program with its own parameters and output, used by one render at a time.
    private static class Target implements GpuResource {
        final GpuProgram program;
        final GpuBuffer params;
        final GpuBuffer output;

        Target(GpuProgram program, GpuBuffer params, GpuBuffer output) {
            this.program = program;
            this.params = params;
            this.output = output;
        }

        @Override
        public boolean isClosed() {
            return program.isClosed();
        }

        @Override
        public void close() {
            program.close();
            params.close();
            output.close();
        }
    }

    public GpuCameraTransform() {
        if (!GpuRuntime.isAvailable()) {
            throw new IllegalStateException("GPU is not available");
        }
        unused = new GpuBuffer(16, false);
    }

    /**
     * mapX and mapY are CV_32FC1 source coordinates for every output pixel, or null to keep the
     * input geometry. lut is a 256x1 CV_8UC3 table applied before the remap, or null.
     */
    public synchronized void setTransform(Mat mapX, Mat mapY, Mat lut) {
        checkOpen();
        targets.clear();
        if (map != null) {
            map.close();
            map = null;
        }
        if (this.lut != null) {
            this.lut.close();
            this.lut = null;
        }
        if (mapX != null) {
            mapWidth = mapX.cols();
            mapHeight = mapX.rows();
            float[] xs = new float[mapWidth * mapHeight];
            float[] ys = new float[xs.length];
            mapX.get(0, 0, xs);
            mapY.get(0, 0, ys);
            map = new GpuBuffer(xs.length * 8L, true);
            ByteBuffer mapped = map.map();
            for (int i = 0; i < xs.length; i++) {
                mapped.putFloat(i * 8, xs[i]);
                mapped.putFloat(i * 8 + 4, ys[i]);
            }
        }
        if (lut != null) {
            if (lut.type() != CvType.CV_8UC3 || lut.total() != 256) {
                throw new IllegalArgumentException("LUT must be 256 CV_8UC3 entries");
            }
            byte[] table = new byte[256 * 3];
            lut.get(0, 0, table);
            this.lut = new GpuBuffer(256 * 4, true);
            ByteBuffer mapped = this.lut.map();
            for (int i = 0; i < 256; i++) {
                mapped.putInt(i * 4, (table[i * 3] & 0xFF) | (table[i * 3 + 1] & 0xFF) << 8
                        | (table[i * 3 + 2] & 0xFF) << 16);
            }
        }
    }

    /**
     * The width and height of full resolution renders of a width x height input.
     */
    public synchronized int[] outputSize(int width, int height) {
        return map != null ? new int[] { mapWidth, mapHeight } : new int[] { width, height };
    }

    /**
     * Renders a frame from a GPU buffer at full resolution. submitted receives the GPU value to
     * wait for before the source may be overwritten.
     */
    public synchronized BufferedImage render(GpuBuffer source, Input input, int width, int height, int stride,
            LongConsumer submitted) {
        checkOpen();
        int dstWidth = map != null ? mapWidth : width;
        int dstHeight = map != null ? mapHeight : height;
        BufferedImage image = new BufferedImage(dstWidth, dstHeight,
                input == Input.Gray ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_3BYTE_BGR);
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Target target = target(new TargetKey(source, input, OUTPUT_PACKED, dstWidth, dstHeight, 1));
        run(target, width, height, stride, dstWidth, dstHeight, 1, 1, submitted);
        target.output.map().get(data);
        return image;
    }

    /**
     * Renders a frame downscaled (or upscaled) into a TYPE_INT_RGB image of any size.
     */
    public synchronized void renderPreview(GpuBuffer source, Input input, int width, int height, int stride,
            BufferedImage preview, LongConsumer submitted) {
        checkOpen();
        int fullWidth = map != null ? mapWidth : width;
        int fullHeight = map != null ? mapHeight : height;
        int dstWidth = preview.getWidth();
        int dstHeight = preview.getHeight();
        float scaleX = (float) fullWidth / dstWidth;
        float scaleY = (float) fullHeight / dstHeight;
        int box = Math.max(1, Math.min(MAX_BOX, (int) Math.ceil(Math.max(scaleX, scaleY))));
        Target target = target(new TargetKey(source, input, OUTPUT_XRGB, dstWidth, dstHeight, box));
        run(target, width, height, stride, dstWidth, dstHeight, scaleX, scaleY, submitted);
        target.output.map().asIntBuffer().get(((DataBufferInt) preview.getRaster().getDataBuffer()).getData());
    }

    /**
     * Transforms a TYPE_3BYTE_BGR or TYPE_BYTE_GRAY image.
     */
    public synchronized BufferedImage apply(BufferedImage image) {
        checkOpen();
        Input input;
        int stride;
        if (image.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            input = Input.Bgr;
            stride = image.getWidth() * 3;
        }
        else if (image.getType() == BufferedImage.TYPE_BYTE_GRAY) {
            input = Input.Gray;
            stride = image.getWidth();
        }
        else {
            throw new IllegalArgumentException("Unsupported image type " + image.getType());
        }
        byte[] src = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        long size = (src.length + 3) & ~3L;
        if (upload == null || upload.getSize() != size) {
            if (upload != null) {
                upload.close();
            }
            targets.clear();
            upload = new GpuBuffer(size, true);
        }
        upload.map().put(src);
        return render(upload, input, image.getWidth(), image.getHeight(), stride, value -> {
        });
    }

    private Target target(TargetKey key) {
        return targets.get(key, k -> {
            int pixels = k.dstWidth * k.dstHeight;
            long outputSize = k.output == OUTPUT_XRGB ? pixels * 4L
                    : ((pixels + 3) / 4) * (k.input == Input.Gray ? 4L : 12L);
            int invocations = k.output == OUTPUT_XRGB ? pixels : (pixels + 3) / 4;
            GpuPipeline pipeline = pipeline(k.input, k.output, k.box);
            GpuBuffer params = new GpuBuffer(PARAMS_SIZE, true);
            GpuBuffer output = new GpuBuffer(outputSize, true);
            GpuProgram program = new GpuProgram.Builder()
                    .dispatch(pipeline, (invocations + 63) / 64, 1, 1, params, k.source,
                            map != null ? map : unused, lut != null ? lut : unused, output)
                    .build();
            return new Target(program, params, output);
        });
    }

    private GpuPipeline pipeline(Input input, int output, int box) {
        boolean hasMap = map != null;
        boolean hasLut = lut != null && input != Input.Gray;
        List<Integer> spec = Arrays.asList(input.ordinal(), output, hasMap ? 1 : 0, hasLut ? 1 : 0, box);
        return pipelines.get(spec, s -> new GpuPipeline("camera_transform", s.stream().mapToInt(Integer::intValue)
                .toArray(), Binding.Uniform, Binding.Storage, Binding.Storage, Binding.Storage, Binding.Storage));
    }

    private void run(Target target, int width, int height, int stride, int dstWidth, int dstHeight, float scaleX,
            float scaleY, LongConsumer submitted) {
        target.params.map()
                .putInt(0, width)
                .putInt(4, height)
                .putInt(8, stride)
                .putInt(12, dstWidth)
                .putInt(16, dstHeight)
                .putInt(20, mapWidth)
                .putInt(24, mapHeight)
                .putFloat(32, scaleX)
                .putFloat(36, scaleY);
        long value = target.program.submit();
        submitted.accept(value);
        GpuRuntime.await(value, TIMEOUT_NS);
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    private void checkOpen() {
        if (closed) {
            throw new IllegalStateException("closed");
        }
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;
            targets.clear();
            unused.close();
            for (GpuBuffer buffer : new GpuBuffer[] { map, lut, upload }) {
                if (buffer != null) {
                    buffer.close();
                }
            }
        }
    }
}
