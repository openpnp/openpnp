package org.openpnp;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.openpnp.capture.CaptureDevice;
import org.openpnp.capture.CaptureFormat;
import org.openpnp.capture.CaptureProperty;
import org.openpnp.capture.CaptureStream;
import org.openpnp.capture.OpenPnpCapture;

public class CaptureTest extends JPanel implements Runnable {
    CaptureStream stream = null;
    BufferedImage image = null;
    
    public CaptureTest(CaptureStream stream) throws Exception {
        this.stream = stream;
        setOpaque(true);
        new Thread(this).start();
    }
    
    public void run() {
        while (true) {
            repaint();
            try {
                Thread.sleep(1000 / 10);
            }
            catch (Exception e) {
                
            }
        }
    }
    
    @Override
    public void paintComponent(Graphics g) {
        if (stream.hasNewFrame()) {
            try {
                BufferedImage image = stream.capture();
                g.drawImage(image,  0,  0,  null);
            }
            catch (Exception e) {
                
            }
        }
    }

    public static void main(String[] args) throws Exception {
        OpenPnpCapture capture = new OpenPnpCapture();
        List<CaptureDevice> devices = capture.getDevices();
        
        for (int i = 0; i < devices.size(); i++) {
            CaptureDevice device = devices.get(i);
            System.out.println(String.format("%d: %s", i, device.getName()));
            List<CaptureFormat> formats = device.getFormats();
            for (int j = 0; j < formats.size(); j++) {
                CaptureFormat format = formats.get(j);
                System.out.println(String.format("  %d: %s", j, format.toString()));
            }
        }
        
        CaptureDevice device = devices.get(0);
        CaptureFormat format = device.getFormats().get(7);
        CaptureStream stream = device.openStream(format);
        stream.setAutoProperty(CaptureProperty.Exposure, true);
        stream.setAutoProperty(CaptureProperty.WhiteBalance, true);
        stream.setProperty(CaptureProperty.Saturation, stream.getPropertyLimits(CaptureProperty.Saturation).getDefault());

        while (!stream.hasNewFrame()) {
            
        }
        BufferedImage image = stream.capture();
        int width = image.getWidth();
        int height = image.getHeight();

        JFrame frame = new JFrame();
        frame.setSize(width, height);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(new CaptureTest(stream), BorderLayout.CENTER);
        frame.setVisible(true);
    }
}
