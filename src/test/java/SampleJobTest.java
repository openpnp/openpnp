import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import org.jcodec.api.awt.SequenceEncoder;
import org.junit.Test;
import org.openpnp.CameraListener;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.spi.Camera;
import org.openpnp.spi.JobProcessor;

import com.google.common.io.Files;

public class SampleJobTest {


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
        camera.setSettleTimeMs(0);
        // File videoFile = new File("target");
        // videoFile = new File(videoFile, "SampleJobTest.mp4");
        // MpegEncodingCameraListener encoder = new MpegEncodingCameraListener(videoFile);
        // camera.startContinuousCapture(encoder, 25);

        JobProcessor jobProcessor = machine.getPnpJobProcessor();
        jobProcessor.addTextStatusListener((text) -> {
            System.out.println(text);
        });

        File jobFile = new File("samples");
        jobFile = new File(jobFile, "pnp-test");
        jobFile = new File(jobFile, "pnp-test.job.xml");
        Job job = Configuration.get().loadJob(jobFile);

        machine.setEnabled(true);
        jobProcessor.initialize(job);
        while (jobProcessor.next());
        // camera.stopContinuousCapture(encoder);
        // encoder.finish();
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
