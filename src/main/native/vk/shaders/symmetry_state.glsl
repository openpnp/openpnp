// The search window and outcome of each circular symmetry pass, written by the plan and reduce
// kernels so later passes can zoom in without a CPU round trip.
struct Level {
    int x0;
    int y0;
    int xSearch;
    int ySearch;
    int cols;
    int rows;
    int status;
    int index;
    float score;
    int radius;
    int xBase;
    int yBase;
};

const int STATUS_OK = 0;
const int STATUS_CROPPED = 1;

layout(binding = STATE_BINDING) buffer State {
    float rangeMin;
    float rangeMax;
    float rangeSum;
    int rangeN;
    Level levels[];
} state;
