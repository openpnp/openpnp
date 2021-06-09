import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;

import org.apache.commons.io.FileUtils;
import org.jcodec.api.awt.SequenceEncoder;
import org.junit.jupiter.api.Test;
import org.openpnp.CameraListener;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferencePnpJobProcessor;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.spi.Axis;
import org.openpnp.spi.base.AbstractCamera;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;

import com.google.common.io.Files;

public class SampleJobTest {

    /**
     * Enable imperfectMachine for a full "imperfect machine" test. The poor machine has bad non-squareness,
     * huge nozzle runout, is off from home, the camera has lag and bad vibration simulated from effective motion.
     * Unlike the default machine, it also uses the Z axis.
     * 
     * This tests non-squareness compensation (axis transforms), runout compensation, visual homing, camera settling,
     * Z motion, 3rd order (jerk controlled) motion planning and prediction, as well as its benefit against vibration.
     * 
     * It also uses "intelligent" pick and place location detection based on the ImageCamera's machine table image.
     * So if the simulation tries to pick or place at the wrong location, the test fails.
     * 
     * Unfortunately, it is terribly slow as some aspects (camera settling/vibration) need to be simulated in 
     * quasi real-time to be conclusive as a test. Takes about 2 min.
     * 
     */
    final public static boolean imperfectMachine = true; 

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

        if (imperfectMachine) {
            // Take the imperfect machine as a test case.
            FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/SampleJobTest/machine.xml"),
                new File(workingDirectory, "machine.xml"));
        }

        Configurator
        .currentConfig()
        .level(Level.INFO) // change this for other log levels.
        .activate();

        Configuration.initialize(workingDirectory);
        Configuration.get().load();
        
        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();

        if (!imperfectMachine) {
            NullDriver driver = (NullDriver) machine.getDefaultDriver();
            // Make it faster for the test (now including axes).
            driver.setFeedRateMmPerMinute(0);
            for (Axis axis : machine.getAxes()) {
                if (axis instanceof ReferenceControllerAxis) {
                    ((ReferenceControllerAxis) axis).setFeedratePerSecond(new Length(1000000, LengthUnit.Millimeters));
                    ((ReferenceControllerAxis) axis).setAccelerationPerSecond2(new Length(2000000, LengthUnit.Millimeters));
                    ((ReferenceControllerAxis) axis).setJerkPerSecond3(new Length(0, LengthUnit.Millimeters));
                }
            }

            AbstractCamera camera = (AbstractCamera)machine.getDefaultHead().getDefaultCamera();
            camera.setSettleMethod(AbstractCamera.SettleMethod.FixedTime);
            camera.setSettleTimeMs(0);
        }


        // File videoFile = new File("target");
        // videoFile = new File(videoFile, "SampleJobTest.mp4");
        // MpegEncodingCameraListener encoder = new MpegEncodingCameraListener(videoFile);
        // camera.startContinuousCapture(encoder, 25);

        ReferencePnpJobProcessor jobProcessor = (ReferencePnpJobProcessor) machine.getPnpJobProcessor();
        jobProcessor.addTextStatusListener((text) -> {
            System.out.println(text);
        });

        File jobFile = new File("samples");
        jobFile = new File(jobFile, "pnp-test");
        jobFile = new File(jobFile, "pnp-test.job.xml");
        Job job = Configuration.get().loadJob(jobFile);

        machine.setEnabled(true);
        machine.execute(() -> {
            machine.home();
            jobProcessor.initialize(job);
            while (jobProcessor.next());
            return null;
        });
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
