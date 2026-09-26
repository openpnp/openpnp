package org.openpnp.vision.gpu;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openpnp.vision.gpu.GpuPipeline.Binding;

/**
 * DetectCircularSymmetry on the GPU: either one search pass for the CPU driven search, or the whole
 * single target search with its finer passes and diagnostics recorded into a GpuRecording.
 */
public class GpuCircularSymmetry {
    private static final int STATE_HEADER = 16;
    private static final int LEVEL_SIZE = 48;
    private static final int ITERATION_RADIUS = 2;
    private static final int ITERATION_DIVISION = 4;

    private static final GpuCache<List<Object>, Tables> tables = new GpuCache<>(2, TimeUnit.MINUTES);

    private static GpuBuffer params;
    private static GpuBuffer pixelBuffer;
    private static GpuBuffer scoreBuffer;
    private static GpuBuffer radiusBuffer;
    private static GpuBuffer stateBuffer;

    public static boolean isAvailable() {
        return GpuRuntime.hasInt64();
    }

    /**
     * Pixel offsets must be grouped by (ring, channel, angle) bin, with binStart holding each bin's
     * first index and a final end index. Writes NaN scores for candidates outside the search radius.
     */
    public static synchronized void score(byte[] pixels, int[] binStart, int[] offsets, int width, int channels,
            int x0, int sub, int cols, int rows, int xSearch, int ySearch, int rSearchSq, int rDim,
            int angleDim, int mode, int r0, int minDiameter, float[] scores, int[] radii) {
        if (!isAvailable()) {
            throw new IllegalStateException("GPU with 64-bit integers is not available");
        }
        params = GpuCompute.scratch(params, 64);
        pixelBuffer = GpuCompute.scratch(pixelBuffer, pixels.length);
        scoreBuffer = GpuCompute.scratch(scoreBuffer, cols * rows * 4L);
        radiusBuffer = GpuCompute.scratch(radiusBuffer, cols * rows * 4L);
        stateBuffer = GpuCompute.scratch(stateBuffer, STATE_HEADER + LEVEL_SIZE);
        GpuProgram program = GpuCompute.program(scorePipeline(channels, mode, angleDim), (cols + 7) / 8,
                (rows + 7) / 8, 1, params, pixelBuffer, GpuCompute.constant(binStart), GpuCompute.constant(offsets),
                scoreBuffer, radiusBuffer, stateBuffer);
        ByteBuffer p = params.map();
        int[] values = { width, sub, cols, rows, rSearchSq, rDim, r0, minDiameter, binStart.length - 1, 0 };
        for (int i = 0; i < values.length; i++) {
            p.putInt(i * 4, values[i]);
        }
        ByteBuffer state = stateBuffer.map();
        int[] plan = { x0, 0, xSearch, ySearch, cols, rows, 0 };
        for (int i = 0; i < plan.length; i++) {
            state.putInt(STATE_HEADER + i * 4, plan[i]);
        }
        pixelBuffer.map().put(pixels);
        GpuRuntime.await(program.submit(), GpuCompute.TIMEOUT_NS);
        scoreBuffer.map().asFloatBuffer().get(scores, 0, cols * rows);
        radiusBuffer.map().asIntBuffer().get(radii, 0, cols * rows);
    }

    /**
     * A recorded single target search; see DetectCircularSymmetry.findCircularSymmetry().
     */
    public static final class Search {
        private final GpuRecording.Scratch state;
        private final List<Level> levels;
        private final double minSymmetry;
        private final GpuImage diagnosticImage;
        private ByteBuffer result;

        private Search(GpuRecording.Scratch state, List<Level> levels, double minSymmetry,
                GpuImage diagnosticImage) {
            this.state = state;
            this.levels = levels;
            this.minSymmetry = minSymmetry;
            this.diagnosticImage = diagnosticImage;
        }

        /**
         * The image with the heat map and indicators painted, or null without diagnostics.
         */
        public GpuImage getDiagnosticImage() {
            return diagnosticImage;
        }

        /**
         * Submits the recording if needed and returns x, y, diameter and score of the detected
         * circle, or null if the best candidate isn't symmetric enough.
         */
        public synchronized double[] result() throws Exception {
            if (result == null) {
                result = state.read();
            }
            Level last = levels.get(levels.size() - 1);
            int base = STATE_HEADER + (levels.size() - 1) * LEVEL_SIZE;
            int cols = result.getInt(base + 16);
            int rows = result.getInt(base + 20);
            int status = result.getInt(base + 24);
            int index = result.getInt(base + 28);
            float score = result.getFloat(base + 32);
            if (status != 0) {
                throw new Exception("Circular symmetry stage: search range is cropped to nothing.");
            }
            if (index < 0 || !(score > minSymmetry)) {
                return null;
            }
            int z = index / (cols * rows);
            double x = result.getInt(base + 40) + 0.5 + last.offset(z / last.superSampling);
            double y = result.getInt(base + 44) + 0.5 + last.offset(z % last.superSampling);
            return new double[] { x, y, result.getInt(base + 36) * 2, score };
        }
    }

    private static final class Level {
        final int sub;
        final int searchDiameter;
        final int searchWidth;
        final int searchHeight;
        final int superSampling;
        final int maxCols;
        final int maxRows;

        Level(int sub, int searchDiameter, int searchWidth, int searchHeight, int superSampling) {
            this.sub = sub;
            this.searchDiameter = searchDiameter;
            this.searchWidth = searchWidth;
            this.searchHeight = searchHeight;
            this.superSampling = superSampling;
            this.maxCols = (searchWidth + sub) / sub + 2;
            this.maxRows = (searchHeight + sub) / sub + 2;
        }

        double offset(int s) {
            return superSampling > 1 ? ((double) s) / superSampling - 0.5 : 0.0;
        }
    }

    private static final class Tables implements GpuResource {
        final GpuBuffer binStart;
        final GpuBuffer offsets;

        Tables(GpuBuffer binStart, GpuBuffer offsets) {
            this.binStart = binStart;
            this.offsets = offsets;
        }

        @Override
        public boolean isClosed() {
            return binStart.isClosed();
        }

        @Override
        public void close() {
            binStart.close();
            offsets.close();
        }
    }

    /**
     * Records findCircularSymmetry() for maxTargetCount 1 into the thread's GpuRecording: every
     * pass, its zoom into the previous pass' best candidate and, if wanted, the diagnostics.
     */
    public static Search search(GpuImage image, int xCenter, int yCenter, int minDiameter, int maxDiameter,
            int searchDiameter, int searchWidth, int searchHeight, double minSymmetry, int subSampling,
            int superSampling, int mode, int angleDim, boolean diagnostics, boolean heatMap) throws Exception {
        int channels = image.channels();
        int width = image.cols();
        int height = image.rows();
        minDiameter = Math.max(3, minDiameter | 1);
        maxDiameter = Math.max(minDiameter + 4, maxDiameter | 1);
        superSampling = Math.min(16, superSampling);
        List<Level> levels = new ArrayList<>();
        for (int sub = subSampling, search = searchDiameter, w = searchWidth, h = searchHeight;;) {
            int subEff = Math.max(1, Math.min(sub, Math.min((maxDiameter - minDiameter) / 4, minDiameter / 2)));
            boolean last = subEff == 1 && (search <= ITERATION_RADIUS || superSampling <= 1);
            levels.add(new Level(subEff, search, w, h, last && superSampling > 1 ? superSampling : 1));
            if (last) {
                break;
            }
            sub = subEff / ITERATION_DIVISION;
            search = w = h = subEff * ITERATION_RADIUS;
        }
        int r = maxDiameter / 2;
        int r0 = minDiameter / 2 - 1;
        checkFirstPass(levels.get(0), xCenter, yCenter, r, maxDiameter, width, height);

        Binding[] planBindings = { Binding.Uniform, Binding.Storage };
        GpuPipeline plan = GpuCompute.pipeline("symmetry_plan", new int[0], planBindings);
        GpuPipeline reduce = GpuCompute.pipeline("symmetry_reduce", new int[0], Binding.Uniform, Binding.Storage,
                Binding.Storage, Binding.Storage);
        GpuPipeline score = scorePipeline(channels, mode, angleDim);
        try (GpuRecording recording = GpuRecording.open()) {
            GpuRecording.Scratch state = recording.scratch(STATE_HEADER + (long) LEVEL_SIZE * levels.size(), true);
            GpuRecording.Scratch[] scores = new GpuRecording.Scratch[levels.size()];
            for (int k = 0; k < levels.size(); k++) {
                Level level = levels.get(k);
                int offsets = level.superSampling * level.superSampling;
                int rDim = (r - r0 + 1) / level.sub;
                int rSearch = level.searchDiameter / 2 + 1;
                int bins = angleDim * rDim * channels;
                long cells = (long) offsets * level.maxCols * level.maxRows;
                Tables table = tables(width, channels, level.sub, r, r0, rDim, angleDim, level.superSampling);
                scores[k] = recording.scratch(cells * 4, false);
                GpuRecording.Scratch radii = recording.scratch(cells * 4, false);
                recording.dispatch(plan, 1, 1, 1, GpuRecording.params(k, xCenter, yCenter, level.searchWidth,
                        level.searchHeight, level.sub, r, maxDiameter, width, height, level.maxCols, level.maxRows),
                        state);
                recording.dispatch(score, (level.maxCols + 7) / 8, (level.maxRows + 7) / 8, offsets,
                        GpuRecording.params(width, level.sub, level.maxCols, level.maxRows, rSearch * rSearch, rDim,
                                r0, minDiameter, bins, k),
                        image, table.binStart, table.offsets, scores[k], radii, state);
                recording.dispatch(reduce, 1, 1, 1,
                        GpuRecording.params(k, level.maxCols, level.maxRows, offsets, level.sub, r),
                        scores[k], radii, state);
            }
            GpuImage painted = null;
            if (diagnostics || heatMap) {
                painted = recording.image(height, width, image.type());
                recording.copy(image, painted);
                GpuPipeline paint = GpuCompute.pipeline("symmetry_paint", new int[] { channels }, Binding.Uniform,
                        Binding.Storage, Binding.Storage, Binding.Storage);
                for (int k = levels.size() - 1; k >= 0; k--) {
                    Level level = levels.get(k);
                    if (level.superSampling > 1) {
                        continue;
                    }
                    ByteBuffer params = GpuRecording.params(k, levels.size() - 1, level.sub, r, level.maxCols, width,
                            k == 0 && diagnostics ? 1 : 0, heatMap ? 1 : 0, xCenter, yCenter,
                            (maxDiameter + minDiameter) / 4);
                    params.putFloat(floatBelow(minSymmetry));
                    recording.dispatch(paint, (level.maxCols * level.sub + 15) / 16,
                            (level.maxRows * level.sub + 15) / 16, 1, params, painted, scores[k], state);
                }
            }
            return new Search(state, levels, minSymmetry, painted);
        }
    }

    private static void checkFirstPass(Level level, int xCenter, int yCenter, int r, int maxDiameter, int width,
            int height) throws Exception {
        int sub = level.sub;
        int x0 = Math.max(0, (xCenter - r - level.searchWidth / 2) / sub) * sub;
        int y0 = Math.max(0, (yCenter - r - level.searchHeight / 2) / sub) * sub;
        int x1 = Math.min((width - maxDiameter) / sub, (xCenter - r + level.searchWidth / 2 + sub / 2) / sub) * sub;
        int y1 = Math.min((height - maxDiameter) / sub, (yCenter - r + level.searchHeight / 2 + sub / 2) / sub)
                * sub;
        if (x1 - x0 < 1 || y1 - y0 < 1) {
            throw new Exception("Circular symmetry stage: search range is cropped to nothing.");
        }
    }

    // The largest float not above value, so float scores compare against it like against the double.
    private static float floatBelow(double value) {
        float f = (float) value;
        return f > value ? Math.nextDown(f) : f;
    }

    private static GpuPipeline scorePipeline(int channels, int mode, int angleDim) {
        return GpuCompute.pipeline("circular_symmetry", new int[] { channels, mode, angleDim }, Binding.Uniform,
                Binding.Storage, Binding.Storage, Binding.Storage, Binding.Storage, Binding.Storage,
                Binding.Storage);
    }

    private static Tables tables(int width, int channels, int sub, int r, int r0, int rDim, int angleDim,
            int superSampling) {
        return tables.get(Arrays.asList(width, channels, sub, r, r0, rDim, angleDim, superSampling), k -> {
            int bins = angleDim * rDim * channels;
            int offsets = superSampling * superSampling;
            int[] binStart = new int[offsets * (bins + 1)];
            List<int[]> perOffset = new ArrayList<>();
            int total = 0;
            for (int z = 0; z < offsets; z++) {
                double xOffset = superSampling > 1 ? ((double) (z / superSampling)) / superSampling - 0.5 : 0.0;
                double yOffset = superSampling > 1 ? ((double) (z % superSampling)) / superSampling - 0.5 : 0.0;
                int[][] binned = binnedSamples(width, channels, sub, r, r0, rDim, angleDim, xOffset, yOffset);
                for (int b = 0; b <= bins; b++) {
                    binStart[z * (bins + 1) + b] = total + binned[0][b];
                }
                total += binned[1].length;
                perOffset.add(binned[1]);
            }
            int[] all = new int[Math.max(1, total)];
            int next = 0;
            for (int[] part : perOffset) {
                System.arraycopy(part, 0, all, next, part.length);
                next += part.length;
            }
            GpuBuffer binStartBuffer = new GpuBuffer(binStart.length * 4L, true);
            binStartBuffer.map().asIntBuffer().put(binStart);
            GpuBuffer offsetBuffer = new GpuBuffer(all.length * 4L, true);
            offsetBuffer.map().asIntBuffer().put(all);
            return new Tables(binStartBuffer, offsetBuffer);
        });
    }

    /**
     * The pixel offsets of all ring samples around a candidate at the given sub-pixel offset,
     * grouped by (ring, channel, angle) bin: {binStart, offsets}, as findCircularSymmetry()
     * samples them.
     */
    static int[][] binnedSamples(int width, int channels, int sub, int r, int r0, int rDim, int angleDim,
            double xOffset, double yOffset) {
        int angleMask = angleDim - 1;
        double fa = angleDim / (Math.PI * 2);
        int bins = angleDim * rDim * channels;
        List<int[]> samples = new ArrayList<>();
        int[] binStart = new int[bins + 1];
        for (int y = -r, yi = 0; y <= r; y += sub, yi += sub) {
            for (int x = -r, idx = yi * width * channels; x <= r; x += sub, idx += channels * sub) {
                double dx = x - xOffset;
                double dy = y - yOffset;
                double d = Math.hypot(dx, dy);
                int idxR = (-r0 + (int) Math.round(d)) / sub;
                if (idxR >= 0 && idxR < rDim) {
                    double angle = angleMask == 0 ? 0 : Math.atan2(dy, dx);
                    int idxAngle = angleMask & (int) Math.round(angle * fa);
                    for (int ch = 0; ch < channels; ch++) {
                        int bin = (idxR * channels + ch) * angleDim + idxAngle;
                        samples.add(new int[] { idx + ch, bin });
                        binStart[bin + 1]++;
                    }
                }
            }
        }
        for (int bin = 0; bin < bins; bin++) {
            binStart[bin + 1] += binStart[bin];
        }
        int[] fill = Arrays.copyOf(binStart, bins);
        int[] offsets = new int[samples.size()];
        for (int[] sample : samples) {
            offsets[fill[sample[1]]++] = sample[0];
        }
        return new int[][] { binStart, offsets };
    }
}
