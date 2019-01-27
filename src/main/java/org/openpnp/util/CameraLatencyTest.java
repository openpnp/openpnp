package org.openpnp.util;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpnp.capture.CaptureDevice;
import org.openpnp.capture.CaptureFormat;
import org.openpnp.capture.CaptureProperty;
import org.openpnp.capture.CaptureStream;
import org.openpnp.capture.OpenPnpCapture;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class CameraLatencyTest extends Frame implements Runnable {
    CaptureStream stream = null;
    BufferedImage image = null;
    long t = System.currentTimeMillis();
    double fps = 0;
    long latency = 0;
    int flashCount = 0;
    double latencies[] = new double[60];
    int latencyCount = 0;
    double averageLatency = 0;
    
    public CameraLatencyTest(CaptureStream stream) throws Exception {
        this.stream = stream;
        while (!stream.hasNewFrame()) {
            
        }
        BufferedImage image = stream.capture();
        int width = image.getWidth();
        int height = image.getHeight();
        setSize(width, height);
        new Thread(this).start();
        setVisible(true);
        addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void windowIconified(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void windowDeiconified(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void windowDeactivated(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
            
            @Override
            public void windowClosed(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void windowActivated(WindowEvent e) {
                // TODO Auto-generated method stub
                
            }
        });
    }
    
    public void run() {
        double time = System.currentTimeMillis();
        double frames = 0;
        while (true) {
            try {
                while (!stream.hasNewFrame()) {
                    
                }
                image = stream.capture();
                frames++;
                double elapsed = System.currentTimeMillis() - time;
                if (elapsed > 1500) {                    
                    fps = 1000. / (elapsed / frames);
                    frames = 0;
                    time = System.currentTimeMillis();
                }
                repaint();
            }
            catch (Exception e) {
                
            }
        }
    }
    
    @Override
    public void paint(Graphics g) {
        BufferedImage image = this.image;
        
        // TODO only send the QR code 4x a second or so instead of on every frame to reduce
        // load
        
        long time = System.currentTimeMillis(); 
        try {
            long t = System.currentTimeMillis();
            Map hintMap = new HashMap();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                    new BufferedImageLuminanceSource(image)));
            Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap,
                    hintMap);
            latency = time - Long.parseLong(qrCodeResult.getText());
            System.arraycopy(latencies, 1, latencies, 0, latencies.length - 1);
            latencies[latencies.length - 1] = latency;
            averageLatency = 0;
            for (int i = 0; i < latencies.length; i++) {
                averageLatency += latencies[i];
            }
            averageLatency /= latencies.length;
            flashCount = 6;
        }
        catch (Exception e) {
        }
        
        g.setColor(Color.black);
        g.fillRect(0,  0, getWidth(), getHeight());
        g.setColor(Color.white);
        g.drawImage(image,  0,  0,  null);
        g.drawString("Time: " + (System.currentTimeMillis() - t), 10, 40);
        g.drawString("FPS: " + fps, 10, 60);
        if (flashCount-- > 0) {
            g.setColor(Color.green);
        }
        g.drawString("Latency: " + latency, 10, 80);
        g.drawString(String.format("Average Latency: %.2f", averageLatency), 10, 100);
        try {
            Map hintMap = new HashMap();
            hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            BitMatrix matrix = new MultiFormatWriter().encode("" + System.currentTimeMillis(),
                    BarcodeFormat.QR_CODE, 64, 64, hintMap);
            BufferedImage qrCode = MatrixToImageWriter.toBufferedImage(matrix);
            g.drawImage(qrCode, 20, 120, null);
        }
        catch (Exception e) {

        }
    }

    public static void main(String[] args) throws Exception {
        OpenPnpCapture capture = new OpenPnpCapture();
        List<CaptureDevice> devices = capture.getDevices();
//        for (int i = 0; i < devices.size(); i++) {
//            CaptureDevice device = devices.get(i);
//            System.out.println(String.format("%d: %s", i, device.getName()));
//            List<CaptureFormat> formats = device.getFormats();
//            for (int j = 0; j < formats.size(); j++) {
//                CaptureFormat format = formats.get(j);
//                System.out.println(String.format("  %d: %s", j, format.toString()));
//            }
//        }
        
        CaptureDevice device = devices.get(0);
        CaptureFormat format = device.getFormats().get(7);
        CaptureStream stream = device.openStream(format);
        stream.setAutoProperty(CaptureProperty.Exposure, true);
        stream.setAutoProperty(CaptureProperty.WhiteBalance, true);
        
        new CameraLatencyTest(stream);
    }
}
