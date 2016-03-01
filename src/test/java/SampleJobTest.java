import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import org.jcodec.api.awt.SequenceEncoder;
import org.junit.Test;
import org.openpnp.CameraListener;
import org.openpnp.JobProcessorDelegate;
import org.openpnp.JobProcessorListener;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.machine.reference.driver.test.TestDriver;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.JobProcessor;
import org.openpnp.spi.JobProcessor.JobError;
import org.openpnp.spi.JobProcessor.JobState;
import org.openpnp.spi.JobProcessor.PickRetryAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

public class SampleJobTest {
    private final static Logger logger = LoggerFactory.getLogger(TestDriver.class);

    /**
     * Loads the pnp-test job that is included in the samples and attempts to run it within a test
     * harness. The job is expected to complete successfully without throwing any exceptions.
     * 
     * This test is intended to exercise the basic job processing functions, image processing,
     * vision, feeder handling and fiducial handling. It's intended to act as a smoke test for large
     * changes.
     */
    @Test
    public void testSampleJob() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();

        NullDriver driver = (NullDriver) machine.getDriver();
        driver.setFeedRateMmPerMinute(0);

        Camera camera = machine.getDefaultHead().getDefaultCamera();
        // File videoFile = new File("target");
        // videoFile = new File(videoFile, "SampleJobTest.mp4");
        // MpegEncodingCameraListener encoder = new MpegEncodingCameraListener(videoFile);
        // camera.startContinuousCapture(encoder, 25);

        TestCompleteNotifier notifier = new TestCompleteNotifier();

        JobProcessor jobProcessor = machine.getJobProcessors().get(JobProcessor.Type.PickAndPlace);
        jobProcessor.addListener(new SampleJobTestProcessorListener(notifier));
        jobProcessor.setDelegate(new SampleJobTestJobProcessorDelegate());

        File jobFile = new File("samples");
        jobFile = new File(jobFile, "pnp-test");
        jobFile = new File(jobFile, "pnp-test.job.xml");
        Job job = Configuration.get().loadJob(jobFile);

        jobProcessor.load(job);
        machine.setEnabled(true);
        synchronized (notifier) {
            jobProcessor.start();
            notifier.wait();
        }
        // camera.stopContinuousCapture(encoder);
        // encoder.finish();
        if (notifier.failed) {
            throw notifier.exception;
        }
    }

    public static class SampleJobTestJobProcessorDelegate implements JobProcessorDelegate {
        @Override
        public PickRetryAction partPickFailed(BoardLocation board, Part part, Feeder feeder) {
            return null;
        }
    }

    public static class SampleJobTestProcessorListener extends JobProcessorListener.Adapter {
        final private TestCompleteNotifier notifier;

        public SampleJobTestProcessorListener(TestCompleteNotifier notifier) {
            this.notifier = notifier;
        }

        @Override
        public void jobLoaded(Job job) {}

        @Override
        public void jobStateChanged(JobState state) {
            if (state == JobState.Stopped) {
                synchronized (notifier) {
                    notifier.notifyAll();
                }
            }
        }

        @Override
        public void jobEncounteredError(JobError error, String description) {
            synchronized (notifier) {
                notifier.failed = true;
                notifier.exception = new Exception(error + " " + description);
                notifier.notifyAll();
            }
        }

        @Override
        public void partProcessingStarted(BoardLocation board, Placement placement) {
            logger.info("Start " + placement.getId());
        }

        @Override
        public void partPicked(BoardLocation board, Placement placement) {}

        @Override
        public void partPlaced(BoardLocation board, Placement placement) {}

        @Override
        public void partProcessingCompleted(BoardLocation board, Placement placement) {
            logger.info("Finish " + placement.getId());
        }

        @Override
        public void detailedStatusUpdated(String status) {}
    }

    public static class TestCompleteNotifier {
        public boolean failed;
        public Exception exception;
    }

    public static class MpegEncodingCameraListener implements CameraListener {
        private SequenceEncoder enc;
        private boolean finished = false;

        public MpegEncodingCameraListener(File file) throws Exception {
            enc = new SequenceEncoder(file);
        }

        @Override
        public synchronized void frameReceived(BufferedImage img) {
            if (finished) {
                return;
            }
            try {
                Graphics g = img.getGraphics();
                g.setColor(Color.white);
                g.drawLine(0, img.getHeight() / 2, img.getWidth(), img.getHeight() / 2);
                g.drawLine(img.getWidth() / 2, 0, img.getWidth() / 2, img.getHeight());
                g.dispose();
                enc.encodeImage(img);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        public synchronized void finish() throws Exception {
            finished = true;
            enc.finish();
        }
    }
}
