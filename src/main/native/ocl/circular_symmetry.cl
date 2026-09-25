// Scores every candidate center of one DetectCircularSymmetry search pass, mirroring
// DetectCircularSymmetry.findCircularSymmetry(). Samples are grouped per (ring, channel, angle) bin.
// There is no double precision on many GPUs, so variances use exact 64-bit integer sums, or sums
// shifted by the first ring's value, to avoid float cancellation in flat (e.g. saturated) areas.

#define MODE_OVERALL 0
#define MODE_RING_AVERAGES 1
#define MODE_RING_MEDIAN 2
#define MAX_ANGLES 16
#define MAX_CHANNELS 4

__kernel void circular_symmetry(__global const uchar *pixels, __global const int *binStart,
        __global const int *offsets, __global float *scores, __global int *radii,
        int width, int channels, int x0, int sub, int cols, int rows, int xSearch, int ySearch,
        int rSearchSq, int rDim, int angleDim, int mode, int r0, int minDiameter) {
    int xis = get_global_id(0);
    int yis = get_global_id(1);
    if (xis >= cols || yis >= rows) {
        return;
    }
    int out = yis * cols + xis;
    int xi = xis * sub;
    int yi = yis * sub;
    int dx = xi - xSearch;
    int dy = yi - ySearch;
    if (dx * dx + dy * dy > rSearchSq) {
        scores[out] = NAN;
        radii[out] = 0;
        return;
    }
    __global const uchar *base = pixels + (yi * width + x0 + xi) * channels;

    float varianceRing = 0.0f;
    long sumAcross[MAX_CHANNELS];
    long sumSqAcross[MAX_CHANNELS];
    long nAcross[MAX_CHANNELS];
    float shift[MAX_CHANNELS];
    bool shifted[MAX_CHANNELS];
    float shiftedSum[MAX_CHANNELS];
    float shiftedSumSq[MAX_CHANNELS];
    float lastAvg[MAX_CHANNELS];
    for (int ch = 0; ch < channels; ch++) {
        sumAcross[ch] = 0;
        sumSqAcross[ch] = 0;
        nAcross[ch] = 0;
        shifted[ch] = false;
        shiftedSum[ch] = 0.0f;
        shiftedSumSq[ch] = 0.0f;
        lastAvg[ch] = 0.0f;
    }
    float contrastBest = -INFINITY;
    int rContrastBest = 0;
    float segments[MAX_ANGLES];

    for (int ri = 0; ri < rDim; ri++) {
        float contrast = 0.0f;
        for (int ch = 0; ch < channels; ch++) {
            long sumRing = 0;
            long sumSqRing = 0;
            int nRing = 0;
            int slots = 0;
            for (int a = 0; a < angleDim; a++) {
                int bin = (ri * channels + ch) * angleDim + a;
                int sum = 0;
                int sumSq = 0;
                int end = binStart[bin + 1];
                for (int k = binStart[bin]; k < end; k++) {
                    int p = base[offsets[k]];
                    sum += p;
                    sumSq += p * p;
                }
                int n = end - binStart[bin];
                sumRing += sum;
                sumSqRing += sumSq;
                nRing += n;
                if (mode == MODE_RING_AVERAGES && n > 0) {
                    float value = (float) sum / n;
                    if (!shifted[ch]) {
                        shift[ch] = value;
                        shifted[ch] = true;
                    }
                    float d = value - shift[ch];
                    shiftedSum[ch] += d * n;
                    shiftedSumSq[ch] += d * d * n;
                }
                else if (mode == MODE_RING_MEDIAN && n > 0) {
                    segments[slots++] = (float) sum / n;
                }
            }
            if (mode == MODE_RING_MEDIAN) {
                for (int i = 1; i < slots; i++) {
                    float v = segments[i];
                    int j = i - 1;
                    while (j >= 0 && segments[j] > v) {
                        segments[j + 1] = segments[j];
                        j--;
                    }
                    segments[j + 1] = v;
                }
                if (slots > 0) {
                    float median = (segments[max(0, slots / 2 - 1)] + segments[slots / 2]) * 0.5f;
                    if (!shifted[ch]) {
                        shift[ch] = median;
                        shifted[ch] = true;
                    }
                    float d = median - shift[ch];
                    shiftedSum[ch] += d * nRing;
                    shiftedSumSq[ch] += d * d * nRing;
                }
            }
            if (nRing == 0) {
                // The CPU code divides 0 by 0 here, which poisons the whole candidate.
                varianceRing = NAN;
                contrast = NAN;
                continue;
            }
            varianceRing += (float) ((long) nRing * sumSqRing - sumRing * sumRing) / nRing;
            sumAcross[ch] += sumRing;
            sumSqAcross[ch] += sumSqRing;
            nAcross[ch] += nRing;
            float avg = (float) sumRing / nRing;
            contrast += (lastAvg[ch] - avg) * (lastAvg[ch] - avg);
            lastAvg[ch] = avg;
        }
        if ((r0 + ri * sub) * 2 >= minDiameter && contrastBest < contrast) {
            contrastBest = contrast;
            rContrastBest = r0 + ri * sub;
        }
    }

    float varianceAcross = 0.0f;
    for (int ch = 0; ch < channels; ch++) {
        if (nAcross[ch] == 0) {
            varianceAcross = NAN;
        }
        else if (mode == MODE_OVERALL) {
            varianceAcross += (float) (nAcross[ch] * sumSqAcross[ch] - sumAcross[ch] * sumAcross[ch])
                    / nAcross[ch];
        }
        else {
            varianceAcross += shiftedSumSq[ch] - shiftedSum[ch] * shiftedSum[ch] / nAcross[ch];
        }
    }
    const float div0Guard = 0.1f;
    scores[out] = (varianceAcross + div0Guard) / (varianceRing + div0Guard);
    radii[out] = rContrastBest;
}
