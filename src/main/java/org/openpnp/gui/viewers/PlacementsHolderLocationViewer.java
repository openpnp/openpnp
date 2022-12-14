/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

package org.openpnp.gui.viewers;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.ScrollPaneConstants;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import java.awt.Cursor;
import java.awt.Dimension;

import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.JScrollPane;

import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.GeometricPath2D;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Placement;
import org.openpnp.model.PlacementsHolder;
import org.openpnp.model.PlacementsHolderLocation;
import org.openpnp.util.Utils2D;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.JButton;


@SuppressWarnings("serial")
public class PlacementsHolderLocationViewer extends JPanel {
    private static final double SCREEN_PPI = Toolkit.getDefaultToolkit().getScreenResolution();
    private static final double INCHES_PER_MAJOR_DIVISION = 0.75;
    private static final double PIXELS_PER_MAJOR_DIVISION = SCREEN_PPI * INCHES_PER_MAJOR_DIVISION;
    private static final double PIXEL_GAP = SCREEN_PPI * 0.125;
    private static final double ZOOM_PER_WHEEL_TICK = Math.pow(2, 1.0/4); //4 ticks to double
    private static final int HORIZONTAL_SCALE_HEIGHT = 25;
    private static final int VERTICAL_SCALE_WIDTH = 45;
    private static final int SCALE_TICK_LENGTH = 5;

    private Color reticleColor = new Color(255, 255, 255, 128);
    private Color maskColor = new Color(29, 1, 43, 230);
    private Color substrateColor = Color.GRAY;
    private Color copperColor = new Color(232, 153, 97);
    private Color legendColor = new Color(249, 247, 250);
    private Color transparentColor = new Color(255, 255, 255, 0);
    private Color profileTopColor = new Color(0, 128, 128);  //Teal for Top
    private Color profileBottomColor = new Color(0, 0, 200); //Blue for Bottom

    private BufferedImage placementsHolderImage;
    private BufferedImage horizontalScaleImage;
    private BufferedImage verticalScaleImage;

    private DrawingPanel drawingPanel;

    private DrawingPanelColumnHeader drawingPanelColumnHeader;

    private DrawingPanelRowHeader drawingPanelRowHeader;

    private DrawingPanelUnit drawingPanelUnit;
    private JScrollPane scrollPane;
    private Double scrollingBounds;
    private Double viewPortBounds;
    private double aspectRatio;
    private PlacementsHolder<?> placementsHolder;
    private PlacementsHolderLocation<?> placementsHolderLocation;
    private BiConsumer<PlacementsHolderLocation<?>, String> refreshTableModel;
    private boolean isJob = false;
    private LengthUnit units;
    private Rectangle2D graphicsBounds;
    private Map<Area, PlacementsHolderLocation<?>> profileMap;
    private Double defaultViewableBounds;
    private Double viewableBounds;
    private double scaleFactor;
    private AffineTransform objectToViewTransform;
    private AffineTransform objectToScreenTransform;
    private AffineTransform viewToObjectTransform;
    private AffineTransform screenToObjectTransform;
    private Double viewableClippingBounds;
    private double zoomFactor = 1.0;
    private Point screenDragStartPoint;
    private boolean dragInProgress;
    private Cursor savedCursor;
    private double unitsPerDivision;
    private double unitsPerTick;
    private String displayUnit = "mm";
    private double displayMultiplier;
    private int displayDecimals;
    
    private boolean viewFromTop = true;
    private boolean showReticle = false;
    protected boolean showPlacements = false;
    protected boolean showFiducials = false;
    protected boolean showOrigins = false;
    protected boolean showLocations = true;
    protected boolean showChildrenOnly = true;
    private PlacementsHolderLocation<?> arrayRoot;
    private List<PlacementsHolderLocation<?>> newArrayMembers;
    private JButton btnViewingSide;
    private JButton btnShowChildrenOnly;
    private JCheckBox chckbxReticle;
    private JCheckBox chckbxLocations;
    private JCheckBox chckbxOrigins;
    private JCheckBox chckbxFiducials;
    private JCheckBox chckbxPlacements;

    /**
     * Create the Panel.
     */
    public PlacementsHolderLocationViewer(PlacementsHolderLocation<?> placementsHolderLocation, boolean isJob, BiConsumer<PlacementsHolderLocation<?>, String> refreshTableModel) {
        this.placementsHolderLocation = placementsHolderLocation;
        this.refreshTableModel = refreshTableModel;
        this.isJob = isJob;
        if (placementsHolderLocation instanceof BoardLocation) {
            placementsHolder = (Board) placementsHolderLocation.getPlacementsHolder();
            showPlacements = true;
            showFiducials = true;
            showOrigins = true;
            showLocations = true;
            showChildrenOnly = true;
        }
        else if (placementsHolderLocation instanceof PanelLocation) {
            PanelLocation.setParentsOfAllDescendants((PanelLocation) placementsHolderLocation); 
            placementsHolder = (Panel) placementsHolderLocation.getPlacementsHolder();
            showPlacements = false;
            showFiducials = false;
            showOrigins = false;
            showLocations = true;
            showChildrenOnly = !isJob;
        }
        else {
            throw new UnsupportedOperationException("Viewing of " + placementsHolder.getClass().getSimpleName() + " types is not currently supported");
        }
        
        units = Configuration.get().getSystemUnits();
        
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout(0, 0));
        
        JPanel panel = new JPanel();
        panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        add(panel, BorderLayout.EAST);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        btnViewingSide = new JButton();
        panel.add(btnViewingSide);
        btnViewingSide.setText("Viewing From Top");
        btnViewingSide.setEnabled(!isJob);
        btnViewingSide.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setViewFromTop(!isViewFromTop());
            }
        });
        
        //This dummy label is invisible but forces the container width to be at least wide enough
        //to display it. The intent is to keep the panel width from changing whenever the text in 
        //btnViewingSide and/or btnShowChildrenOnly changes.
        JLabel lblNewLabel = new JLabel("This is just some wide dummy text");
        lblNewLabel.setForeground(lblNewLabel.getBackground());
        panel.add(lblNewLabel);
        
        btnShowChildrenOnly = new JButton();
        if (showChildrenOnly) {
            btnShowChildrenOnly.setText("Viewing Children Only");
        }
        else {
            btnShowChildrenOnly.setText("Viewing All Descendants");
        }
        btnShowChildrenOnly.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowChildrenOnly(!isShowChildrenOnly());
            }
        });
        btnShowChildrenOnly.setVisible(placementsHolderLocation instanceof PanelLocation);
        panel.add(btnShowChildrenOnly);
        
        Component verticalStrut = Box.createVerticalStrut(15);
        panel.add(verticalStrut);
        
        chckbxReticle = new JCheckBox("Reticle");
        panel.add(chckbxReticle);
        chckbxReticle.setSelected(showReticle);
        chckbxReticle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowReticle(chckbxReticle.isSelected());
            }});
        
        chckbxLocations = new JCheckBox("Board/Panel Locations");
        panel.add(chckbxLocations);
        chckbxLocations.setSelected(showLocations);
        chckbxLocations.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowLocations(chckbxLocations.isSelected());
            }});
        
        chckbxOrigins = new JCheckBox("Board/Panel Origins");
        panel.add(chckbxOrigins);
        chckbxOrigins.setSelected(showOrigins);
        chckbxOrigins.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowOrigins(chckbxOrigins.isSelected());
            }});
        
        chckbxFiducials = new JCheckBox("Fiducials");
        panel.add(chckbxFiducials);
        chckbxFiducials.setSelected(showFiducials);
        chckbxFiducials.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowFiducials(chckbxFiducials.isSelected());
            }});
        
        chckbxPlacements = new JCheckBox("Placements");
        panel.add(chckbxPlacements);
        chckbxPlacements.setSelected(showPlacements);
        chckbxPlacements.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowPlacements(chckbxPlacements.isSelected());
            }});
        
        scrollPane = new JScrollPane();
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        
        drawingPanel = new DrawingPanel();
        scrollPane.setViewportView(drawingPanel);

        drawingPanelColumnHeader = new DrawingPanelColumnHeader();
        drawingPanelColumnHeader.setBorder(BorderFactory.createEtchedBorder());
        scrollPane.setColumnHeaderView(drawingPanelColumnHeader);
        
        drawingPanelRowHeader = new DrawingPanelRowHeader();
        drawingPanelRowHeader.setBorder(BorderFactory.createEtchedBorder());
        scrollPane.setRowHeaderView(drawingPanelRowHeader);
        
        drawingPanelUnit = new DrawingPanelUnit();
        drawingPanelUnit.setBorder(BorderFactory.createEtchedBorder());
        scrollPane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, drawingPanelUnit);
        
        Dimension newSize = getDisplayPanelSize();
        drawingPanel.setPreferredSize(newSize);
        drawingPanel.setSize(newSize);
        
        drawingPanel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
               if (e.isPopupTrigger()) {
                    displayPopupMenu(e);
               }
               else {
                    screenDragStartPoint = e.getPoint();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    displayPopupMenu(e);
                }
                else if (dragInProgress) {
                    dragInProgress = false;
                    drawingPanel.setCursor(savedCursor);
                    computePannedBounds(screenDragStartPoint, e.getPoint());
                    drawingPanel.revalidate();
                    drawingPanel.repaint();
                }
            }

        });


        drawingPanel.addMouseWheelListener(new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double newZoomFactor = 1e-6 * Math.round(Math.max(1, Math.pow(ZOOM_PER_WHEEL_TICK, 
                        -e.getPreciseWheelRotation())*zoomFactor) * 1e6);
                if (newZoomFactor != zoomFactor) {
                    computeZoomedBounds(newZoomFactor, e.getPoint());
                    drawingPanel.repaint();
                    drawingPanelRowHeader.repaint();
                    drawingPanelColumnHeader.repaint();
                    drawingPanelUnit.repaint();
                }
            }
        });
        
        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
                    dragInProgress = true;
                    if (savedCursor == null) {
                        savedCursor = drawingPanel.getCursor();
                    }
                    drawingPanel.setCursor(new Cursor(Cursor.MOVE_CURSOR));
                    computePannedBounds(screenDragStartPoint, e.getPoint());
                    screenDragStartPoint = e.getPoint();
                    drawingPanel.revalidate();
                    drawingPanel.repaint();
                    drawingPanelRowHeader.repaint();
                    drawingPanelColumnHeader.repaint();
                }
            }});
        
        drawingPanel.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                computeResizedBounds();
                drawingPanel.revalidate();
                drawingPanel.repaint();
                drawingPanelRowHeader.repaint();
                drawingPanelColumnHeader.repaint();
            }});
        
        drawingPanelColumnHeader.setSize(newSize.width, HORIZONTAL_SCALE_HEIGHT);
        drawingPanelColumnHeader.setPreferredSize(new Dimension(newSize.width, HORIZONTAL_SCALE_HEIGHT));
        
        drawingPanelRowHeader.setSize(VERTICAL_SCALE_WIDTH, newSize.height);
        drawingPanelRowHeader.setPreferredSize(new Dimension(VERTICAL_SCALE_WIDTH, newSize.height));

        regenerate();
    }

    public void setPlacementsHolder(PlacementsHolder<?> placementsHolder) {
        if (placementsHolder == null) {
            return;
        }
        this.placementsHolder = placementsHolder;
        if (placementsHolder instanceof Board) {
            placementsHolderLocation = new BoardLocation((Board) placementsHolder);
        }
        else if (placementsHolder instanceof Panel) {
            placementsHolderLocation = new PanelLocation((Panel) placementsHolder);
            PanelLocation.setParentsOfAllDescendants((PanelLocation) placementsHolderLocation);
        }
        else {
            throw new UnsupportedOperationException("Viewing of " + placementsHolder.getClass().getSimpleName() + " types is not currently supported");
        }
        regenerate();
    }
    
    public void regenerate() {
        Rectangle2D oldBounds = graphicsBounds;
        generateGraphicalObjects(placementsHolderLocation);
        if (!graphicsBounds.equals(oldBounds)) {
            initializeBounds();
        }
        else {
            computeTransforms();
        }
        refresh();
    }
  
    public void refresh() {
        renderPlacementsHolderImage();
        repaint();
    }
  
    public PlacementsHolder<?> getPlacementsHolder() {
        return placementsHolder;
    }
    
    private void initializeBounds() {
        computeViewPortBoundsAndAspectRatio();
        computeDefaultViewableBounds();
        viewableBounds = defaultViewableBounds;
        computeTransforms();
    }
    
    private Dimension getDisplayPanelSize() {
        Dimension currentSize = scrollPane.getViewport().getSize();
        Dimension x = drawingPanelRowHeader.getSize();
        Dimension y = drawingPanelColumnHeader.getSize();
        return new Dimension(currentSize.width + x.width - VERTICAL_SCALE_WIDTH, 
                currentSize.height + y.height - HORIZONTAL_SCALE_HEIGHT);
    }
    
    private void computeViewPortBoundsAndAspectRatio() {
        Dimension currentSize = getDisplayPanelSize();
        if (currentSize.width > 0 && currentSize.height > 0) {
            scrollingBounds = new Rectangle.Double(0, 0, currentSize.width, currentSize.height);
            viewPortBounds = new Rectangle.Double(PIXEL_GAP, PIXEL_GAP, 
                    currentSize.width - 2*PIXEL_GAP, currentSize.height - 2*PIXEL_GAP);
            aspectRatio = viewPortBounds.width / viewPortBounds.height;
        }
    }
    
    private void computeDefaultViewableBounds() {
        if (graphicsBounds != null) {
            defaultViewableBounds = new Rectangle.Double(graphicsBounds.getX(), graphicsBounds.getY(),
                    Math.max(graphicsBounds.getWidth(), aspectRatio*graphicsBounds.getHeight()),
                    Math.max(graphicsBounds.getHeight(), graphicsBounds.getWidth()/aspectRatio));
        }
        else {
            graphicsBounds = new Rectangle.Double(0, 0, aspectRatio, 1);
            defaultViewableBounds = new Rectangle.Double(0, 0, aspectRatio, 1);
        }
    }
    
    private void computeResizedBounds() {
        if (viewableBounds != null) {
            computeViewPortBoundsAndAspectRatio();
            computeDefaultViewableBounds();
            double newVBw = defaultViewableBounds.width / zoomFactor;
            double newVBh = defaultViewableBounds.height / zoomFactor;
            viewableBounds = new Rectangle.Double(viewableBounds.x, viewableBounds.y, newVBw, newVBh);
            computeTransforms();
        }
    }
    
    private void computePannedBounds(Point2D startPoint, Point2D endPoint) {
        Point2D objectStartPoint = screenToObjectTransform.transform(startPoint, null);
        Point2D objectEndPoint = screenToObjectTransform.transform(endPoint, null);
        
        viewableBounds = new Rectangle.Double(
                viewableBounds.x + objectStartPoint.getX() - objectEndPoint.getX(),
                viewableBounds.y + objectStartPoint.getY() - objectEndPoint.getY(), 
                viewableBounds.width, viewableBounds.height);
        computeTransforms();
    }
    
    private void computeZoomedBounds(double newZoomFactor, Point2D screenZoomPoint) {
        if (newZoomFactor > 1) {
            //Convert the mouse screen coordinates to object coordinates
            Point2D objectZoomPoint = screenToObjectTransform.transform(screenZoomPoint, null);
            
            //Now we compute the new viewableBounds so that it has the correct zoom and is located
            //so that the object point is at the same screen location as it was before the zoom
            double newVBw = defaultViewableBounds.width / newZoomFactor;
            double newVBh = defaultViewableBounds.height / newZoomFactor;
            double newVBx = objectZoomPoint.getX() - 
                    newVBw * (objectZoomPoint.getX() - viewableBounds.x) / viewableBounds.width;
            double newVBy = objectZoomPoint.getY() - 
                    newVBh * (objectZoomPoint.getY() - viewableBounds.y) / viewableBounds.height;
            viewableBounds = new Rectangle.Double(newVBx, newVBy, newVBw, newVBh);
        }
        else {
            viewableBounds = defaultViewableBounds;
        }
        zoomFactor = newZoomFactor;
        computeTransforms();
    }
    
    private class DrawingPanelUnit extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Color foreGround = Color.BLACK;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            BufferedImage bufImgR = new BufferedImage(VERTICAL_SCALE_WIDTH, 
                    HORIZONTAL_SCALE_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D offScr = (Graphics2D) bufImgR.getGraphics();
            offScr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            offScr.setColor(this.getBackground());
            offScr.fillRect(0, 0, VERTICAL_SCALE_WIDTH, HORIZONTAL_SCALE_HEIGHT);
            offScr.setColor(foreGround);
            Font font = offScr.getFont().deriveFont(14);
            FontRenderContext frc = offScr.getFontRenderContext();
            TextLayout textTl = new TextLayout(displayUnit, font, frc);
            Rectangle2D b = textTl.getBounds();
            offScr.drawString(displayUnit, (int) (VERTICAL_SCALE_WIDTH/2.0 - b.getWidth()/2.0),
                    (int) (HORIZONTAL_SCALE_HEIGHT/2.0 + b.getHeight()/2.0));
            
            offScr.dispose();
            g2.drawImage(bufImgR, 0, 0, this);
        }        
    }
    
    private class DrawingPanelColumnHeader extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (horizontalScaleImage != null) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(horizontalScaleImage, 0, 0, this);
            }
        }
    }

    private class DrawingPanelRowHeader extends JPanel {
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (verticalScaleImage != null) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(verticalScaleImage, 0, 0, this);
            }
        }
    }
    
    private class DrawingPanel extends JPanel {
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (placementsHolderImage == null) {
                initializeBounds();
                renderPlacementsHolderImage();
            }
            if (placementsHolderImage != null) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.drawImage(placementsHolderImage, 0, 0, this);
            }
        }
        
        
    }
    
    private void computeTransforms() {
        if (viewableBounds == null || viewPortBounds == null) {
            return;
        }
        double sign = 1;
        double xOffset = 0;
        if (!viewFromTop) {
            sign = -1.0;
            xOffset = viewableBounds.getWidth();
        }
        scaleFactor = viewPortBounds.width / viewableBounds.width;

        objectToViewTransform = new AffineTransform();
        objectToViewTransform.scale(sign*scaleFactor, scaleFactor);
        objectToViewTransform.translate(-xOffset-viewableBounds.getX(), -viewableBounds.getY());

        objectToScreenTransform = new AffineTransform();
        objectToScreenTransform.translate(PIXEL_GAP, (int) scrollingBounds.height - PIXEL_GAP);
        objectToScreenTransform.scale(1, -1);
        objectToScreenTransform.concatenate(objectToViewTransform);

        viewToObjectTransform = new AffineTransform();
        viewToObjectTransform.translate(xOffset+viewableBounds.getX(), viewableBounds.getY());
        viewToObjectTransform.scale(sign*1.0/scaleFactor, 1.0/scaleFactor);

        screenToObjectTransform = new AffineTransform(viewToObjectTransform);
        screenToObjectTransform.scale(1, -1);
        screenToObjectTransform.translate(-PIXEL_GAP, -((int) scrollingBounds.height - PIXEL_GAP));
        
        viewableClippingBounds = new Rectangle.Double(viewableBounds.getX() - PIXEL_GAP/scaleFactor,
                viewableBounds.getY() - PIXEL_GAP/scaleFactor,
                viewableBounds.getWidth() + 2*PIXEL_GAP/scaleFactor,
                viewableBounds.getHeight() + 2*PIXEL_GAP/scaleFactor);
        
        renderPlacementsHolderImage();
    }

    public void generateGraphicalObjects(PlacementsHolderLocation<?> placementsHolderLocation) {
        
        if (placementsHolderLocation == null || placementsHolderLocation.getPlacementsHolder() == null) {
            return;
        }
        AffineTransform at = placementsHolderLocation.getLocalToGlobalTransform();
        GeometricPath2D profile = placementsHolderLocation.getPlacementsHolder().getProfile().convertToUnits(units);
        profile.transform(at);
        boolean atRoot = placementsHolderLocation == this.placementsHolderLocation;
        if (atRoot) {
            profileMap = new HashMap<>();
            if (isJob) {
                //include (0,0) as this is the machine origin
                graphicsBounds = new Rectangle2D.Double(); 
            }
            else {
                graphicsBounds = null;
            }
        }
        if (!atRoot || !isJob) {
            if (graphicsBounds == null) {
                graphicsBounds = profile.getBounds2D();
            }
            else {
                graphicsBounds.add(profile.getBounds2D());
            }
            for (Placement placement : placementsHolderLocation.getPlacementsHolder().getPlacements()) {
                Location loc = Utils2D.calculateBoardPlacementLocation(placementsHolderLocation, placement).convertToUnits(units);
                graphicsBounds.add(new Point2D.Double(loc.getX(), loc.getY()));
            }
            profileMap.put(new Area(profile), placementsHolderLocation);
        }
        if ((atRoot || !showChildrenOnly) && placementsHolderLocation instanceof PanelLocation) {
            for (PlacementsHolderLocation<?> child : ((PanelLocation) placementsHolderLocation).getChildren()) {
                generateGraphicalObjects(child);
            }
        }
    }
    
    public void renderPlacementsHolderImage() {
        if (scrollingBounds != null && scrollingBounds.width > 0 && scrollingBounds.height > 0) {
            placementsHolderImage = new BufferedImage((int) scrollingBounds.width, (int) scrollingBounds.height, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D offScr = (Graphics2D) placementsHolderImage.getGraphics();
            offScr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
            offScr.setColor(Color.BLACK);
            offScr.fillRect(0, 0, (int) scrollingBounds.width, (int) scrollingBounds.height);
            
            offScr.translate(PIXEL_GAP, (int) scrollingBounds.height - PIXEL_GAP);
            offScr.scale(1, -1);
            
            offScr.setStroke(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));

            Paint originalPaint = offScr.getPaint();
            GradientPaint gp1 = new GradientPaint(5, 5, 
                    new Color(255, 0, 0, 128), 8, 8, new Color(0, 0, 0, 128), true);
            

            for (Area profile : profileMap.keySet() ) {
                if (profile.intersects(viewableClippingBounds)) {
                    PlacementsHolderLocation<?> phl = profileMap.get(profile);
                    if (phl == arrayRoot) {
                        offScr.setStroke(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND,
                                0, new float[]{(float) (PIXEL_GAP * 0.25)}, 0));
                    }
                    else if (newArrayMembers != null && newArrayMembers.contains(phl)) {
                        offScr.setStroke(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND,
                                  0, new float[]{(float) (PIXEL_GAP * 0.5)}, 0));
                    }
                    else {
                        offScr.setStroke(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
                    }
                    Shape profileShape = objectToViewTransform.createTransformedShape(profile);
                    if (!phl.isLocallyEnabled()) {
                        offScr.setPaint(gp1);
                        offScr.fill(profileShape);
                    }
                    offScr.setPaint(originalPaint);
                    if ((phl.getGlobalSide() == Side.Top && viewFromTop) || 
                            (phl.getGlobalSide() == Side.Bottom && !viewFromTop)) {
                        offScr.setColor(profileTopColor);
                    }
                    else {
                        offScr.setColor(profileBottomColor);
                    }
                    offScr.draw(profileShape);
                }
            }
            
            if (showFiducials) {
                overlayFiducialMarks(offScr);
            }
            if (showPlacements) {
                overlayPlacementMarks(offScr);
            }
            if (showLocations) {
                overlayLocationMarks(offScr);
            }
            if (showOrigins) {
                overlayOriginMarks(offScr);
            }
            if (isJob) {
                //Show the machine's origin
                overlayOriginMark(offScr, placementsHolderLocation);
            }
            overlayReticle(offScr);
            
            offScr.dispose();
        }
    }
    
    private void overlayLocationMarks(Graphics2D offScr) {
        double d = PIXEL_GAP/(1.0*scaleFactor);
        offScr.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        for (Area profileArea : profileMap.keySet()) {
            PlacementsHolderLocation<?> placementsHolderLocation = profileMap.get(profileArea);
            if (placementsHolderLocation != null) {
                Location offset = Location.origin;
                if ((viewFromTop && placementsHolderLocation.getGlobalSide() == Side.Bottom) ||
                        (!viewFromTop && placementsHolderLocation.getGlobalSide() == Side.Top)) {
                    offset = offset.add(new Location(units, placementsHolderLocation.getPlacementsHolder().getDimensions().getLengthX().convertToUnits(units).getValue(), 0, 0, 0));
                }
                Location location = Utils2D.calculateBoardPlacementLocation(placementsHolderLocation, offset);
                overlayLocationMark(offScr, d, location);
            }
        }
    }
    
    private void overlayLocationMark(Graphics2D offScr, double size, Location location) {
        AffineTransform at = new AffineTransform(objectToViewTransform);
        at.translate(location.getX(), location.getY());
        at.rotate(Math.toRadians(location.getRotation()));
        Shape line = new Line2D.Double(0, 0, 0, size);
        offScr.setColor(Color.CYAN);
        offScr.draw(at.createTransformedShape(line));
        line = new Line2D.Double(-size, 0, size, 0);
        offScr.setColor(Color.RED);
        offScr.draw(at.createTransformedShape(line));
        line = new Line2D.Double(0, 0, 0, -size);
        offScr.draw(at.createTransformedShape(line));
    }
    
    private void overlayOriginMarks(Graphics2D offScr) {
        for (Area profileArea : profileMap.keySet()) {
            PlacementsHolderLocation<?> placementsHolderLocation = profileMap.get(profileArea);
            overlayOriginMark(offScr, placementsHolderLocation);
        }
    }
    
    private void overlayOriginMark(Graphics2D offScr, PlacementsHolderLocation<?> phl) {
        Location location = Utils2D.calculateBoardPlacementLocation(phl, Location.origin.convertToUnits(units));
        AffineTransform at = new AffineTransform(objectToViewTransform);
        at.translate(location.getX(), location.getY());
        at.rotate(Math.toRadians(location.getRotation()));
        at.scale(phl.getGlobalSide() == Side.Top ? 1 : -1, 1);
        double d = PIXEL_GAP/(0.75*scaleFactor);
        offScr.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        offScr.setColor(Color.CYAN);
        Shape line = new Line2D.Double(-d/10, -d/10, -d/10, d);
        offScr.draw(at.createTransformedShape(line));
        line = new Line2D.Double(d/10, d/10, d/10, d);
        offScr.draw(at.createTransformedShape(line));
        line = new Line2D.Double(-2*d/5, d-d/5, 0, d+d/5);
        offScr.draw(at.createTransformedShape(line));
        line = new Line2D.Double(+2*d/5, d-d/5, 0, d+d/5);
        offScr.draw(at.createTransformedShape(line));
        offScr.setColor(Color.RED);
        line = new Line2D.Double(-d/10, -d/10, d, -d/10);
        offScr.draw(at.createTransformedShape(line));
        line = new Line2D.Double(d/10, d/10, d, d/10);
        offScr.draw(at.createTransformedShape(line));
        line = new Line2D.Double(d-d/5, +2*d/5, d+d/5, 0);
        offScr.draw(at.createTransformedShape(line));
        line = new Line2D.Double(d-d/5, -2*d/5, d+d/5, 0);
        offScr.draw(at.createTransformedShape(line));
    }
    
    private void overlayPlacementMarks(Graphics2D offScr) {
        double d = PIXEL_GAP/(2.0*scaleFactor);
        offScr.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        for (Area profileArea : profileMap.keySet()) {
            PlacementsHolderLocation<?> placementsHolderLocation = profileMap.get(profileArea);
            for (Placement placement : placementsHolderLocation.getPlacementsHolder().getPlacements()) {
                if (placement.getType() == Placement.Type.Placement && 
                        ((viewFromTop && placement.getSide() == placementsHolderLocation.getGlobalSide()) || 
                        (!viewFromTop && placement.getSide() != placementsHolderLocation.getGlobalSide()))) {
                    Location localLocation = placement.getLocation().multiply(1, 1, 1, viewFromTop ? 1 : -1);
                    Location location = Utils2D.calculateBoardPlacementLocation(placementsHolderLocation, localLocation);
                    overlayLocationMark(offScr, d, location);
                }
            }
        }
    }
    
    private void overlayFiducialMarks(Graphics2D offScr) {
        for (Area profileArea : profileMap.keySet()) {
            PlacementsHolderLocation<?> placementsHolderLocation = profileMap.get(profileArea);
            for (Placement placement : placementsHolderLocation.getPlacementsHolder().getPlacements()) {
                if (placement.getType() == Placement.Type.Fiducial && 
                        ((viewFromTop && placement.getSide() == placementsHolderLocation.getGlobalSide()) || 
                        (!viewFromTop && placement.getSide() != placementsHolderLocation.getGlobalSide()))) {
                    Location localLocation = placement.getLocation().multiply(1, 1, 1, viewFromTop ? 1 : -1);
                    Location location = Utils2D.calculateBoardPlacementLocation(placementsHolderLocation, localLocation);
                    overlayFiducialMark(offScr, 1, location);
                }
            }
        }
    }
    
    private void overlayFiducialMark(Graphics2D offScr, double size, Location location) {
        AffineTransform at = new AffineTransform(objectToViewTransform);
        at.translate(location.getX(), location.getY());
        at.rotate(Math.toRadians(location.getRotation()));
        offScr.setColor(copperColor);
        Shape fid = new Ellipse2D.Double(-0.5*size, -0.5*size, size, size);
        offScr.fill(at.createTransformedShape(fid));
    }
    

    private void overlayReticle(Graphics2D offScr) {
        double unitScaling = 1;
        String displayUnits;
        switch (units) {
            case Millimeters:
            case Centimeters:
            case Microns:
            case Meters:
                displayUnits = "Metric";
                break;
            case Feet:
            case Inches:
            case Mils:
                displayUnits = "Imperial";
                break;
            default:
                displayUnits = "Metric";
                break;
        }
        
        Rectangle2D objectSpaceBounds = screenToObjectTransform.createTransformedShape(scrollingBounds).getBounds2D();
        double numberOfMajorDivisions = scrollingBounds.getWidth() / PIXELS_PER_MAJOR_DIVISION;
        
        unitsPerDivision = unitScaling * objectSpaceBounds.getWidth() / numberOfMajorDivisions;
        
        double powerOfTen = Math.floor(Math.log10(unitsPerDivision));
        double mult = unitsPerDivision / Math.pow(10, powerOfTen);
        if (mult >= 2.5) {
            mult = 5;
        }
        else if (mult >= 1.5) {
            mult = 2;
        }
        else {
            mult = 1;
        }
        unitsPerDivision = mult * Math.pow(10, powerOfTen);
        if (mult == 5) {
            unitsPerTick = unitsPerDivision / 5;
        }
        else {
            unitsPerTick = unitsPerDivision / 10;
        }
        
        if (displayUnits.equals("Metric")) {
            if (unitsPerDivision > 1) {
                displayUnit = "mm";
                displayMultiplier = 1;
                displayDecimals = 0;
            }
            else if (unitsPerDivision > 0.1) {
                displayUnit = "mm";
                displayMultiplier = 1;
                displayDecimals = 1;
            }
            else {
                displayUnit = "um";
                displayMultiplier = 1000;
                displayDecimals = 0;
            }
        }
        else {
            if (unitsPerDivision >= 1) {
                displayUnit = "in";
                displayMultiplier = 1;
                displayDecimals = 0;
            }
            else if (unitsPerDivision >= 0.1) {
                displayUnit = "in";
                displayMultiplier = 1;
                displayDecimals = 1;
            }
            else {
                displayUnit = "mil";
                displayMultiplier = 1000;
                displayDecimals = 0;
            }
        }
        
        offScr.setColor(reticleColor);
        offScr.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        
        double tickLengthOver2 = 0.025*PIXELS_PER_MAJOR_DIVISION;
        double xMin = unitsPerDivision*Math.floor(unitScaling*objectSpaceBounds.getMinX()/unitsPerDivision);
        double xMax = unitsPerDivision*Math.ceil(unitScaling*objectSpaceBounds.getMaxX()/unitsPerDivision);
        double yMin = unitsPerDivision*Math.floor(unitScaling*objectSpaceBounds.getMinY()/unitsPerDivision);
        double yMax = unitsPerDivision*Math.ceil(unitScaling*objectSpaceBounds.getMaxY()/unitsPerDivision);
        
        AffineTransform at = new AffineTransform(objectToViewTransform);
        at.scale(1/unitScaling, 1/unitScaling);
        
        if (showReticle ) {
            //Major vertical lines
            double x = xMin;
            while (x <= xMax) {
                Point2D start = new Point2D.Double(x, yMin);
                Point2D end = new Point2D.Double(x, yMax);
                
                at.transform(start, start);
                at.transform(end, end);
                
                offScr.drawLine((int) start.getX(), (int) start.getY(), (int) end.getX(), (int) end.getY());
                
                //with horizontal tick marks
                double y = yMin;
                while (y <= yMax) {
                    Point2D mid = new Point2D.Double(x, y);
                    
                    at.transform(mid, mid);
                    offScr.drawLine((int) (mid.getX()-tickLengthOver2), (int) mid.getY(), (int) (mid.getX()+tickLengthOver2), (int) mid.getY());
                    y += unitsPerTick;
                }
                x += unitsPerDivision;
            }
            
            //Major horizontal lines
            double y = yMin;
            while (y <= yMax) {
                Point2D start = new Point2D.Double(xMin, y);
                Point2D end = new Point2D.Double(xMax, y);
                
                at.transform(start, start);
                at.transform(end, end);
                
                offScr.drawLine((int) start.getX(), (int) start.getY(), (int) end.getX(), (int) end.getY());
                
                //with vertical tick marks
                x = xMin;
                while (x <= xMax) {
                    Point2D mid = new Point2D.Double(x, y);
                    
                    at.transform(mid, mid);
                    offScr.drawLine((int) mid.getX(), (int) (mid.getY()-tickLengthOver2), (int) mid.getX(), (int) (mid.getY()+tickLengthOver2));
                    x += unitsPerTick;
                }
                y += unitsPerDivision;
            }
        }
        renderVerticalScale(at, xMin, xMax, yMin, yMax);
        renderHorizontalScale(at, xMin, xMax, yMin, yMax);
    }
    
    private void renderVerticalScale(AffineTransform at, double xMin, double xMax,
            double yMin, double yMax) {
        Color foreGround = Color.BLACK;

        verticalScaleImage = new BufferedImage(VERTICAL_SCALE_WIDTH, (int) scrollingBounds.height, 
                BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D offScr = (Graphics2D) verticalScaleImage.getGraphics();
        offScr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        offScr.setColor(drawingPanelRowHeader.getBackground());
        offScr.fillRect(0, 0, VERTICAL_SCALE_WIDTH, (int) scrollingBounds.height);
        
        offScr.translate(0, (int) scrollingBounds.height - PIXEL_GAP);
        offScr.scale(1, -1);

        offScr.setColor(foreGround);
        offScr.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        
        //Major horizontal lines
        double y = yMin;
        Font f = offScr.getFont().deriveFont(10);
        FontRenderContext frc = offScr.getFontRenderContext();
        while (y <= yMax) {
            Point2D tick = new Point2D.Double(xMin, y);
            
            at.transform(tick, tick);
            
            offScr.drawLine(VERTICAL_SCALE_WIDTH-SCALE_TICK_LENGTH, (int) tick.getY(), VERTICAL_SCALE_WIDTH, (int) tick.getY());

            String text = String.format("%." + displayDecimals + "f", y*displayMultiplier);
            TextLayout textTl = new TextLayout(text, f, frc);
        
            AffineTransform transform = new AffineTransform();
            transform.translate(VERTICAL_SCALE_WIDTH-SCALE_TICK_LENGTH-textTl.getBounds().getWidth()-2, tick.getY()-textTl.getBounds().getHeight()/2);
            transform.scale(1, -1);
            Shape outline = textTl.getOutline(null);
            offScr.fill(transform.createTransformedShape(outline));
            
            y += unitsPerDivision;
        
        }
        
        offScr.dispose();
    }

    private void renderHorizontalScale(AffineTransform at, double xMin, double xMax,
            double yMin, double yMax) {
        Color foreGround = Color.BLACK;

        horizontalScaleImage = new BufferedImage((int) scrollingBounds.width, HORIZONTAL_SCALE_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D offScr = (Graphics2D) horizontalScaleImage.getGraphics();
        offScr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        offScr.setColor(drawingPanelColumnHeader.getBackground());
        offScr.fillRect(0, 0, (int) scrollingBounds.width, HORIZONTAL_SCALE_HEIGHT);
        
        offScr.translate(PIXEL_GAP, 0);
        offScr.scale(1, 1);
        
        offScr.setColor(foreGround);
        offScr.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        
        //Major vertical lines
        double x = xMin;
        FontRenderContext frc = offScr.getFontRenderContext();
        Font f = offScr.getFont().deriveFont(10);
        while (x <= xMax) {
            Point2D tick = new Point2D.Double(x, yMax);
            
            at.transform(tick, tick);
            
            offScr.drawLine((int) tick.getX(), HORIZONTAL_SCALE_HEIGHT, (int) tick.getX(), HORIZONTAL_SCALE_HEIGHT-SCALE_TICK_LENGTH);
            
            String text = String.format("%." + displayDecimals + "f", x*displayMultiplier);
            TextLayout textTl = new TextLayout(text, f, frc);
            AffineTransform transform = new AffineTransform();
            transform.translate(tick.getX()-textTl.getBounds().getWidth()/2, HORIZONTAL_SCALE_HEIGHT - 2 - textTl.getBounds().getHeight());
            Shape outline = transform.createTransformedShape(textTl.getOutline(null));
            offScr.fill(outline);
            
            x += unitsPerDivision;
        }
        
        offScr.dispose();
    }

    public void cancel() {
    }
    
    public class LabeledPopupMenu extends JPopupMenu {
        private String originalLabelText = null;
        private final JLabel label;

        public LabeledPopupMenu() {
            super();
            this.label = null;
        }

        public LabeledPopupMenu(String label) {
            super();
            originalLabelText = label;
            this.label = new JLabel("<html><b>" +
                label + "</b></html>");
            this.label.setHorizontalAlignment(SwingConstants.CENTER);
            add(this.label);
            addSeparator();
        }

        @Override 
        public void setLabel(String text) {
            if (null == label) return;
            originalLabelText = text;
            label.setText("<html><b>" +
                text +
                "</b></html>");
        }

        @Override 
        public String getLabel() {
            return originalLabelText;
        }
    }
    
    private void displayPopupMenu(MouseEvent e) {
        Point point = e.getPoint();
        Point2D imagePoint = screenToObjectTransform.transform(point, null);
        PlacementsHolderLocation<?> potentialPlacementsHolderLocation = null;
        String uniqueId = "";
        for (Area a : profileMap.keySet()) {
            if (a.contains(imagePoint)) {
                String potentialId = profileMap.get(a).getUniqueId();
                if (potentialId != null && potentialId.length() > uniqueId.length()) {
                    uniqueId = potentialId;
                    potentialPlacementsHolderLocation = profileMap.get(a);
                }
            }
        }
        if (potentialPlacementsHolderLocation != null) {
            final PlacementsHolderLocation<?> phl = potentialPlacementsHolderLocation;
            JPopupMenu popUp = new LabeledPopupMenu(
                   "<pre>Id:   " + uniqueId + 
                   "<br>Name: " + phl.getPlacementsHolder().getName() + 
                   "<br>Side: " + phl.getGlobalSide() + "</pre>");
            JCheckBoxMenuItem cbmi = new JCheckBoxMenuItem("Enabled?");
            cbmi.setSelected(phl.isLocallyEnabled());
            cbmi.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    phl.setLocallyEnabled(cbmi.isSelected());
                    try {
                        refreshTableModel.accept(phl, "Enabled?");
                    }
                    catch (Exception e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
                
            });
            popUp.add(cbmi);
            popUp.show(drawingPanel, point.x, point.y);
        }
    }

    public boolean isViewFromTop() {
        return viewFromTop;
    }

    public void setViewFromTop(boolean viewFromTop) {
        this.viewFromTop = viewFromTop;
        if (viewFromTop) {
            btnViewingSide.setText("Viewing From Top");
        }
        else {
            btnViewingSide.setText("Viewing From Bottom");
        }
        regenerate();
        drawingPanel.repaint();
    }

    public boolean isShowReticle() {
        return showReticle;
    }

    public void setShowReticle(boolean showReticle) {
        this.showReticle = showReticle;
        chckbxReticle.setSelected(showReticle);
        renderPlacementsHolderImage();
        drawingPanel.repaint();
    }

    public boolean isShowPlacements() {
        return showPlacements;
    }

    public void setShowPlacements(boolean showPlacements) {
        this.showPlacements = showPlacements;
        chckbxPlacements.setSelected(showPlacements);
        renderPlacementsHolderImage();
        drawingPanel.repaint();
    }

    public boolean isShowFiducials() {
        return showFiducials;
    }

    public void setShowFiducials(boolean showFiducials) {
        this.showFiducials = showFiducials;
        chckbxFiducials.setSelected(showFiducials);
        renderPlacementsHolderImage();
        drawingPanel.repaint();
    }

    public boolean isShowOrigins() {
        return showOrigins;
    }

    public void setShowOrigins(boolean showOrigins) {
        this.showOrigins = showOrigins;
        chckbxOrigins.setSelected(showOrigins);
        renderPlacementsHolderImage();
        drawingPanel.repaint();
    }

    public boolean isShowLocations() {
        return showLocations;
    }

    public void setShowLocations(boolean showLocations) {
        this.showLocations = showLocations;
        chckbxLocations.setSelected(showLocations);
        renderPlacementsHolderImage();
        drawingPanel.repaint();
    }

    public boolean isShowChildrenOnly() {
        return showChildrenOnly;
    }

    public void setShowChildrenOnly(boolean showChildrenOnly) {
        this.showChildrenOnly = showChildrenOnly;
        if (showChildrenOnly) {
            btnShowChildrenOnly.setText("Viewing Children Only");
        }
        else {
            btnShowChildrenOnly.setText("Viewing All Descendants");
        }
        regenerate();
        drawingPanel.repaint();
    }

    public void setArrayRoot(PlacementsHolderLocation<?> arrayRoot) {
        this.arrayRoot = arrayRoot;
        renderPlacementsHolderImage();
        drawingPanel.repaint();
    }
    
    public void setNewArrayMembers(List<PlacementsHolderLocation<?>> newArrayMembers) {
        this.newArrayMembers = newArrayMembers;
        renderPlacementsHolderImage();
        drawingPanel.repaint();
    }
}
