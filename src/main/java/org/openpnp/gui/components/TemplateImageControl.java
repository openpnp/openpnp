/*
 * Copyright (C) 2021 <mark@makr.zone>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.UIManager;

import org.openpnp.gui.MainFrame;
import org.openpnp.model.Location;
import org.openpnp.spi.Camera;
import org.openpnp.vision.TemplateImage;

@SuppressWarnings("serial")
public class TemplateImageControl extends JComponent implements MouseMotionListener, MouseListener {  

    private TemplateImage templateImage = null;
    private int minWidth = 100;
    private int minHeight = 100;
    private Camera camera = null;
    private String name = "Template";
    private Location captureLocation = null; 
    
    public TemplateImageControl() {
        addMouseMotionListener(this);
        addMouseListener(this);
    }
    public TemplateImageControl(TemplateImage templateImage) {
        this();
        this.templateImage = templateImage;
    }

    public TemplateImage getTemplateImage() {
        return templateImage;
    }
    public void setTemplateImage(TemplateImage templateImage) {
        this.templateImage = templateImage;
        repaint();
    }

    public int getMinWidth() {
        return minWidth;
    }
    public void setMinWidth(int minWidth) {
        this.minWidth = minWidth;
        validate();
    }
    public int getMinHeight() {
        return minHeight;
    }
    public void setMinHeight(int minHeight) {
        this.minHeight = minHeight;
        validate();
    }

    public Camera getCamera() {
        return camera;
    }
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color gridColor = UIManager.getColor ( "PasswordField.capsLockIconColor" );
        if (gridColor == null) {
            gridColor = new Color(0, 0, 0, 64);
        } else {
            gridColor = new Color(gridColor.getRed(), gridColor.getGreen(), gridColor.getBlue(), 64);
        }
        Font font = getFont();
        FontMetrics dfm = g2d.getFontMetrics(font);
        g2d.setFont(font);
        int w = getWidth();
        int h = getHeight();
        BufferedImage image = getImage();
        if (image != null) {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            AffineTransform t = new AffineTransform();
            double scale = Math.min(((double)w)/image.getWidth(), ((double)h)/image.getHeight());
            t.scale(scale, scale);
            g2d.drawImage(image, t, null);
        }
        else {
            g2d.setColor(gridColor);
            int d = Math.min(w, h)-1;
            g2d.drawRect(0, 0, d, d);
            String text = "no image";
            Rectangle2D bounds = dfm.getStringBounds(text, 0, text.length(), g2d);
            g2d.drawString(text, (int)(Math.min(h, w)/2-bounds.getWidth()/2), (int)(h/2));
        }
    }
    protected BufferedImage getImage() {
        BufferedImage image = null;
        if (templateImage != null) {
            try {
                image = templateImage.getImage();
            }
            catch (Exception e) {
            }
        }
        return image;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension superDim = super.getPreferredSize();
        BufferedImage image = getImage();
        if (image != null) {
            double scale = Math.max(Math.max(minWidth, superDim.getWidth())/image.getWidth(), Math.max(minHeight, superDim.getHeight())/image.getHeight());
            int width = (int)(image.getWidth()*scale);
            int height = (int)(image.getHeight()*scale); 
            return new Dimension(width, height);
        }
        else {
            int width = (int)Math.max(minWidth, superDim.getWidth());
            int height = (int)Math.max(minHeight, superDim.getHeight());; 
            return new Dimension(width, height);
        }
    }
    @Override
    public void mouseDragged(MouseEvent e) {
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        if (camera != null) {
            BufferedImage image = getImage();
            if (image != null) {
                CameraPanel cameraViews = MainFrame.get().getCameraViews();
                CameraView view = cameraViews.getCameraView(camera);
                view.showFilteredImage(image, name, 2000);
                cameraViews.ensureCameraVisible(camera);
            }
        }
    }
    @Override
    public void mouseClicked(MouseEvent e) {
        
    }
    @Override
    public void mousePressed(MouseEvent e) {
    }
    @Override
    public void mouseReleased(MouseEvent e) {
    }
    @Override
    public void mouseEntered(MouseEvent e) {
    }
    @Override
    public void mouseExited(MouseEvent e) {
    }

}