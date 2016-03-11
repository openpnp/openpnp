package org.openpnp.vision.pipeline.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import org.opencv.core.Mat;
import org.openpnp.gui.components.reticle.Reticle;
import org.openpnp.util.OpenCvUtils;

public class MatView extends JComponent {
    private BufferedImage image;
    
    public MatView() {
        setBackground(Color.black);
    }
    
    public void setMat(Mat mat) {
        if (mat == null) {
            image = null;
        }
        else {
            image = OpenCvUtils.toBufferedImage(mat);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image == null) {
            return;
        }
        
        Insets ins = getInsets();
        int sourceWidth = image.getWidth();
        int sourceHeight = image.getHeight();
        int width = getWidth() - ins.left - ins.right;
        int height = getHeight() - ins.top - ins.bottom;

        double destWidth = width, destHeight = height;

        double widthRatio = sourceWidth / destWidth;
        double heightRatio = sourceHeight / destHeight;

        int scaledHeight, scaledWidth;
        
        if (heightRatio > widthRatio) {
            double aspectRatio = sourceWidth / sourceHeight;
            scaledHeight = (int) destHeight;
            scaledWidth = (int) (scaledHeight * aspectRatio);
        }
        else {
            double aspectRatio = sourceHeight / sourceWidth;
            scaledWidth = (int) destWidth;
            scaledHeight = (int) (scaledWidth * aspectRatio);
        }

        int imageX = (int) (ins.left + (width / 2) - (scaledWidth / 2));
        int imageY = (int) (ins.top + (height / 2) - (scaledHeight / 2));

        Graphics2D g2d = (Graphics2D) g;

        g2d.drawImage(image, imageX, imageY, scaledWidth, scaledHeight, null);
    }
}
