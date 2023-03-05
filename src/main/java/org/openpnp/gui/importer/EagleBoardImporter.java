/*
 * Copyright (C) 2015 Douglas Pearless <Douglas.Pearless@gmail.com>
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

package org.openpnp.gui.importer;

import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.TitledBorder;

import org.openpnp.Translations;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.BoardPad;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Pad;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Point;
import org.openpnp.model.eagle.EagleLoader;
import org.openpnp.model.eagle.xml.Element;
import org.openpnp.model.eagle.xml.Layer;
import org.openpnp.model.eagle.xml.Library;
import org.openpnp.model.eagle.xml.Param;
import org.openpnp.model.eagle.xml.Vertex;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class EagleBoardImporter implements BoardImporter {
    private final static String NAME = "CadSoft EAGLE Board"; //$NON-NLS-1$
    private final static String DESCRIPTION =
            Translations.getString("EagleBoardImporter.Importer.Description"); //$NON-NLS-1$

    private static Board board;
    private File boardFile;
    static private Double mil_to_mm = 0.0254;

    @Override
    public String getImporterName() {
        return NAME;
    }

    @Override
    public String getImporterDescription() {
        return DESCRIPTION;
    }

    @Override
    public Board importBoard(Frame parent) throws Exception {
        Dlg dlg = new Dlg(parent);
        dlg.setVisible(true);
        return board;
    }

    private static List<Placement> parseFile(File file, Side side, boolean createMissingParts,
            boolean updateExistingParts, boolean addLibraryPrefix) throws Exception {

        String dimensionLayer = ""; //$NON-NLS-1$
        String topLayer = ""; //$NON-NLS-1$
        String bottomLayer = ""; //$NON-NLS-1$
        String tCreamLayer = ""; //$NON-NLS-1$
        String bCreamLayer = ""; //$NON-NLS-1$

        String mmMinCreamFrame_string;
        double mmMinCreamFrame_number = 0;
        String mmMaxCreamFrame_string;
        double mmMaxCreamFrame_number = 0;
        String libraryId = ""; //$NON-NLS-1$
        String packageId = ""; //$NON-NLS-1$

        List<BoardPad> pads = new ArrayList<>();

        // Keep track of unique parts as we see them, so we don't update them for every placement
        HashMap<String, Part> parts = new HashMap<String, Part>();

        ArrayList<Placement> placements = new ArrayList<>();
        // we don't use the 'side' parameter as we can read this from the .brd file in the future we
        // could use the side parameter to restrict this from only parsing one side or the other or
        // both

        EagleLoader boardToProcess = new EagleLoader(file);
        if (boardToProcess.board != null) {

            // first establish which is the Dimension, Top, Bottom, tCream and bCream layers in case
            // the board has non-standard layer numbering
            for (Layer layer : boardToProcess.layers.getLayer()) {
                if (layer.getName()
                         .equalsIgnoreCase("Dimension")) { //$NON-NLS-1$
                    dimensionLayer = layer.getNumber();
                }
                else if (layer.getName()
                              .equalsIgnoreCase("Top")) { //$NON-NLS-1$
                    topLayer = layer.getNumber();
                }
                else if (layer.getName()
                              .equalsIgnoreCase("Bottom")) { //$NON-NLS-1$
                    bottomLayer = layer.getNumber();
                }
                else if (layer.getName()
                              .equalsIgnoreCase("tCream")) { //$NON-NLS-1$
                    tCreamLayer = layer.getNumber();
                }
                else if (layer.getName()
                              .equalsIgnoreCase("bCream")) { //$NON-NLS-1$
                    bCreamLayer = layer.getNumber();
                }
            }

            // Now we want to establish the width of the board which we need to record
            Double x_boundary = 0.0;
            for (Object e : boardToProcess.board.getPlain()
                                                .getPolygonOrWireOrTextOrDimensionOrCircleOrRectangleOrFrameOrHole()) {
                if (e instanceof org.openpnp.model.eagle.xml.Wire) {
                    if (((org.openpnp.model.eagle.xml.Wire) e).getLayer()
                                                              .equalsIgnoreCase(dimensionLayer)) {
                        x_boundary = Math.max(x_boundary,
                                Double.parseDouble(((org.openpnp.model.eagle.xml.Wire) e).getX1()));
                        x_boundary = Math.max(x_boundary,
                                Double.parseDouble(((org.openpnp.model.eagle.xml.Wire) e).getX2()));
                    }
                }
            }
            Point center = new Point(x_boundary / 2, 0); // note that we set x = maximum x point on
                                                         // the Y=0;

            // determine the parameters for the pads based on DesignRules
            for (Param params : boardToProcess.board.getDesignrules()
                                                    .getParam()) {

                if (params.getName()
                          .compareToIgnoreCase("mlMinCreamFrame") == 0) { // found exact match when //$NON-NLS-1$
                                                                          // 0 returned
                    mmMinCreamFrame_string = params.getValue()
                                                   .replaceAll("[A-Za-z ]", ""); // remove all //$NON-NLS-1$ //$NON-NLS-2$
                                                                                 // letters, i.e.
                                                                                 // 0mil becomes 0
                    if (params.getValue()
                              .toUpperCase()
                              .endsWith("MIL")) { //$NON-NLS-1$
                        mmMinCreamFrame_number =
                                Double.parseDouble(mmMinCreamFrame_string) * mil_to_mm;
                    }
                    else if (params.getValue()
                                   .toUpperCase()
                                   .endsWith("MM")) { //$NON-NLS-1$
                        mmMinCreamFrame_number =
                                Double.parseDouble(mmMinCreamFrame_string) * mil_to_mm;
                    }
                    else {
                        throw new Exception("mlMinCream must either be in mil or mm"); // Force the //$NON-NLS-1$
                                                                                       // importer
                                                                                       // to abort,
                                                                                       // something
                                                                                       // is very
                                                                                       // wrong
                    }
                }
                if (params.getName()
                          .compareToIgnoreCase("mlMaxCreamFrame") == 0) { // found exact match when //$NON-NLS-1$
                                                                          // 0 returned
                    mmMaxCreamFrame_string = params.getValue()
                                                   .replaceAll("[A-Za-z ]", ""); // remove all //$NON-NLS-1$ //$NON-NLS-2$
                                                                                 // letters, i.e.
                                                                                 // "0mil" becomes 0
                    if (params.getValue()
                              .toUpperCase()
                              .endsWith("MIL")) { //$NON-NLS-1$
                        mmMaxCreamFrame_number =
                                Double.parseDouble(mmMaxCreamFrame_string) * mil_to_mm;
                    }
                    else if (params.getValue()
                                   .toUpperCase()
                                   .endsWith("MM")) { //$NON-NLS-1$
                        mmMaxCreamFrame_number = Double.parseDouble(mmMaxCreamFrame_string);
                    }
                    else {
                        throw new Exception("mlMaxCream must either be in mil or mm"); // Force the //$NON-NLS-1$
                                                                                       // importer
                                                                                       // to abort,
                                                                                       // something
                                                                                       // is very
                                                                                       // wrong
                    }
                }
            }
            // Now we know the min and max tolerance for the cream (aka solder paste) which are
            // mmMinCreamFrame_number and mmMaxCreamFrame_number and are in mm (converted from mil
            // as required)

            // Now we got through each of the parts
            if (!boardToProcess.board.getElements()
                                     .getElement()
                                     .isEmpty()) {

                // Process each of the element items
                for (Element element : boardToProcess.board.getElements()
                                                           .getElement()) {
                    // first we determine if the part is on the top layer or bottom layer

                    Side element_side;
                    String rot = element.getRot();
                    if (rot.toUpperCase()
                           .startsWith("M")) { //$NON-NLS-1$
                        // The part is mirrored and therefore is on the bottom of the board
                        element_side = Side.Bottom;
                    }
                    else {
                        element_side = Side.Top;
                    }

                    // Now determine if we want to process this part based on which side of the
                    // board it is on

                    if (side != null) { // null means process both sides
                        if (side != element_side) {
                            continue; // exit this loop and process the next element
                        }
                    }

                    String rot_number = rot.replaceAll("[A-Za-z ]", ""); // remove all letters, i.e. //$NON-NLS-1$ //$NON-NLS-2$
                                                                         // R180 becomes 180

                    Placement placement = new Placement(element.getName());
                    double rotation = Double.parseDouble(rot_number);
                    double x = Double.parseDouble(element.getX());
                    double y = Double.parseDouble(element.getY());
                    placement.setLocation(new Location(LengthUnit.Millimeters, x, y, 0, rotation));

                    // Get all SMD pads and polygons associated with this package
                    packageId = element.getPackage(); // Package
                    libraryId = element.getLibrary(); // Library that contains the package

                    List<Object> polys = new ArrayList<>();

                    if (!boardToProcess.board.getLibraries()
                                             .getLibrary()
                                             .isEmpty()) {
                        for (Library library : boardToProcess.board.getLibraries()
                                                                   .getLibrary()) {
                            if (library.getName()
                                       .equalsIgnoreCase(libraryId)) {
                                // we have found the library, now to scan for the package we want
                                if (!library.getPackages()
                                            .getPackage()
                                            .isEmpty()) {

                                    ListIterator<org.openpnp.model.eagle.xml.Package> it =
                                            library.getPackages()
                                                   .getPackage()
                                                   .listIterator();

                                    while (it.hasNext()) {

                                        org.openpnp.model.eagle.xml.Package pak =
                                                (org.openpnp.model.eagle.xml.Package) it.next();
                                        if (pak.getName()
                                               .equalsIgnoreCase(packageId)) {

                                            for (Object e : pak.getPolygonOrWireOrTextOrDimensionOrCircleOrRectangleOrFrameOrHoleOrPadOrSmd()) {
                                                if (e instanceof org.openpnp.model.eagle.xml.Smd
                                                        || e instanceof org.openpnp.model.eagle.xml.Pad
                                                        || e instanceof org.openpnp.model.eagle.xml.Polygon) {
                                                    polys.add(e);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // placement now contains where the package is on the PCB, we need to work out
                    // where the pads are relative to the 'placement'
                    Configuration cfg = Configuration.get();
                    Part part = null;

                    if (cfg != null && (createMissingParts || updateExistingParts)) {
                        String value = element.getValue(); // Value

                        String pkgId = addLibraryPrefix ? libraryId + "-" + packageId : packageId; //$NON-NLS-1$
                        String partId = value.trim()
                                             .length() > 0 ? pkgId + "-" + value : pkgId; //$NON-NLS-1$

                        // Only create or update a part the first time we encounter it
                        if (!parts.containsKey(partId)) {
                            part = cfg.getPart(partId);
                            Package pkg = cfg.getPackage(pkgId);

                            if ((pkg == null && createMissingParts)
                                    || (pkg != null && updateExistingParts)) {
                                if (pkg == null) {
                                    pkg = new Package(pkgId);
                                }

                                org.openpnp.model.Footprint fp = new org.openpnp.model.Footprint();

                                for (Object e : polys) {
                                    if (e instanceof org.openpnp.model.eagle.xml.Smd) {
                                        org.openpnp.model.eagle.xml.Smd s =
                                                (org.openpnp.model.eagle.xml.Smd) e;
                                        org.openpnp.model.Footprint.Pad p =
                                                new org.openpnp.model.Footprint.Pad();

                                        p.setName(s.getName());
                                        p.setX(Double.parseDouble(s.getX()));
                                        p.setY(Double.parseDouble(s.getY()));
                                        p.setWidth(Double.parseDouble(s.getDx()));
                                        p.setHeight(Double.parseDouble(s.getDy()));
                                        p.setRotation(Double.parseDouble(s.getRot()
                                                                          .replaceAll("[A-Za-z]", //$NON-NLS-1$
                                                                                  ""))); //$NON-NLS-1$
                                        p.setRoundness(Double.parseDouble(s.getRoundness()));

                                        fp.addPad(p);
                                    }
                                }

                                pkg.setFootprint(fp); // add the footprint to the package
                                cfg.addPackage(pkg); // save the package in the configuration file
                            }

                            if ((part == null && createMissingParts)
                                    || (part != null && updateExistingParts)) {
                                if (part == null) {
                                    part = new Part(partId);
                                }

                                part.setPackage(pkg);
                                // TODO part.setLibrary(libraryId);
                                cfg.addPart(part); // save the part in the configuration file
                            }

                            parts.put(partId, part); // keep track of parts we've already created or
                                                     // updated
                        }
                        else {
                            part = parts.get(partId);
                        }
                    }
                    placement.setPart(part);

                    // Now we have the part, we now need to add the SolderPastePad to the board
                    // Note, Eagle has the concept of minimum and max from the edge of the pad so we
                    // need to adjust the pad to be the size as the mid-point between the minimum
                    // and max in practice these are usually 0, which means we paste the entire pad

                    // TODO: This desperately needs to be broken up into functions. This function is
                    // way too long and wide.
                    for (Object e : polys) {
                        if (e instanceof org.openpnp.model.eagle.xml.Smd) {
                            // we have found the correct package in the correct library and we need
                            // to to add the pad to the boardPads

                            if (!((org.openpnp.model.eagle.xml.Smd) e).getCream()
                                                                      .equalsIgnoreCase("No")) { // if //$NON-NLS-1$
                                                                                                 // cream="no"
                                                                                                 // then
                                                                                                 // we
                                                                                                 // do
                                                                                                 // not
                                                                                                 // paste
                                                                                                 // this
                                                                                                 // pad

                                Pad.RoundRectangle pad = new Pad.RoundRectangle();
                                pad.setUnits(LengthUnit.Millimeters);

                                // TODO check that these reduce the pad to the halfway between the
                                // minimum & maximum tolerances
                                pad.setHeight(Double.parseDouble(
                                        ((org.openpnp.model.eagle.xml.Smd) e).getDx())
                                        - (mmMaxCreamFrame_number - mmMinCreamFrame_number) / 2);
                                pad.setWidth(Double.parseDouble(
                                        ((org.openpnp.model.eagle.xml.Smd) e).getDy())
                                        - (mmMaxCreamFrame_number - mmMinCreamFrame_number) / 2);

                                pad.setRoundness(0);
                                pad.setRoundness(Double.parseDouble(
                                        ((org.openpnp.model.eagle.xml.Smd) e).getRoundness()));

                                // first find out how is the package defined
                                Double pad_rotation = Double.parseDouble(rot_number);
                                // now rotate the pad by its own rotation relative to its origin and
                                // make sure we don't turn through 360 degrees
                                pad_rotation += Double.parseDouble(
                                        ((org.openpnp.model.eagle.xml.Smd) e).getRot()
                                                                             .replaceAll(
                                                                                     "[A-Za-z ]", //$NON-NLS-1$
                                                                                     "")) //$NON-NLS-1$
                                        % 360;

                                Point a = new Point(
                                        Double.parseDouble(
                                                ((org.openpnp.model.eagle.xml.Smd) e).getX()) + x,
                                        Double.parseDouble(
                                                ((org.openpnp.model.eagle.xml.Smd) e).getY()) + y);

                                Point part_center = new Point(x, y);

                                if (element_side == Side.Top) {
                                    if (rotation > 180) {
                                        a = Utils2D.rotateTranslateCenterPoint(a, rotation, 0, 0,
                                                part_center); // rotate the part-pin
                                    }
                                    else {
                                        a = Utils2D.rotateTranslateCenterPoint(a, -rotation, 0, 0,
                                                part_center); // rotate the part-pin
                                    }
                                }
                                else if (element_side == Side.Bottom) {
                                    if (rotation > 180) {
                                        a = Utils2D.rotateTranslateCenterPoint(a, rotation, 0, 0,
                                                part_center); // rotate the part-pin
                                    }
                                    else {
                                        a = Utils2D.rotateTranslateCenterPoint(a, -(180 - rotation),
                                                0, 0, part_center); // rotate the part-pin
                                    }

                                    // Mirror along the Y axis of the board
                                    if (a.getX() < center.getX()) {
                                        Double offset = center.getX() - a.getX();
                                        a.setX(center.getX() + offset); // mirror left to right
                                                                        // across the centre of the
                                                                        // board
                                    }
                                    else {
                                        Double offset = a.getX() - center.getX();
                                        a.setX(center.getX() - offset);
                                    }
                                    // Mirror along the X axis of the part's center line
                                    if (a.getY() < y) {
                                        Double offset = y - a.getY();
                                        a.setY(y + offset); // mirror top to bottom across the
                                                            // centre of the part
                                    }
                                    else {
                                        Double offset = a.getY() - y;
                                        a.setY(y - offset); // mirror bottom to top across the
                                                            // centre of the part
                                    }

                                }

                                // TODO Need to write the logic for pad rotation
                                // A =
                                // Utils2D.rotateTranslateCenterPoint(A,pad_rotation,0,0,center);


                                BoardPad boardPad =
                                        new BoardPad(pad, new Location(LengthUnit.Millimeters,
                                                a.getX(), a.getY(), 0, pad_rotation));

                                // TODO add support for Circle pads

                                boardPad.setName(element.getName() + "-" //$NON-NLS-1$
                                        + ((org.openpnp.model.eagle.xml.Smd) e).getName());

                                if (((org.openpnp.model.eagle.xml.Smd) e).getLayer()
                                                                         .equalsIgnoreCase(
                                                                                 topLayer)) { // is
                                                                                              // the
                                                                                              // pad
                                                                                              // on
                                                                                              // top
                                    if (element_side == Side.Top) {// part is on the top
                                        boardPad.setSide(Side.Top);
                                    } // pad is on the top
                                    else {
                                        boardPad.setSide(Side.Bottom); // part is on top, but pad is
                                                                       // on the bottom
                                    }
                                }
                                else if (((org.openpnp.model.eagle.xml.Smd) e).getLayer()
                                                                              .equalsIgnoreCase(
                                                                                      bottomLayer)) { // is
                                                                                                      // the
                                                                                                      // pad
                                                                                                      // on
                                                                                                      // the
                                                                                                      // bottom
                                    if (element_side == Side.Top) { // part is top
                                        boardPad.setSide(Side.Bottom); // pad stays on the bottom
                                    }
                                    else {
                                        boardPad.setSide(Side.Top); // pad moves to the top
                                    }
                                }
                                else {
                                    Logger.info("Warning: " + file //$NON-NLS-1$
                                            + "contains a SMD pad that is not on a topLayer or bottomLayer"); //$NON-NLS-1$
                                }

                                // TODO figure out if it is possible for an SMD pad to have a drill,
                                // it appears not !!
                                // pad.setdrillDiameter(0);

                                // TODO later we need to associate a list of pads to a board.
                                pads.add(boardPad);

                                board.addSolderPastePad(boardPad); // This adds the pad to the
                                                                   // SolderPaste
                            }
                        }
                        else if (e instanceof org.openpnp.model.eagle.xml.Pad) {

                            // TODO implement pasting for through hole pads

                        }
                        else if (e instanceof org.openpnp.model.eagle.xml.Polygon) {
                            // We have a polygon is it on a tCream or bCream layer, otherwise ignore
                            // it
                            if (((org.openpnp.model.eagle.xml.Polygon) e).getLayer()
                                                                         .equalsIgnoreCase(
                                                                                 tCreamLayer)
                                    || ((org.openpnp.model.eagle.xml.Polygon) e).getLayer()
                                                                                .equalsIgnoreCase(
                                                                                        bCreamLayer)) {
                                Logger.info("Warning: " + file //$NON-NLS-1$
                                        + " contains a Polygon pad - this functionality has been implmented as the smallest bounded rectangle and may over paste the area"); //$NON-NLS-1$
                                Logger.info("Layer" //$NON-NLS-1$
                                        + ((org.openpnp.model.eagle.xml.Polygon) e).getLayer()
                                                                                   .toString());
                                Double vertex_x_min = 0.0;
                                Double vertex_x_max = 0.0;
                                Double vertex_y_min = 0.0;
                                Double vertex_y_max = 0.0;
                                ListIterator<org.openpnp.model.eagle.xml.Vertex> vertex_it =
                                        ((org.openpnp.model.eagle.xml.Polygon) e).getVertex()
                                                                                 .listIterator();
                                while (vertex_it.hasNext()) {
                                    org.openpnp.model.eagle.xml.Vertex vertex =
                                            (Vertex) vertex_it.next();
                                    vertex_x_min = Math.min(vertex_x_min,
                                            Double.parseDouble(vertex.getX()));
                                    vertex_x_max = Math.max(vertex_x_max,
                                            Double.parseDouble(vertex.getX()));
                                    vertex_y_min = Math.min(vertex_y_min,
                                            Double.parseDouble(vertex.getY()));
                                    vertex_y_max = Math.max(vertex_y_max,
                                            Double.parseDouble(vertex.getY()));
                                    Logger.info(
                                            "Vertex: X=" + vertex.getX() + " y=" + vertex.getY()); //$NON-NLS-1$ //$NON-NLS-2$
                                }
                                // TODO implement polygon pad in Pad.java
                                Pad.RoundRectangle pad = new Pad.RoundRectangle();
                                pad.setUnits(LengthUnit.Millimeters);
                                pad.setRoundness(0);
                                pad.setHeight((vertex_y_max - vertex_y_min));
                                pad.setWidth((vertex_x_max - vertex_x_min));

                                BoardPad boardPad = new BoardPad(pad,
                                        new Location(LengthUnit.Millimeters,
                                                x + (vertex_x_max + vertex_x_min) / 2,
                                                y + (vertex_y_max + vertex_y_min) / 2, 0, 0));
                                Logger.info("Pad generated width is " + pad.getWidth() + " height " //$NON-NLS-1$ //$NON-NLS-2$
                                        + pad.getHeight() + " centered at x = " //$NON-NLS-1$
                                        + boardPad.getLocation()
                                                  .getX()
                                        + " y = " + boardPad.getLocation() //$NON-NLS-1$
                                                            .getY());
                                boardPad.setName(element.getName() + "-" + "Polygon "); // Polygons //$NON-NLS-1$ //$NON-NLS-2$
                                                                                        // are not
                                                                                        // named so
                                                                                        // just name
                                                                                        // it as
                                                                                        // "Polygon"

                                if (((org.openpnp.model.eagle.xml.Polygon) e).getLayer()
                                                                             .equalsIgnoreCase(
                                                                                     tCreamLayer)) {
                                    boardPad.setSide(Side.Top);
                                }
                                else {
                                    boardPad.setSide(Side.Bottom);
                                }

                                pads.add(boardPad);

                                board.addSolderPastePad(boardPad); // This adds the pad to the
                                                                   // SolderPaste
                            }
                        }
                    }

                    placement.setSide(element_side);
                    placements.add(placement);
                    board.addPlacement(placement); // this adds the placement to the Pick and Place
                                                   // list

                }
            }
        }
        if (boardToProcess.library != null) {

        }
        if (boardToProcess.schematic != null) {

        }

        return placements;
    }

    class Dlg extends JDialog {
        private JTextField textFieldBoardFile;
        private final Action browseBoardFileAction = new SwingAction();
        private final Action importAction = new SwingAction_2();
        private final Action cancelAction = new SwingAction_3();
        private JCheckBox chckbxCreateMissingParts;
        private JCheckBox chckbxUpdateExistingParts;
        private JCheckBox chckbxAddLibraryPrefix;
        private JCheckBox chckbxImportTop;
        private JCheckBox chckbxImportBottom;

        public Dlg(Frame parent) {
            super(parent, DESCRIPTION, true);
            getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

            JPanel panel = new JPanel();
            panel.setBorder(new TitledBorder(null, Translations.getString("EagleBoardImporter.FilesPanel.Border.title"), TitledBorder.LEADING, TitledBorder.TOP, //$NON-NLS-1$
                    null, null));
            getContentPane().add(panel);
            panel.setLayout(new FormLayout(
                    new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"), //$NON-NLS-1$
                            FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                    new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

            JLabel lblBoardFilebrd = new JLabel(Translations.getString("EagleBoardImporter.FilesPanel.BoardFileLabel.text")); //$NON-NLS-1$
            panel.add(lblBoardFilebrd, "2, 2, right, default"); //$NON-NLS-1$

            textFieldBoardFile = new JTextField();
            panel.add(textFieldBoardFile, "4, 2, fill, default"); //$NON-NLS-1$
            textFieldBoardFile.setColumns(10);

            JButton btnBrowse = new JButton(Translations.getString("EagleBoardImporter.FilesPanel.browseButton.text")); //$NON-NLS-1$
            btnBrowse.setAction(browseBoardFileAction);
            panel.add(btnBrowse, "6, 2"); //$NON-NLS-1$

            JPanel panel_1 = new JPanel();
            panel_1.setBorder(new TitledBorder(null, Translations.getString("EagleBoardImporter.OptionsPanel.Border.title"), TitledBorder.LEADING, //$NON-NLS-1$
                    TitledBorder.TOP, null, null));
            getContentPane().add(panel_1);
            panel_1.setLayout(new FormLayout(
                    new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                    new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

            chckbxCreateMissingParts = new JCheckBox(Translations.getString("EagleBoardImporter.OptionsPanel.createMissingPartsChkbox.text")); //$NON-NLS-1$
            chckbxCreateMissingParts.setSelected(true);
            panel_1.add(chckbxCreateMissingParts, "2, 2"); //$NON-NLS-1$

            chckbxUpdateExistingParts = new JCheckBox(Translations.getString("EagleBoardImporter.OptionsPanel.updateExistingPartsChkbox.text")); //$NON-NLS-1$
            chckbxUpdateExistingParts.setSelected(false);
            panel_1.add(chckbxUpdateExistingParts, "2, 4"); //$NON-NLS-1$

            chckbxAddLibraryPrefix = new JCheckBox(Translations.getString("EagleBoardImporter.OptionsPanel.addLibraryPrefixChkbox.text")); //$NON-NLS-1$
            chckbxAddLibraryPrefix.setSelected(false);
            panel_1.add(chckbxAddLibraryPrefix, "2, 6"); //$NON-NLS-1$

            chckbxImportTop = new JCheckBox(Translations.getString("EagleBoardImporter.OptionsPanel.importTopChkbox.text")); //$NON-NLS-1$
            chckbxImportTop.setSelected(true);
            panel_1.add(chckbxImportTop, "2, 8"); //$NON-NLS-1$

            chckbxImportBottom = new JCheckBox(Translations.getString("EagleBoardImporter.OptionsPanel.ImportBottomChkbox.text")); //$NON-NLS-1$
            chckbxImportBottom.setSelected(true);
            panel_1.add(chckbxImportBottom, "2, 10"); //$NON-NLS-1$

            JSeparator separator = new JSeparator();
            getContentPane().add(separator);

            JPanel panel_2 = new JPanel();
            FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
            flowLayout.setAlignment(FlowLayout.RIGHT);
            getContentPane().add(panel_2);

            JButton btnCancel = new JButton(Translations.getString("EagleBoardImporter.ButtonsPanel.CancelButton.text")); //$NON-NLS-1$
            btnCancel.setAction(cancelAction);
            panel_2.add(btnCancel);

            JButton btnImport = new JButton(Translations.getString("EagleBoardImporter.ButtonsPanel.ImportButton.text")); //$NON-NLS-1$
            btnImport.setAction(importAction);
            panel_2.add(btnImport);

            setSize(400, 400);
            setLocationRelativeTo(parent);

            JRootPane rootPane = getRootPane();
            KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE"); //$NON-NLS-1$
            InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            inputMap.put(stroke, "ESCAPE"); //$NON-NLS-1$
            rootPane.getActionMap()
                    .put("ESCAPE", cancelAction); //$NON-NLS-1$
        }

        private class SwingAction extends AbstractAction {
            public SwingAction() {
                putValue(NAME, Translations.getString("EagleBoardImporter.BrowseAction.Name")); //$NON-NLS-1$
                putValue(SHORT_DESCRIPTION, Translations.getString("EagleBoardImporter.BrowseAction.ShortDescription")); //$NON-NLS-1$
            }

            public void actionPerformed(ActionEvent e) {
                FileDialog fileDialog = new FileDialog(Dlg.this);
                fileDialog.setFilenameFilter(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase()
                                   .endsWith(".brd"); //$NON-NLS-1$
                    }
                });
                fileDialog.setVisible(true);
                if (fileDialog.getFile() == null) {
                    return;
                }
                File file = new File(new File(fileDialog.getDirectory()), fileDialog.getFile());
                textFieldBoardFile.setText(file.getAbsolutePath());
            }
        }

        private class SwingAction_2 extends AbstractAction {
            public SwingAction_2() {
                putValue(NAME, Translations.getString("EagleBoardImporter.ImportAction.Name")); //$NON-NLS-1$
                putValue(SHORT_DESCRIPTION, Translations.getString("EagleBoardImporter.ImportAction.ShortDescription")); //$NON-NLS-1$
            }

            public void actionPerformed(ActionEvent e) {
                boardFile = new File(textFieldBoardFile.getText());
                board = new Board();
                List<Placement> placements = new ArrayList<>();
                try {
                    if (boardFile.exists()) {
                        if (chckbxImportTop.isSelected() && chckbxImportBottom.isSelected()) {
                            placements.addAll(parseFile(boardFile, null,
                                    chckbxCreateMissingParts.isSelected(),
                                    chckbxUpdateExistingParts.isSelected(),
                                    chckbxAddLibraryPrefix.isSelected())); // both Top and Bottom
                                                                           // of the board
                        }
                        else if (chckbxImportTop.isSelected()) {
                            placements.addAll(parseFile(boardFile, Side.Top,
                                    chckbxCreateMissingParts.isSelected(),
                                    chckbxUpdateExistingParts.isSelected(),
                                    chckbxAddLibraryPrefix.isSelected())); // Just the Top side of
                                                                           // the board
                        }
                        else if (chckbxImportBottom.isSelected()) {
                            placements.addAll(parseFile(boardFile, Side.Bottom,
                                    chckbxCreateMissingParts.isSelected(),
                                    chckbxUpdateExistingParts.isSelected(),
                                    chckbxAddLibraryPrefix.isSelected())); // Just the Bottom side
                                                                           // of the board
                        }
                    }
                }
                catch (Exception e1) {
                    MessageBoxes.errorBox(Dlg.this, Translations.getString("EagleBoardImporter.ImportErrorMessage"), e1); //$NON-NLS-1$
                    return;
                }

                setVisible(false);
            }
        }

        private class SwingAction_3 extends AbstractAction {
            public SwingAction_3() {
                putValue(NAME, Translations.getString("EagleBoardImporter.CancelAction.Name")); //$NON-NLS-1$
                putValue(SHORT_DESCRIPTION, Translations.getString("EagleBoardImporter.CancelAction.ShortDescription")); //$NON-NLS-1$
            }

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }
    }
}
