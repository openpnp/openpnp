package org.openpnp.vision.pipeline.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;

import org.opencv.core.Mat;
import org.openpnp.util.OpenCvUtils;

public class MatView extends JComponent {
    private BufferedImage image;

    public MatView() {
        setBackground(Color.black);
    }

    public void setMat(Mat mat) {
        if (mat == null || mat.empty()) {
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
        double sourceWidth = image.getWidth();
        double sourceHeight = image.getHeight();
        double destWidth = getWidth() - ins.left - ins.right;
        double destHeight = getHeight() - ins.top - ins.bottom;

        /**
         * We want to fit both axes in the given destWidth and destHeight while maintaining the
         * aspect ratio. If the frame is smaller in either or both axes than the original will need
         * to be scaled to fill the space as completely as possible while still maintaining the
         * aspect ratio. 1. Determine the source size of the image: sourceWidth, sourceHeight. 2.
         * Determine the max size each axis can be: destWidth, destHeight. 3. Calculate how much
         * each axis needs to be scaled to fit. 4. Use the larger of the two and scale the opposite
         * axis by the aspect ratio + the scaling ratio.
         */

        double widthRatio = sourceWidth / destWidth;
        double heightRatio = sourceHeight / destHeight;

        double scaledHeight, scaledWidth;

        if (heightRatio > widthRatio) {
            double aspectRatio = sourceWidth / sourceHeight;
            scaledHeight = destHeight;
            scaledWidth = (scaledHeight * aspectRatio);
        }
        else {
            double aspectRatio = sourceHeight / sourceWidth;
            scaledWidth = destWidth;
            scaledHeight = (scaledWidth * aspectRatio);
        }

        int imageX = (int) (ins.left + (destWidth / 2) - (scaledWidth / 2));
        int imageY = (int) (ins.top + (destHeight / 2) - (scaledHeight / 2));

        Graphics2D g2d = (Graphics2D) g;

        g2d.drawImage(image, imageX, imageY, (int) scaledWidth, (int) scaledHeight, null);
    }
}
