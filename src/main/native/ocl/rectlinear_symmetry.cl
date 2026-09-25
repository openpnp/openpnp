// Computes the rotated x and y cross-sections of one DetectRectlinearSymmetry pass for all angles,
// mirroring the splatting loop in DetectRectlinearSymmetry.findReclinearSymmetry(). Each work-item
// gathers one (angle, bin) by walking only the strip of pixels that can land in that bin, which keeps
// the sums deterministic without atomics.

#define MAX_CHANNELS 4

__kernel void rectlinear_cross_sections(__global const uchar *pixels, __constant float *gammaLut,
        __global const float *sines, __global const float *cosines, __global float *sums,
        __global float *weights, __global float *maskedWeights,
        int width, int channels, int x0, int sub, int cols, int rows, int cxPixels, int cyPixels,
        int wCross, int hCross, float cxCross, float cyCross, float thresholdLuminance, int angles) {
    // Neighbouring work-items take neighbouring bins, so their pixel strips are adjacent in memory.
    int j = get_global_id(0);
    int a = get_global_id(1);
    int bins = wCross + hCross;
    if (a >= angles || j >= bins) {
        return;
    }
    float s = sines[a];
    float c = cosines[a];
    bool alongX = j < wCross;
    int bin = alongX ? j : j - wCross;
    // u is the coordinate along this cross-section: u = alpha*i + beta*k + u0 for pixel column i, row k.
    float alpha = (alongX ? c : -s) * sub;
    float beta = (alongX ? s : c) * sub;
    float u0 = alongX ? -c * cxPixels - s * cyPixels + cxCross : s * cxPixels - c * cyPixels + cyCross;
    // Only pixels rounding to bin or bin + 1 contribute.
    float lo = bin - 0.5f;
    float hi = bin + 1.5f;

    float sum[MAX_CHANNELS];
    for (int ch = 0; ch < channels; ch++) {
        sum[ch] = 0.0f;
    }
    float n = 0.0f;
    float masked = 0.0f;
    bool outerRows = fabs(alpha) >= fabs(beta);
    int outerCount = outerRows ? rows : cols;
    int innerCount = outerRows ? cols : rows;
    float innerStep = outerRows ? alpha : beta;
    float outerStep = outerRows ? beta : alpha;
    for (int outer = 0; outer < outerCount; outer++) {
        float base = outerStep * outer + u0;
        float t0 = (lo - base) / innerStep;
        float t1 = (hi - base) / innerStep;
        int first = max(0, (int) floor(fmin(t0, t1)) - 1);
        int last = min(innerCount - 1, (int) ceil(fmax(t0, t1)) + 1);
        for (int inner = first; inner <= last; inner++) {
            int i = outerRows ? inner : outer;
            int k = outerRows ? outer : inner;
            float dx = i * sub - cxPixels;
            float dy = k * sub - cyPixels;
            float xCross = c * dx + s * dy + cxCross;
            float yCross = -s * dx + c * dy + cyCross;
            int ix = (int) floor(xCross + 0.5f);
            int iy = (int) floor(yCross + 0.5f);
            if (iy <= 1 || iy >= hCross || ix <= 1 || ix >= wCross) {
                continue;
            }
            float u = alongX ? xCross : yCross;
            int iu = alongX ? ix : iy;
            float w1 = u + 0.5f - iu;
            float w;
            if (iu == bin) {
                w = w1;
            }
            else if (iu == bin + 1) {
                w = 1.0f - w1;
            }
            else {
                continue;
            }
            __global const uchar *p = pixels + ((k * sub) * width + x0 + i * sub) * channels;
            float luminance = 0.0f;
            for (int ch = 0; ch < channels; ch++) {
                float v = gammaLut[p[ch]];
                luminance += v;
                sum[ch] += v * w;
            }
            n += w;
            if (luminance > thresholdLuminance) {
                masked += w;
            }
        }
    }
    int out = a * bins + j;
    for (int ch = 0; ch < channels; ch++) {
        sums[out * channels + ch] = sum[ch];
    }
    weights[out] = n;
    maskedWeights[out] = masked;
}
