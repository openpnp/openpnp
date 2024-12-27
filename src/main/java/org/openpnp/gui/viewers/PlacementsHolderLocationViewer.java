/*
 * Copyright (C) 2023 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.ScrollPaneConstants;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import java.awt.Cursor;
import java.awt.Dimension;

import javax.swing.SwingUtilities;
import javax.swing.border.EtchedBorder;
import javax.swing.JScrollPane;

import org.openpnp.Translations;
import org.openpnp.events.DefinitionStructureChangedEvent;
import org.openpnp.events.PlacementChangedEvent;
import org.openpnp.events.PlacementsHolderChangedEvent;
import org.openpnp.events.PlacementsHolderLocationChangedEvent;
import org.openpnp.gui.MainFrame;
import org.openpnp.gui.components.LabeledPopupMenu;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.model.Board;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.GeometricPath2D;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Placement;
import org.openpnp.model.PlacementsHolder;
import org.openpnp.model.PlacementsHolderLocation;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;

import com.google.common.eventbus.Subscribe;

import java.awt.Component;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;

@SuppressWarnings("serial")
public class PlacementsHolderLocationViewer extends JPanel implements PropertyChangeListener {
    private static final double SCREEN_PPI = Toolkit.getDefaultToolkit().getScreenResolution();
    private static final double INCHES_PER_MAJOR_DIVISION = 0.75;
    private static final double PIXELS_PER_MAJOR_DIVISION = SCREEN_PPI * INCHES_PER_MAJOR_DIVISION;
    private static final double EDGE_GAP = SCREEN_PPI * 0.125;
    private static final double ZOOM_PER_WHEEL_TICK = Math.pow(2, 1.0/4); //4 ticks to double
    private static final int HORIZONTAL_SCALE_HEIGHT = 25;
    private static final int VERTICAL_SCALE_WIDTH = 45;
    private static final int SCALE_TICK_LENGTH = 5;

    private Color reticleColor = new Color(255, 255, 255, 128);
    private Color maskColor = new Color(29, 1, 43, 230); //OSHPARK dark purple solder mask
    private Color substrateColor = Color.GRAY;
    private Color copperColor = new Color(232, 153, 97);//E89961
    private Color legendColor = new Color(249, 247, 250); //Silkscreen
    private Color transparentColor = new Color(255, 255, 255, 0);
    private Color profileTopColor = new Color(0, 144, 144);  //Teal for Top
    private Color profileBottomColor = new Color(10, 10, 255); //Blue for Bottom
    private Color placementEnabledColor = Color.WHITE;
    private Color placementDisabledColor = Color.DARK_GRAY;
    private Color fiducialEnabledColor = copperColor;
    private Color fiducialDisabledColor = Color.DARK_GRAY;
    
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
    private List<PlacementsHolderLocation<?>> selections;
    private boolean isJob = false;
    private LengthUnit units;
    private Rectangle2D graphicsBounds;
    private Map<Area, PlacementsHolderLocation<?>> profileMap;
    private Map<Area, Placement> placementMap;
    private Map<Placement, Area> placementInverseMap;
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
    private String displayUnit = "mm"; //$NON-NLS-1$
    private double displayMultiplier;
    private int displayDecimals;
    
    private boolean viewFromTop = true;
    private boolean showReticle = false;
    protected boolean showPlacements = false;
    protected boolean showFiducials = false;
    protected boolean showOrigins = false;
    protected boolean showLocations = true;
    protected ViewingOption viewingOption = ViewingOption.CHILDREN;
    private PlacementsHolderLocation<?> arrayRoot;
    private List<PlacementsHolderLocation<?>> newArrayMembers;
    private JButton btnViewingSide;
    private JComboBox <Enum> cbxViewingOptions;
    private JCheckBox chckbxReticle;
    private JCheckBox chckbxLocations;
    private JCheckBox chckbxOrigins;
    private JCheckBox chckbxFiducials;
    private JCheckBox chckbxPlacements;
    private Component verticalStrut_1;
    private JLabel lblNewLabel_1;
    private JLabel lblNewLabel_2;
    private JLabel lblNewLabel_3;
    private JLabel lblNewLabel_4;
    private JLabel lblNewLabel_5;
    private JLabel lblNewLabel_6;

    public enum ViewingOption {
        ALL(Translations.getString("PlacementsHolderLocationViewer.ViewingOption.AllDescendants")),  //$NON-NLS-1$
        CHILDREN(Translations.getString("PlacementsHolderLocationViewer.ViewingOption.ChildrenOnly")),  //$NON-NLS-1$
        SELECTED(Translations.getString("PlacementsHolderLocationViewer.ViewingOption.SelectedOnly"));  //$NON-NLS-1$

        private String name;

        private ViewingOption(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    /**
     * Create the Panel.
     */
    public PlacementsHolderLocationViewer(PlacementsHolderLocation<?> placementsHolderLocation, boolean isJob, List<PlacementsHolderLocation<?>> selections) {
        this.placementsHolderLocation = placementsHolderLocation;
        this.selections = selections;
        this.isJob = isJob;
        if (placementsHolderLocation instanceof BoardLocation) {
            placementsHolder = (Board) placementsHolderLocation.getPlacementsHolder();
            showPlacements = true;
            showFiducials = true;
            showOrigins = true;
            showLocations = true;
            viewingOption = ViewingOption.CHILDREN;
        }
        else if (placementsHolderLocation instanceof PanelLocation) {
            if (!isJob) {
                PanelLocation.setParentsOfAllDescendants((PanelLocation) placementsHolderLocation);
            }
            placementsHolder = (Panel) placementsHolderLocation.getPlacementsHolder();
            showPlacements = false;
            showFiducials = false;
            showOrigins = false;
            showLocations = true;
            if (isJob) {
                if (selections != null && selections.size() > 1) {   // when one selected row only then likely by default user wants to see all
                    viewingOption = ViewingOption.SELECTED;
                } else {
                    viewingOption = ViewingOption.ALL;
                }
            } else {
                viewingOption = ViewingOption.CHILDREN;
            }
        }
        else {
            throw new UnsupportedOperationException("Viewing of "  //$NON-NLS-1$
                    + placementsHolder.getClass().getSimpleName() + " types is not currently supported"); //$NON-NLS-1$
        }
        
        units = Configuration.get().getSystemUnits();
        
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new BorderLayout(0, 0));
        
        JPanel panel = new JPanel();
        panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
        add(panel, BorderLayout.EAST);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        btnViewingSide = new JButton();
        btnViewingSide.setToolTipText(
                Translations.getString("PlacementsHolderLocationViewer.ViewingSide.ToolTip")); //$NON-NLS-1$
        panel.add(btnViewingSide);
        btnViewingSide.setText(
                Translations.getString("PlacementsHolderLocationViewer.ViewingSide.Top")); //$NON-NLS-1$
        btnViewingSide.setEnabled(!isJob);
        btnViewingSide.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setViewFromTop(!isViewFromTop());
            }
        });
        
        cbxViewingOptions = new JComboBox <>();
        cbxViewingOptions.setToolTipText(Translations.getString("PlacementsHolderLocationViewer.ViewingOption.ToolTip"));
        cbxViewingOptions.addItem(ViewingOption.ALL);
        cbxViewingOptions.addItem(ViewingOption.CHILDREN);
        cbxViewingOptions.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (isJob) {
            cbxViewingOptions.addItem(ViewingOption.SELECTED);
        }
        cbxViewingOptions.setSelectedItem(viewingOption);
        cbxViewingOptions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox)e.getSource();
                setViewingOption((ViewingOption) cb.getSelectedItem());
            }
        });
        cbxViewingOptions.setVisible(placementsHolderLocation instanceof PanelLocation);
        cbxViewingOptions.setMaximumSize(cbxViewingOptions.getPreferredSize());
        panel.add(Box.createVerticalStrut(15));
        panel.add(cbxViewingOptions);
        
        Component verticalStrut = Box.createVerticalStrut(15);
        panel.add(verticalStrut);
        
        chckbxReticle = new JCheckBox(
                Translations.getString("PlacementsHolderLocationViewer.ViewingOption.Reticle")); //$NON-NLS-1$
        chckbxReticle.setToolTipText(
                Translations.getString("PlacementsHolderLocationViewer.ViewingOption.Reticle.ToolTip")); //$NON-NLS-1$
        panel.add(chckbxReticle);
        chckbxReticle.setSelected(showReticle);
        chckbxReticle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowReticle(chckbxReticle.isSelected());
            }});
        
        chckbxLocations = new JCheckBox(
                Translations.getString("PlacementsHolderLocationViewer.ViewingOption.Locations")); //$NON-NLS-1$
        chckbxLocations.setToolTipText(
                Translations.getString("PlacementsHolderLocationViewer.ViewingOption.Locations.ToolTip")); //$NON-NLS-1$
        panel.add(chckbxLocations);
        chckbxLocations.setSelected(showLocations);
        chckbxLocations.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowLocations(chckbxLocations.isSelected());
            }});
        
        chckbxOrigins = new JCheckBox(
                Translations.getString("PlacementsHolderLocationViewer.ViewingOption.Origins")); //$NON-NLS-1$
        chckbxOrigins.setToolTipText(
                Translations.getString("PlacementsHolderLocationViewer.ViewingOption.Origins.ToolTip")); //$NON-NLS-1$
        panel.add(chckbxOrigins);
        chckbxOrigins.setSelected(showOrigins);
        chckbxOrigins.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowOrigins(chckbxOrigins.isSelected());
            }});
        
        chckbxFiducials = new JCheckBox(
                Translations.getString("PlacementsHolderLocationViewer.ViewingOption.Fiducials")); //$NON-NLS-1$
        chckbxFiducials.setToolTipText(
                Translations.getString("PlacementsHolderLocationViewer.ViewingOption.Fiducials.ToolTip")); //$NON-NLS-1$
        panel.add(chckbxFiducials);
        chckbxFiducials.setSelected(showFiducials);
        chckbxFiducials.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setShowFiducials(chckbxFiducials.isSelected());
            }});
        
        chckbxPlacements = new JCheckBox(
                Translations.getString("PlacementsHolderLocationViewer.ViewingOption.Placements")); //$NON-NLS-1$
        chckbxPlacements.setToolTipText(
                Translations.getString("PlacementsHolderLocationViewer.ViewingOption.Placements.ToolTip")); //$NON-NLS-1$
        panel.add(chckbxPlacements);
        chckbxPlacements.setSelected(showPlacements);
        
        verticalStrut_1 = Box.createVerticalStrut(15);
        panel.add(verticalStrut_1);
        
        lblNewLabel_4 = new JLabel(
                Translations.getString("PlacementsHolderLocationViewer.Hints")); //$NON-NLS-1$
        panel.add(lblNewLabel_4);
        
        lblNewLabel_5 = new JLabel(Translations.getString("PlacementsHolderLocationViewer.Hints.Colors.Top")); //$NON-NLS-1$
        panel.add(lblNewLabel_5);
        
        lblNewLabel_6 = new JLabel(Translations.getString("PlacementsHolderLocationViewer.Hints.Colors.Bottom")); //$NON-NLS-1$
        panel.add(lblNewLabel_6);
        
        lblNewLabel_1 = new JLabel(
                Translations.getString("PlacementsHolderLocationViewer.Hints.Zooming")); //$NON-NLS-1$
        panel.add(lblNewLabel_1);
        
        lblNewLabel_2 = new JLabel(
                Translations.getString("PlacementsHolderLocationViewer.Hints.Panning")); //$NON-NLS-1$
        panel.add(lblNewLabel_2);
        
        lblNewLabel_3 = new JLabel(
                Translations.getString("PlacementsHolderLocationViewer.Hints.RightClicking")); //$NON-NLS-1$
        panel.add(lblNewLabel_3);
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

        setPlacementsHolder(placementsHolder, selections);
        
        Configuration.get().getBus().register(this);
    }
    
    @Subscribe
    public void placementChangedEventHandler(PlacementChangedEvent evt) {
        String propName = evt.propertyName;
        //Possibilities are "partId", "type", "part", "comments", "errorHandling", "side", "enabled", "location", "placed"
        String regenerateArray[] = {"type", "side", "location"};
        String refreshArray[] = {"enabled"};
        List<String> regenerateOnPropertyChange = new ArrayList<>(Arrays.asList(regenerateArray));
        List<String> refreshOnPropertyChange = new ArrayList<>(Arrays.asList(refreshArray));
        
        //For now, we'll just regenerate or refresh everything but in the future we could be more 
        //specific based on the contents of the PlacementChangedEvent
        if (regenerateOnPropertyChange.contains(propName)) {
            SwingUtilities.invokeLater(() -> {
                regenerate();
            });
        }
        else if (refreshOnPropertyChange.contains(propName)) {
            SwingUtilities.invokeLater(() -> {
                refresh();
            });
        }
    }

    @Subscribe 
    public void placementsHolderChangedEventHandler(PlacementsHolderChangedEvent evt) {
        String propName = evt.propertyName;
        //Possibilities are "name", "dimensions"
        String regenerateArray[] = {"dimensions"};
        String refreshArray[] = {};
        List<String> regenerateOnPropertyChange = new ArrayList<>(Arrays.asList(regenerateArray));
        List<String> refreshOnPropertyChange = new ArrayList<>(Arrays.asList(refreshArray));
        
        //For now, we'll just regenerate or refresh everything but in the future we could be more 
        //specific based on the contents of the PlacementsHolderLocationChangedEvent
        if (regenerateOnPropertyChange.contains(propName)) {
            SwingUtilities.invokeLater(() -> {
                regenerate();
            });
        }
        else if (refreshOnPropertyChange.contains(propName)) {
            SwingUtilities.invokeLater(() -> {
                refresh();
            });
        }
    }
    
    @Subscribe
    public void placementsHolderLocationChangedEventHandler(PlacementsHolderLocationChangedEvent evt) {
        String propName = evt.propertyName;
        //Possibilities are "ALL", "id", "name", "dimensions", "side", "location", "locallyEnabled", "checkFiducials"
        String regenerateArray[] = {"ALL", "dimensions", "side", "location"};
        String refreshArray[] = {"locallyEnabled"};
        List<String> regenerateOnPropertyChange = new ArrayList<>(Arrays.asList(regenerateArray));
        List<String> refreshOnPropertyChange = new ArrayList<>(Arrays.asList(refreshArray));
        
        //For now, we'll just regenerate or refresh everything but in the future we could be more 
        //specific based on the contents of the PlacementsHolderLocationChangedEvent
        if (regenerateOnPropertyChange.contains(propName)) {
            SwingUtilities.invokeLater(() -> {
                regenerate();
            });
        }
        else if (refreshOnPropertyChange.contains(propName)) {
            SwingUtilities.invokeLater(() -> {
                refresh();
            });
        }
    }
    
    @Subscribe
    public void definitionStructureChangedEventHandler(DefinitionStructureChangedEvent evt) {
        if (isJob && ((Panel) placementsHolderLocation.getPlacementsHolder()).getDefinition().isDefinitionUsed((PlacementsHolder<?>) evt.definition)) {
            SwingUtilities.invokeLater(() -> {
                regenerate();
            });
        }
        else if ((evt.definition instanceof Board) && (placementsHolderLocation instanceof BoardLocation) &&
                (placementsHolderLocation.getPlacementsHolder().getDefinition() == ((Board) evt.definition).getDefinition())) {
            SwingUtilities.invokeLater(() -> {
                regenerate();
            });
        }
        else if ((placementsHolderLocation instanceof PanelLocation) &&
                ((Panel) placementsHolderLocation.getPlacementsHolder()).getDefinition().isDefinitionUsed((PlacementsHolder<?>) evt.definition)) {
            SwingUtilities.invokeLater(() -> {
                regenerate();
            });
        }
    }

    public void setPlacementsHolder(PlacementsHolder<?> placementsHolder, List<PlacementsHolderLocation<?>> selections) {
        if (placementsHolder == null) {
            return;
        }
        this.placementsHolder = placementsHolder;
        this.selections = selections;
        if (placementsHolder instanceof Board) {
            placementsHolderLocation = new BoardLocation((Board) placementsHolder);
        }
        else if (placementsHolder instanceof Panel) {
            placementsHolderLocation = new PanelLocation((Panel) placementsHolder);
            if (!isJob) {
                PanelLocation.setParentsOfAllDescendants((PanelLocation) placementsHolderLocation);
            }
        }
        else {
            throw new UnsupportedOperationException("Viewing of " + //$NON-NLS-1$
                    placementsHolder.getClass().getSimpleName() + 
                    " types is not currently supported"); //$NON-NLS-1$
        }
        regenerate();
    }
    
    public void regenerate() {
        if (isJob) {
            placementsHolderLocation = MainFrame.get().getJobTab().getJob().getRootPanelLocation();
        }
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
            viewPortBounds = new Rectangle.Double(EDGE_GAP, EDGE_GAP, 
                    currentSize.width - 2*EDGE_GAP, currentSize.height - 2*EDGE_GAP);
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
        objectToScreenTransform.translate(EDGE_GAP, (int) scrollingBounds.height - EDGE_GAP);
        objectToScreenTransform.scale(1, -1);
        objectToScreenTransform.concatenate(objectToViewTransform);

        viewToObjectTransform = new AffineTransform();
        viewToObjectTransform.translate(xOffset+viewableBounds.getX(), viewableBounds.getY());
        viewToObjectTransform.scale(sign*1.0/scaleFactor, 1.0/scaleFactor);

        screenToObjectTransform = new AffineTransform(viewToObjectTransform);
        screenToObjectTransform.scale(1, -1);
        screenToObjectTransform.translate(-EDGE_GAP, -((int) scrollingBounds.height - EDGE_GAP));
        
        viewableClippingBounds = new Rectangle.Double(viewableBounds.getX() - EDGE_GAP/scaleFactor,
                viewableBounds.getY() - EDGE_GAP/scaleFactor,
                viewableBounds.getWidth() + 2*EDGE_GAP/scaleFactor,
                viewableBounds.getHeight() + 2*EDGE_GAP/scaleFactor);
        
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
        boolean jobTopChild = isJob && placementsHolderLocation.getParent() == this.placementsHolderLocation;
        if (atRoot) {
            profileMap = new HashMap<>();
            placementMap = new HashMap<>();
            placementInverseMap = new HashMap<>();
            if (isJob) {
                //include (0,0) as this is the machine origin
                graphicsBounds = new Rectangle2D.Double(); 
            }
            else {
                graphicsBounds = null;
            }
        }
        if ((!atRoot || !isJob) && (viewingOption != ViewingOption.SELECTED || selections.indexOf(placementsHolderLocation) >= 0)) {
            if (graphicsBounds == null) {
                graphicsBounds = profile.getBounds2D();
            }
            else {
                graphicsBounds.add(profile.getBounds2D());
            }
            List<Placement> placements = new ArrayList<>(placementsHolderLocation.getPlacementsHolder().getPlacements());
            if ((atRoot || jobTopChild) && placementsHolderLocation instanceof PanelLocation) {
                placements.addAll(((PanelLocation) placementsHolderLocation).getPanel().getPseudoPlacements()); 
            }
            for (Placement placement : placements) {
                Location loc = Utils2D.calculateBoardPlacementLocation(placementsHolderLocation, placement).
                        convertToUnits(units);
                graphicsBounds.add(new Point2D.Double(loc.getX(), loc.getY()));
                
                AffineTransform at2 = new AffineTransform();
                at2.translate(loc.getX(), loc.getY());
                if (atRoot && placementsHolderLocation.getGlobalSide() != placement.getSide()) {
                    at2.rotate(-Math.toRadians(loc.getRotation()));
                }
                else {
                    at2.rotate(Math.toRadians(loc.getRotation()));
                }
                double d = (new Length(1, LengthUnit.Millimeters)).convertToUnits(units).getValue() * 0.5;
                //In the future, we probably should use the actual shape of the footprint here - will
                //probably wait until we can import Gerber files and get the true footprint from there
                if (placement.getType() == Placement.Type.Placement) {
                    Area area = new Area(
                            at2.createTransformedShape(new Rectangle2D.Double(-d, -d, 2*d, 2*d)));
                    placementMap.put(area, placement);
                    placementInverseMap.put(placement, area);
                }
                else if (placement.getType() == Placement.Type.Fiducial){
                    Area area = new Area(
                            at2.createTransformedShape(new Ellipse2D.Double(-d, -d, 2*d, 2*d)));
                    placementMap.put(area, placement);
                    placementInverseMap.put(placement, area);
                }
                
            }
            profileMap.put(new Area(profile), placementsHolderLocation);
        }
        if ((atRoot || viewingOption != ViewingOption.CHILDREN) && placementsHolderLocation instanceof PanelLocation) {
            for (PlacementsHolderLocation<?> child : ((PanelLocation) placementsHolderLocation).getChildren()) {
                if (viewingOption == ViewingOption.SELECTED && selections.indexOf(child) < 0 && !(child instanceof PanelLocation)) {
                    continue;
                }
                generateGraphicalObjects(child);
            }
        }
    }
    
    public void renderPlacementsHolderImage() {
        if (scrollingBounds != null && scrollingBounds.width > 0 && scrollingBounds.height > 0) {
            placementsHolderImage = new BufferedImage((int) scrollingBounds.width,
                    (int) scrollingBounds.height, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D offScr = (Graphics2D) placementsHolderImage.getGraphics();
            offScr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
            offScr.setColor(Color.BLACK);
            offScr.fillRect(0, 0, (int) scrollingBounds.width, (int) scrollingBounds.height);
            
            offScr.translate(EDGE_GAP, (int) scrollingBounds.height - EDGE_GAP);
            offScr.scale(1, -1);
            
            offScr.setStroke(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));

            //Save the original Paint so it can be restored when needed
            Paint originalPaint = offScr.getPaint();
            
            //Create a striped pattern to fill disabled Boards and Panels
            GradientPaint stripedPaint = new GradientPaint(5, 5, 
                    new Color(255, 0, 0, 128), 8, 8, new Color(0, 0, 0, 128), true);
            

            for (Area profile : profileMap.keySet() ) {
                if (profile.intersects(viewableClippingBounds)) {
                    PlacementsHolderLocation<?> phl = profileMap.get(profile);
                    
                    if (showPlacements || showFiducials) {
                        offScr.setStroke(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
                        PlacementsHolder<?> ph = phl.getPlacementsHolder();
                        List<Placement> placements = new ArrayList<>(ph.getPlacements());
                        boolean atRoot = phl == this.placementsHolderLocation;
                        boolean jobTopChild = isJob && phl.getParent() == this.placementsHolderLocation;

                        if ((atRoot || jobTopChild) && phl instanceof PanelLocation) {
                            placements.addAll(((PanelLocation) phl).getPanel().getPseudoPlacements()); 
                        }
                        for (Placement placement : placements) {
                            if ((viewFromTop && placement.getSide() == phl.getGlobalSide()) ||
                                    (!viewFromTop && placement.getSide() != phl.getGlobalSide())) {
                                if (showPlacements && placement.getType() == Placement.Type.Placement) {
                                    Area area = placementInverseMap.get(placement);
                                    if (area == null || area.isEmpty()) {
                                        continue;
                                    }
                                    offScr.setColor(placement.isEnabled() ? placementEnabledColor : placementDisabledColor);
                                    Shape placementShape = objectToViewTransform.createTransformedShape(area);
                                    offScr.draw(placementShape);
                                }
                                else if (showFiducials && placement.getType() == Placement.Type.Fiducial) {
                                    Area area = placementInverseMap.get(placement);
                                    if (area == null || area.isEmpty()) {
                                        continue;
                                    }
                                    offScr.setColor(placement.isEnabled() ? fiducialEnabledColor : fiducialDisabledColor);
                                    Shape placementShape = objectToViewTransform.createTransformedShape(area);
                                    offScr.fill(placementShape);
                                }
                            }
                        }
                    }
                    

                    if (phl == arrayRoot) {
                        offScr.setStroke(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND,
                                0, new float[]{(float) (EDGE_GAP * 0.75)}, 0));
                    }
                    else if (newArrayMembers != null && newArrayMembers.contains(phl)) {
                        offScr.setStroke(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND,
                                  0, new float[]{(float) (EDGE_GAP * 0.25)}, 0));
                    }
                    else {
                        offScr.setStroke(new BasicStroke(2, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
                    }
                    
                    if (profile == null || profile.isEmpty()) {
                        continue;
                    }

                    Shape profileShape = objectToViewTransform.createTransformedShape(profile);
                    if (!phl.isLocallyEnabled()) {
                        offScr.setPaint(stripedPaint);
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
        double d = EDGE_GAP/(1.0*scaleFactor);
        offScr.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        for (Area profileArea : profileMap.keySet()) {
            PlacementsHolderLocation<?> placementsHolderLocation = profileMap.get(profileArea);
            if (placementsHolderLocation != null) {
                Location offset = Location.origin;
                if ((viewFromTop && placementsHolderLocation.getGlobalSide() == Side.Bottom) ||
                        (!viewFromTop && placementsHolderLocation.getGlobalSide() == Side.Top)) {
                    offset = offset.add(new Location(units, 
                            placementsHolderLocation.getPlacementsHolder().getDimensions().getLengthX().
                            convertToUnits(units).getValue(), 0, 0, 0));
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
        double d = EDGE_GAP/(0.75*scaleFactor);
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
        double d = EDGE_GAP/(2.0*scaleFactor);
        offScr.setStroke(new BasicStroke(1, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        for (Area profileArea : profileMap.keySet()) {
            PlacementsHolderLocation<?> placementsHolderLocation = profileMap.get(profileArea);
            List<Placement> placements = new ArrayList<>(placementsHolderLocation.getPlacementsHolder().getPlacements());
            boolean atRoot = placementsHolderLocation == this.placementsHolderLocation;
            boolean jobTopChild = isJob && placementsHolderLocation.getParent() == this.placementsHolderLocation;
            if ((atRoot || jobTopChild) && placementsHolderLocation instanceof PanelLocation) {
                placements.addAll(((PanelLocation) placementsHolderLocation).getPanel().getPseudoPlacements()); 
            }
            for (Placement placement : placements) {
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
        double d = EDGE_GAP/(2.0*scaleFactor);
        for (Area profileArea : profileMap.keySet()) {
            PlacementsHolderLocation<?> placementsHolderLocation = profileMap.get(profileArea);
            List<Placement> placements = new ArrayList<>(placementsHolderLocation.getPlacementsHolder().getPlacements());
            boolean atRoot = placementsHolderLocation == this.placementsHolderLocation;
            boolean jobTopChild = isJob && placementsHolderLocation.getParent() == this.placementsHolderLocation;
            if ((atRoot || jobTopChild) && placementsHolderLocation instanceof PanelLocation) {
                placements.addAll(((PanelLocation) placementsHolderLocation).getPanel().getPseudoPlacements()); 
            }
            for (Placement placement : placements) {
                if (placement.getType() == Placement.Type.Fiducial && 
                        ((viewFromTop && placement.getSide() == placementsHolderLocation.getGlobalSide()) || 
                        (!viewFromTop && placement.getSide() != placementsHolderLocation.getGlobalSide()))) {
                    Location localLocation = placement.getLocation().multiply(1, 1, 1, viewFromTop ? 1 : -1);
                    Location location = Utils2D.calculateBoardPlacementLocation(placementsHolderLocation, localLocation);
                    overlayLocationMark(offScr, d, location);
                }
            }
        }
    }
    
    private void overlayReticle(Graphics2D offScr) {
        double unitScaling = 1;
        String displayUnits;
        switch (units) {
            case Millimeters:
            case Centimeters:
            case Microns:
            case Meters:
                displayUnits = "Metric"; //$NON-NLS-1$
                break;
            case Feet:
            case Inches:
            case Mils:
                displayUnits = "Imperial"; //$NON-NLS-1$
                break;
            default:
                displayUnits = "Metric"; //$NON-NLS-1$
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
        
        if (displayUnits.equals("Metric")) { //$NON-NLS-1$
            if (unitsPerDivision > 1) {
                displayUnit = "mm"; //$NON-NLS-1$
                displayMultiplier = 1;
                displayDecimals = 0;
            }
            else if (unitsPerDivision > 0.1) {
                displayUnit = "mm"; //$NON-NLS-1$
                displayMultiplier = 1;
                displayDecimals = 1;
            }
            else {
                displayUnit = "um"; //$NON-NLS-1$
                displayMultiplier = 1000;
                displayDecimals = 0;
            }
        }
        else {
            if (unitsPerDivision >= 1) {
                displayUnit = "in"; //$NON-NLS-1$
                displayMultiplier = 1;
                displayDecimals = 0;
            }
            else if (unitsPerDivision >= 0.1) {
                displayUnit = "in"; //$NON-NLS-1$
                displayMultiplier = 1;
                displayDecimals = 1;
            }
            else {
                displayUnit = "mil"; //$NON-NLS-1$
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
        
        if (showReticle) {
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
                    offScr.drawLine((int) mid.getX(), (int) (mid.getY()-tickLengthOver2), 
                            (int) mid.getX(), (int) (mid.getY()+tickLengthOver2));
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
        
        offScr.translate(0, (int) scrollingBounds.height - EDGE_GAP);
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
            
            offScr.drawLine(VERTICAL_SCALE_WIDTH-SCALE_TICK_LENGTH, (int) tick.getY(), 
                    VERTICAL_SCALE_WIDTH, (int) tick.getY());

            String text = String.format("%." + displayDecimals + "f", y*displayMultiplier); //$NON-NLS-1$ //$NON-NLS-2$
            TextLayout textTl = new TextLayout(text, f, frc);
        
            AffineTransform transform = new AffineTransform();
            transform.translate(VERTICAL_SCALE_WIDTH-SCALE_TICK_LENGTH-textTl.getBounds().getWidth()-2, 
                    tick.getY()-textTl.getBounds().getHeight()/2);
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

        horizontalScaleImage = new BufferedImage((int) scrollingBounds.width, 
                HORIZONTAL_SCALE_HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D offScr = (Graphics2D) horizontalScaleImage.getGraphics();
        offScr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        offScr.setColor(drawingPanelColumnHeader.getBackground());
        offScr.fillRect(0, 0, (int) scrollingBounds.width, HORIZONTAL_SCALE_HEIGHT);
        
        offScr.translate(EDGE_GAP, 0);
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
            
            offScr.drawLine((int) tick.getX(), HORIZONTAL_SCALE_HEIGHT, (int) tick.getX(), 
                    HORIZONTAL_SCALE_HEIGHT-SCALE_TICK_LENGTH);
            
            String text = String.format("%." + displayDecimals + "f", x*displayMultiplier); //$NON-NLS-1$ //$NON-NLS-2$
            TextLayout textTl = new TextLayout(text, f, frc);
            AffineTransform transform = new AffineTransform();
            transform.translate(tick.getX()-textTl.getBounds().getWidth()/2, 
                    HORIZONTAL_SCALE_HEIGHT - 2 - textTl.getBounds().getHeight());
            Shape outline = transform.createTransformedShape(textTl.getOutline(null));
            offScr.fill(outline);
            
            x += unitsPerDivision;
        }
        
        offScr.dispose();
    }

    public void cancel() {
        Configuration.get().getBus().unregister(this);
    }
    
    private void displayPopupMenu(MouseEvent e) {
        Point point = e.getPoint();
        Point2D imagePoint = screenToObjectTransform.transform(point, null);
        
        //Find the deepest nested PlacementsHolder that contains the clicked-on point
        PlacementsHolderLocation<?> potentialPlacementsHolderLocation = null;
        String uniqueId = ""; //$NON-NLS-1$
        for (Area a : profileMap.keySet()) {
            if (a.contains(imagePoint)) {
                String potentialId = profileMap.get(a).getUniqueId();
                if (potentialId == null) {
                    potentialId = ""; //$NON-NLS-1$
                }
                if (potentialId.length() >= uniqueId.length()) {
                    uniqueId = potentialId;
                    potentialPlacementsHolderLocation = profileMap.get(a);
                }
            }
        }
        final PlacementsHolderLocation<?> phl = potentialPlacementsHolderLocation;
        
        //Check to see if a Placement contains the clicked-on point
        Placement placement = null;
        Side visibleSide;
        if (showPlacements || showFiducials) {
            Collection<Placement> placements;
            if (phl != null) {
                placements = phl.getPlacementsHolder().getPlacements();
                visibleSide = phl.getGlobalSide().flip(!viewFromTop);
            }
            else {
                placements = placementInverseMap.keySet();
                visibleSide = Side.Top.flip(!viewFromTop);
            }
            for (Placement plmt : placements) {
                if (plmt.getSide() == visibleSide  && 
                        ((showPlacements && (plmt.getType() == Placement.Type.Placement)) ||
                                (showFiducials && (plmt.getType() == Placement.Type.Fiducial)))) {
                    if (placementInverseMap.get(plmt).contains(imagePoint)) {
                        placement = plmt;
                        break;
                    }
                }
            }
        }
        
        boolean enablable = isJob || ((uniqueId.equals("") && placement != null)) || //$NON-NLS-1$
                (phl != null && placementsHolder instanceof Panel &&
                ((Panel) placementsHolder).getChildren().contains(phl) && (placement == null)) ||
                ((phl == null) && (placement != null));
        
        if (placement != null) {
            displayPlacementPopupMenu(point, phl, uniqueId, placement, enablable);
        }
        else if (phl != null) {
            displayPlacementsHolderLocationPopupMenu(point, phl, uniqueId, enablable);
        }
    }

    private void displayPlacementPopupMenu(Point point, PlacementsHolderLocation<?> phl, 
            String uniqueId, Placement placement, boolean enablable) {
        final Placement plmt = placement;
        String type = plmt.getType() == Placement.Type.Fiducial ?
                Translations.getString("Placement.Type.Fiducial") : //$NON-NLS-1$
                Translations.getString("Placement.Type.Placement"); //$NON-NLS-1$
        String side = phl.getGlobalSide() == Side.Top ?
                Translations.getString("Placement.Side.Top") : //$NON-NLS-1$
                Translations.getString("Placement.Side.Bottom"); //$NON-NLS-1$
        String popupLabel = "<pre>" + //$NON-NLS-1$
                "<b>" + //$NON-NLS-1$
                (plmt.getType() == Placement.Type.Placement ?
                        Translations.getString("PlacementsHolderLocationViewer.Placement.Type.Placement") : //$NON-NLS-1$
                            Translations.getString("PlacementsHolderLocationViewer.Placement.Type.Fiducial")) + //$NON-NLS-1$
                "</b><br>" + //$NON-NLS-1$
               Translations.getString("PlacementsHolderLocationViewer.PopupMenu.Label.Id") + //$NON-NLS-1$
                (uniqueId.contentEquals("") ? "" :  //$NON-NLS-1$ //$NON-NLS-2$
                    uniqueId + PlacementsHolderLocation.ID_DELIMITTER) + //$NON-NLS-1$ //$NON-NLS-2$
                plmt.getId() +
                "<br>" + //$NON-NLS-1$
                Translations.getString("PlacementsHolderLocationViewer.PopupMenu.Label.Part") + //$NON-NLS-1$
                (plmt.getPart() != null ? plmt.getPart().getId() : "unassigned") + //$NON-NLS-1$
                "<br>" + //$NON-NLS-1$
                Translations.getString("PlacementsHolderLocationViewer.PopupMenu.Label.Side") + //$NON-NLS-1$
                side +
                "</pre>"; //$NON-NLS-1$
        LabeledPopupMenu popUp = new LabeledPopupMenu(popupLabel);
        JCheckBoxMenuItem mnItmEnabled = new JCheckBoxMenuItem(
                Translations.getString("PlacementsHolderLocationViewer.PopupMenu.Enabled")); //$NON-NLS-1$
        mnItmEnabled.setSelected(plmt.isEnabled());
        mnItmEnabled.addActionListener(new ActionListener() {
    
            @Override
            public void actionPerformed(ActionEvent e) {
                Object oldValue = plmt.isEnabled();
                plmt.setEnabled(mnItmEnabled.isSelected());
                Configuration.get().getBus().post(new PlacementChangedEvent(phl.getPlacementsHolder(), plmt, "enabled", oldValue, mnItmEnabled.isSelected(), PlacementsHolderLocationViewer.this)); //$NON-NLS-1$
            }
            
        });
        mnItmEnabled.setEnabled(enablable);
        popUp.add(mnItmEnabled);
        
        if (isJob) {
            popUp.addSeparator();
            JCheckBoxMenuItem mnItmPlaced = new JCheckBoxMenuItem(
                    Translations.getString("PlacementsHolderLocationViewer.PopupMenu.Placed")); //$NON-NLS-1$
            mnItmPlaced.setSelected(MainFrame.get().getJobTab().getJob().retrievePlacedStatus(phl, plmt.getId()));
            mnItmPlaced.addActionListener(new ActionListener() {
        
                @Override
                public void actionPerformed(ActionEvent e) {
                    Object oldValue = MainFrame.get().getJobTab().getJob().retrievePlacedStatus(phl, plmt.getId());
                    MainFrame.get().getJobTab().getJob().storePlacedStatus(phl, plmt.getId(), mnItmPlaced.isSelected());
                    Configuration.get().getBus().post(new PlacementChangedEvent(phl.getPlacementsHolder(), plmt, "placed", oldValue, mnItmPlaced.isSelected(), PlacementsHolderLocationViewer.this)); //$NON-NLS-1$
                }
                
            });
            mnItmPlaced.setEnabled(enablable);
            popUp.add(mnItmPlaced);
        }

        if (isJob) {
            popUp.addSeparator();
            JMenuItem mnItemMoveCamera = new JMenuItem(
                    Translations.getString("PlacementsHolderLocationViewer.PopupMenu.CenterCamera") + type); //$NON-NLS-1$
            mnItemMoveCamera.setIcon(Icons.centerCamera);
            mnItemMoveCamera.setToolTipText(
                    Translations.getString("PlacementsHolderLocationViewer.PopupMenu.CenterCamera.MenuTip") + type); //$NON-NLS-1$
            mnItemMoveCamera.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    UiUtils.submitUiMachineTask(() -> {
                        Location location = Utils2D.calculateBoardPlacementLocation(phl, plmt);

                        Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                                .getDefaultCamera();
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                        MovableUtils.fireTargetedUserAction(camera);

                        Map<String, Object> globals = new HashMap<>();
                        globals.put("camera", camera); //$NON-NLS-1$
                        Configuration.get().getScripting().on("Camera.AfterPosition", globals); //$NON-NLS-1$
                    });
                }});
            popUp.add(mnItemMoveCamera);
        }
        popUp.show(drawingPanel, point.x, point.y);
    }

    private void displayPlacementsHolderLocationPopupMenu(Point point, PlacementsHolderLocation<?> phl, String uniqueId, boolean enablable) {
        String popupLabel = "<pre>" + //$NON-NLS-1$
                "<b>" + //$NON-NLS-1$
                (phl instanceof BoardLocation ? 
                        Translations.getString("PlacementsHolderLocationViewer.PopupMenu.Label.Board") : //$NON-NLS-1$
                            Translations.getString("PlacementsHolderLocationViewer.PopupMenu.Label.Panel")) + //$NON-NLS-1$
                "</b>" + //$NON-NLS-1$
                (uniqueId.equals("") ? "" : //$NON-NLS-1$ //$NON-NLS-2$
                    "<br>" + //$NON-NLS-1$
                    Translations.getString("PlacementsHolderLocationViewer.PopupMenu.Label.Id") + //$NON-NLS-1$
                    uniqueId) +
                "<br>" + //$NON-NLS-1$
                Translations.getString("PlacementsHolderLocationViewer.PopupMenu.Label.Name") + //$NON-NLS-1$
                phl.getPlacementsHolder().getName() +
                "<br>" + //$NON-NLS-1$
                Translations.getString("PlacementsHolderLocationViewer.PopupMenu.Label.Side") + //$NON-NLS-1$
                phl.getGlobalSide().flip(!viewFromTop) +
                "</pre>"; //$NON-NLS-1$
        LabeledPopupMenu popUp = new LabeledPopupMenu(popupLabel);
        JCheckBoxMenuItem mnItmEnabled = new JCheckBoxMenuItem(
                Translations.getString("PlacementsHolderLocationViewer.PopupMenu.Enabled")); //$NON-NLS-1$
        mnItmEnabled.setSelected(phl.isLocallyEnabled());
        mnItmEnabled.addActionListener(new ActionListener() {
    
            @Override
            public void actionPerformed(ActionEvent e) {
                Object oldValue = phl.isLocallyEnabled();
                phl.setLocallyEnabled(mnItmEnabled.isSelected());
                Configuration.get().getBus().post(new PlacementsHolderLocationChangedEvent(phl, 
                        "locallyEnabled", oldValue, mnItmEnabled.isSelected(), //$NON-NLS-1$
                        PlacementsHolderLocationViewer.this));
            }
            
        });
        mnItmEnabled.setEnabled(enablable);
        popUp.add(mnItmEnabled);
        
        JCheckBoxMenuItem mnItmCheckFids = new JCheckBoxMenuItem(
                Translations.getString("PlacementsHolderLocationViewer.PopupMenu.CheckFids")); //$NON-NLS-1$
        mnItmCheckFids.setSelected(phl.isCheckFiducials());
        mnItmCheckFids.addActionListener(new ActionListener() {
    
            @Override
            public void actionPerformed(ActionEvent e) {
                Object oldValue = phl.isCheckFiducials();
                phl.setCheckFiducials(mnItmCheckFids.isSelected());
                Configuration.get().getBus().post(new PlacementsHolderLocationChangedEvent(phl, 
                        "checkFiducials", oldValue, mnItmCheckFids.isSelected(), //$NON-NLS-1$
                        PlacementsHolderLocationViewer.this));
            }
            
        });
        mnItmCheckFids.setEnabled(enablable);
        popUp.add(mnItmCheckFids);
        
        if (isJob) {
            popUp.addSeparator();
            String type = phl instanceof BoardLocation ?
                    Translations.getString("PlacementsHolderLocationViewer.PopupMenu.BoardLocation") : //$NON-NLS-1$
                    Translations.getString("PlacementsHolderLocationViewer.PopupMenu.PanelLocation"); //$NON-NLS-1$
            JMenuItem mnItemMoveCamera = new JMenuItem(
                    Translations.getString("PlacementsHolderLocationViewer.PopupMenu.CenterCamera") + type); //$NON-NLS-1$
            mnItemMoveCamera.setIcon(Icons.centerCamera);
            mnItemMoveCamera.setToolTipText(
                    Translations.getString("PlacementsHolderLocationViewer.PopupMenu.CenterCamera.MenuTip") + type); //$NON-NLS-1$
            mnItemMoveCamera.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    UiUtils.submitUiMachineTask(() -> {
                        Placement dummy = new Placement("Dummy"); //$NON-NLS-1$
                        dummy.removePropertyChangeListener(dummy);
                        if (phl.getGlobalSide() == Side.Bottom) {
                            dummy.setLocation(Location.origin.deriveLengths(
                                    phl.getPlacementsHolder().getDimensions().getLengthX(), null, null, null));
                        }
                        Location location = Utils2D.calculateBoardPlacementLocation(phl, dummy);
                        
                        Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                                .getDefaultCamera();
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                        MovableUtils.fireTargetedUserAction(camera);

                        Map<String, Object> globals = new HashMap<>();
                        globals.put("camera", camera); //$NON-NLS-1$
                        Configuration.get().getScripting().on("Camera.AfterPosition", globals); //$NON-NLS-1$
                    });
                }});
            popUp.add(mnItemMoveCamera);
            
            popUp.addSeparator();
            JMenuItem mnItemCheckFiducials = new JMenuItem(
                    Translations.getString("PlacementsHolderLocationViewer.PopupMenu.CheckFiducials") + type); //$NON-NLS-1$
            mnItemCheckFiducials.setIcon(Icons.fiducialCheck);
            mnItemCheckFiducials.setToolTipText(
                    Translations.getString("PlacementsHolderLocationViewer.PopupMenu.CheckFiducials.MenuTip") + type); //$NON-NLS-1$
            mnItemCheckFiducials.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    UiUtils.submitUiMachineTask(() -> {
                        Location location = Configuration.get().getMachine().getFiducialLocator()
                                .locatePlacementsHolder(phl);
                        
                        /**
                         * Update the board/panel's location to the one returned from the fiducial check. We
                         * have to store and restore the placement transform because setting the location
                         * clears it.  Note that we only update the location if the board/panel is
                         * not a part of another panel.
                         */
                        if (phl.getParent() == MainFrame.get().getJobTab().getJob().getRootPanelLocation()) {
                            AffineTransform tx = phl.getLocalToGlobalTransform();
                            phl.setLocation(location);
                            phl.setLocalToGlobalTransform(tx);
                        }
                        Helpers.selectObjectTableRow(MainFrame.get().getJobTab().
                                getPlacementsHolderLocationsTable(), phl);
                        
                        SwingUtilities.invokeLater(() -> {
                            MainFrame.get().getJobTab().refreshSelectedRow();
                        });
                        
                        /**
                         * Move the camera to the calculated position.
                         */
                        HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                        Camera camera = tool.getHead().getDefaultCamera();
                        MovableUtils.moveToLocationAtSafeZ(camera, location);
                        MovableUtils.fireTargetedUserAction(camera);
                    });
                }
            });
            popUp.add(mnItemCheckFiducials);
        }
        
        popUp.show(drawingPanel, point.x, point.y);
    }

    public boolean isViewFromTop() {
        return viewFromTop;
    }

    public void setViewFromTop(boolean viewFromTop) {
        this.viewFromTop = viewFromTop;
        if (viewFromTop) {
            btnViewingSide.setText(
                    Translations.getString("PlacementsHolderLocationViewer.ViewingSide.Top")); //$NON-NLS-1$
        }
        else {
            btnViewingSide.setText(
                    Translations.getString("PlacementsHolderLocationViewer.ViewingSide.Bottom")); //$NON-NLS-1$
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

    public void setViewingOption(ViewingOption option) {
        viewingOption = option;
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        regenerate();
    }
}
