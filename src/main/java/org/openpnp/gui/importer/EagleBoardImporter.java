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

import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

@SuppressWarnings("serial")
public class EagleBoardImporter implements BoardImporter {
    private final static Logger logger = LoggerFactory.getLogger(EagleBoardImporter.class);

    private final static String NAME = "CadSoft EAGLE Board";
    private final static String DESCRIPTION =
            "Import files directly from EAGLE's <filename>.brd file.";

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

    private static List<Placement> parseFile(File file, Side side, boolean createMissingParts)
            throws Exception {

        String dimensionLayer = "";
        String topLayer = "";
        String bottomLayer = "";
        String tCreamLayer = "";
        String bCreamLayer = "";

        String mmMinCreamFrame_string;
        double mmMinCreamFrame_number = 0;
        String mmMaxCreamFrame_string;
        double mmMaxCreamFrame_number = 0;
        String libraryId = "";
        String packageId = "";
        Part part = null;

        List<BoardPad> pads = new ArrayList<>();

        ArrayList<Placement> placements = new ArrayList<>();
        // we don't use the 'side' parameter as we can read this from the .brd file
        // in the future we could use the side parameter to restrict this from only parsing one side
        // or the other or both

        EagleLoader boardToProcess = new EagleLoader(file);
        if (boardToProcess.board != null) {

            // first establish which is the Dimension, Top, Bottom, tCream and bCream layers in case
            // the board has non-standard layer numbering
            for (Layer layer : boardToProcess.layers.getLayer()) {
                if (layer.getName().equalsIgnoreCase("Dimension")) {
                    dimensionLayer = layer.getNumber();
                }
                else if (layer.getName().equalsIgnoreCase("Top")) {
                    topLayer = layer.getNumber();
                }
                else if (layer.getName().equalsIgnoreCase("Bottom")) {
                    bottomLayer = layer.getNumber();
                }
                else if (layer.getName().equalsIgnoreCase("tCream")) {
                    tCreamLayer = layer.getNumber();
                }
                else if (layer.getName().equalsIgnoreCase("bCream")) {
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
            for (Param params : boardToProcess.board.getDesignrules().getParam()) {

                if (params.getName().compareToIgnoreCase("mlMinCreamFrame") == 0) { // found exact
                                                                                    // match when 0
                                                                                    // returned
                    mmMinCreamFrame_string = params.getValue().replaceAll("[A-Za-z ]", ""); // remove
                                                                                            // all
                                                                                            // letters,
                                                                                            // i.e.
                                                                                            // 0mil
                                                                                            // becomes
                                                                                            // 0
                    if (params.getValue().toUpperCase().endsWith("MIL")) {
                        mmMinCreamFrame_number =
                                Double.parseDouble(mmMinCreamFrame_string) * mil_to_mm;
                    }
                    else if (params.getValue().toUpperCase().endsWith("MM")) {
                        mmMinCreamFrame_number =
                                Double.parseDouble(mmMinCreamFrame_string) * mil_to_mm;
                    }
                    else
                        throw new Exception("mlMinCream must either be in mil or mm"); // Force the
                                                                                       // importer
                                                                                       // to abort,
                                                                                       // something
                                                                                       // is very
                                                                                       // wrong
                }
                if (params.getName().compareToIgnoreCase("mlMaxCreamFrame") == 0) { // found exact
                                                                                    // match when 0
                                                                                    // returned
                    mmMaxCreamFrame_string = params.getValue().replaceAll("[A-Za-z ]", ""); // remove
                                                                                            // all
                                                                                            // letters,
                                                                                            // i.e.
                                                                                            // "0mil"
                                                                                            // becomes
                                                                                            // 0
                    if (params.getValue().toUpperCase().endsWith("MIL")) {
                        mmMaxCreamFrame_number =
                                Double.parseDouble(mmMaxCreamFrame_string) * mil_to_mm;
                    }
                    else if (params.getValue().toUpperCase().endsWith("MM")) {
                        mmMaxCreamFrame_number = Double.parseDouble(mmMaxCreamFrame_string);
                    }
                    else
                        throw new Exception("mlMaxCream must either be in mil or mm"); // Force the
                                                                                       // importer
                                                                                       // to abort,
                                                                                       // something
                                                                                       // is very
                                                                                       // wrong
                }
            }
            // Now we know the min and max tolerance for the cream (aka solder paste)
            // which are mmMinCreamFrame_number and mmMaxCreamFrame_number and are in mm (converted
            // from mil as required)

            // Now we got through each of the parts
            if (!boardToProcess.board.getElements().getElement().isEmpty()) {

                // Process each of the element items
                for (Element element : boardToProcess.board.getElements().getElement()) {
                    // first we determine if the part is on the top layer or bottom layer

                    Side element_side;
                    String rot = element.getRot();
                    if (rot.toUpperCase().startsWith("M"))
                        // The part is mirrored and therefore is on the bottom of the board
                        element_side = Side.Bottom;
                    else
                        element_side = Side.Top;

                    // Now determine if we want to process this part based on which side of the
                    // board it is on

                    if (side != null) { // null means process both sides
                        if (side != element_side)
                            continue; // exit this loop and process the next element
                    }

                    String rot_number = rot.replaceAll("[A-Za-z ]", ""); // remove all letters, i.e.
                                                                         // R180 becomes 180

                    Placement placement = new Placement(element.getName());
                    double rotation = Double.parseDouble(rot_number);
                    double x = Double.parseDouble(element.getX());
                    double y = Double.parseDouble(element.getY());
                    placement.setLocation(new Location(LengthUnit.Millimeters, x, y, 0, rotation));

                    // placement now contains where the package is on the PCB, we need to work out
                    // where the pads
                    // are relative to the 'placement'
                    Configuration cfg = Configuration.get();
                    if (cfg != null && createMissingParts) {
                        String value = element.getValue(); // Value
                        packageId = element.getPackage(); // Package
                        libraryId = element.getLibrary(); // Library that contains the package

                        String pkgId = libraryId + "-" + packageId;

                        String partId = libraryId + "-" + packageId;
                        if (value.trim().length() > 0) {
                            partId += "-" + value;
                        }

                        part = cfg.getPart(partId);
                        Package pkg = cfg.getPackage(pkgId);

                        if ((part == null) || (pkg == null)) {

                            if (pkg == null) {
                                pkg = new Package(pkgId);
                                cfg.addPackage(pkg); // save the package in the configuration file
                                if (part != null) {
                                    cfg.removePart(part);// we have to remove the part so we can
                                                         // re-add it with the correct package &
                                                         // library
                                    part = null;
                                }

                            }
                            if (part == null) {
                                part = new Part(partId);
                                part.setPackage(pkg);
                                // TODO part.setLibrary(libraryId);
                                cfg.addPart(part); // save the package in the configuration file
                            }
                            cfg.addPart(part);
                        }
                    }
                    placement.setPart(part);

                    // Now we have the part, we now need to add the SolderPastePad to the board
                    // Note, Eagle has the concept of minimum and max from the edge of the pad so we
                    // need to
                    // adjust the pad to be the size as the mid-point between the minimum and max
                    // in practice these are usually 0, which means we paste the entire pad

                    if (!boardToProcess.board.getLibraries().getLibrary().isEmpty()) {
                        for (Library library : boardToProcess.board.getLibraries().getLibrary()) {
                            if (library.getName().equalsIgnoreCase(libraryId)) {
                                // we have found the library, now to scan for the package we want
                                if (!library.getPackages().getPackage().isEmpty()) {

                                    ListIterator<org.openpnp.model.eagle.xml.Package> it =
                                            library.getPackages().getPackage().listIterator();

                                    while (it.hasNext()) {

                                        org.openpnp.model.eagle.xml.Package pak =
                                                (org.openpnp.model.eagle.xml.Package) it.next();
                                        if (pak.getName().equalsIgnoreCase(packageId)) {

                                            for (Object e : pak
                                                    .getPolygonOrWireOrTextOrDimensionOrCircleOrRectangleOrFrameOrHoleOrPadOrSmd()) {
                                                if (e instanceof org.openpnp.model.eagle.xml.Smd) {
                                                    // we have found the correct package in the
                                                    // correct library and we need to to add the pad
                                                    // to the boardPads

                                                    if (!((org.openpnp.model.eagle.xml.Smd) e)
                                                            .getCream().equalsIgnoreCase("No")) { // if
                                                                                                  // cream="no"
                                                                                                  // then
                                                                                                  // we
                                                                                                  // do
                                                                                                  // not
                                                                                                  // paste
                                                                                                  // this
                                                                                                  // pad

                                                        Pad.RoundRectangle pad =
                                                                new Pad.RoundRectangle();
                                                        pad.setUnits(LengthUnit.Millimeters);

                                                        // TODO check that these reduce the pad to
                                                        // the halfway between the minimum & maximum
                                                        // tolerances
                                                        pad.setHeight(Double.parseDouble(
                                                                ((org.openpnp.model.eagle.xml.Smd) e)
                                                                        .getDx())
                                                                - (mmMaxCreamFrame_number
                                                                        - mmMinCreamFrame_number)
                                                                        / 2);
                                                        pad.setWidth(Double.parseDouble(
                                                                ((org.openpnp.model.eagle.xml.Smd) e)
                                                                        .getDy())
                                                                - (mmMaxCreamFrame_number
                                                                        - mmMinCreamFrame_number)
                                                                        / 2);

                                                        pad.setRoundness(0);
                                                        pad.setRoundness(Double.parseDouble(
                                                                ((org.openpnp.model.eagle.xml.Smd) e)
                                                                        .getRoundness()));

                                                        // first find out how is the package defined
                                                        Double pad_rotation =
                                                                Double.parseDouble(rot_number);
                                                        // now rotate the pad by its own rotation
                                                        // relative to its origin and make sure we
                                                        // don't turn through 360 degrees
                                                        pad_rotation += Double.parseDouble(
                                                                ((org.openpnp.model.eagle.xml.Smd) e)
                                                                        .getRot().replaceAll(
                                                                                "[A-Za-z ]", ""))
                                                                % 360;

                                                        Point A = new Point(
                                                                Double.parseDouble(
                                                                        ((org.openpnp.model.eagle.xml.Smd) e)
                                                                                .getX())
                                                                        + x,
                                                                Double.parseDouble(
                                                                        ((org.openpnp.model.eagle.xml.Smd) e)
                                                                                .getY())
                                                                        + y);

                                                        Point part_center = new Point(x, y);

                                                        if (element_side == Side.Top) {
                                                            if (rotation > 180)
                                                                A = Utils2D
                                                                        .rotateTranslateCenterPoint(
                                                                                A, rotation, 0, 0,
                                                                                part_center); // rotate
                                                                                              // the
                                                                                              // part-pin
                                                            else
                                                                A = Utils2D
                                                                        .rotateTranslateCenterPoint(
                                                                                A, -rotation, 0, 0,
                                                                                part_center); // rotate
                                                                                              // the
                                                                                              // part-pin
                                                        }
                                                        else if (element_side == Side.Bottom) {
                                                            if (rotation > 180)
                                                                A = Utils2D
                                                                        .rotateTranslateCenterPoint(
                                                                                A, rotation, 0, 0,
                                                                                part_center); // rotate
                                                                                              // the
                                                                                              // part-pin
                                                            else
                                                                A = Utils2D
                                                                        .rotateTranslateCenterPoint(
                                                                                A,
                                                                                -(180 - rotation),
                                                                                0, 0, part_center); // rotate
                                                                                                    // the
                                                                                                    // part-pin

                                                            // Mirror along the Y axis of the board
                                                            if (A.getX() < center.getX()) {
                                                                Double offset =
                                                                        center.getX() - A.getX();
                                                                A.setX(center.getX() + offset); // mirror
                                                                                                // left
                                                                                                // to
                                                                                                // right
                                                                                                // across
                                                                                                // the
                                                                                                // centre
                                                                                                // of
                                                                                                // the
                                                                                                // board
                                                            }
                                                            else {
                                                                Double offset =
                                                                        A.getX() - center.getX();
                                                                A.setX(center.getX() - offset);
                                                            }
                                                            // Mirror along the X axis of the part's
                                                            // center line
                                                            if (A.getY() < y) {
                                                                Double offset = y - A.getY();
                                                                A.setY(y + offset); // mirror top to
                                                                                    // bottom across
                                                                                    // the centre of
                                                                                    // the part
                                                            }
                                                            else {
                                                                Double offset = A.getY() - y;
                                                                A.setY(y - offset); // mirror bottom
                                                                                    // to top across
                                                                                    // the centre of
                                                                                    // the part
                                                            }

                                                        }

                                                        // TODO Need to write the logic for pad
                                                        // rotation
                                                        // A = Utils2D.rotateTranslateCenterPoint(A,
                                                        // pad_rotation,0,0,center);
                                                        //


                                                        BoardPad boardPad = new BoardPad(pad,
                                                                new Location(LengthUnit.Millimeters,
                                                                        A.getX(), A.getY(), 0,
                                                                        pad_rotation));

                                                        // TODO add support for Circle pads

                                                        boardPad.setName(element.getName() + "-"
                                                                + ((org.openpnp.model.eagle.xml.Smd) e)
                                                                        .getName());

                                                        if (((org.openpnp.model.eagle.xml.Smd) e)
                                                                .getLayer()
                                                                .equalsIgnoreCase(topLayer)) { // is
                                                                                               // the
                                                                                               // pad
                                                                                               // on
                                                                                               // top
                                                            if (element_side == Side.Top) // part is
                                                                                          // on the
                                                                                          // top
                                                                boardPad.setSide(Side.Top); // pad
                                                                                            // is on
                                                                                            // the
                                                                                            // top
                                                            else
                                                                boardPad.setSide(Side.Bottom); // part
                                                                                               // is
                                                                                               // on
                                                                                               // top,
                                                                                               // but
                                                                                               // pat
                                                                                               // is
                                                                                               // on
                                                                                               // the
                                                                                               // bottom
                                                        }
                                                        else if (((org.openpnp.model.eagle.xml.Smd) e)
                                                                .getLayer()
                                                                .equalsIgnoreCase(bottomLayer)) { // is
                                                                                                  // the
                                                                                                  // pad
                                                                                                  // on
                                                                                                  // the
                                                                                                  // bottom
                                                            if (element_side == Side.Top) // part is
                                                                                          // top
                                                                boardPad.setSide(Side.Bottom); // pad
                                                                                               // stays
                                                                                               // on
                                                                                               // the
                                                                                               // bottom
                                                            else
                                                                boardPad.setSide(Side.Top); // pad
                                                                                            // moves
                                                                                            // to
                                                                                            // the
                                                                                            // top
                                                        }
                                                        else
                                                            logger.info("Warning: " + file
                                                                    + "contains a SMD pad that is not on a topLayer or bottomLayer");

                                                        // TODO figure out if it is possible for an
                                                        // SMD pad to have a drill, it appears not
                                                        // !!
                                                        // pad.setdrillDiameter(0);

                                                        // TODO later we need to associate a list of
                                                        // pads to a board.
                                                        pads.add(boardPad);

                                                        board.addSolderPastePad(boardPad); // This
                                                                                           // adds
                                                                                           // the
                                                                                           // pad to
                                                                                           // the
                                                                                           // SolderPaste
                                                    }
                                                }
                                                else if (e instanceof org.openpnp.model.eagle.xml.Pad) {

                                                    // TODO implement pasting for through hole pads

                                                }
                                                else if (e instanceof org.openpnp.model.eagle.xml.Polygon) {
                                                    // We have a polygon is it on a tCream or bCream
                                                    // layer, otherwise ignore it
                                                    if (((org.openpnp.model.eagle.xml.Polygon) e)
                                                            .getLayer()
                                                            .equalsIgnoreCase(tCreamLayer)
                                                            || ((org.openpnp.model.eagle.xml.Polygon) e)
                                                                    .getLayer().equalsIgnoreCase(
                                                                            bCreamLayer)) {
                                                        logger.info("Warning: " + file
                                                                + " contains a Polygon pad - this functionality has been implmented as the smallest bounded rectangle and may over paste the area");
                                                        logger.info(
                                                                "Layer" + ((org.openpnp.model.eagle.xml.Polygon) e)
                                                                        .getLayer().toString());
                                                        Double vertex_x_min = 0.0;
                                                        Double vertex_x_max = 0.0;
                                                        Double vertex_y_min = 0.0;
                                                        Double vertex_y_max = 0.0;
                                                        ListIterator<org.openpnp.model.eagle.xml.Vertex> vertex_it =
                                                                ((org.openpnp.model.eagle.xml.Polygon) e)
                                                                        .getVertex().listIterator();
                                                        while (vertex_it.hasNext()) {
                                                            org.openpnp.model.eagle.xml.Vertex vertex =
                                                                    (Vertex) vertex_it.next();
                                                            vertex_x_min = Math.min(vertex_x_min,
                                                                    Double.parseDouble(
                                                                            vertex.getX()));
                                                            vertex_x_max = Math.max(vertex_x_max,
                                                                    Double.parseDouble(
                                                                            vertex.getX()));
                                                            vertex_y_min = Math.min(vertex_y_min,
                                                                    Double.parseDouble(
                                                                            vertex.getY()));
                                                            vertex_y_max = Math.max(vertex_y_max,
                                                                    Double.parseDouble(
                                                                            vertex.getY()));
                                                            logger.info("Vertex: X=" + vertex.getX()
                                                                    + " y=" + vertex.getY());
                                                        }
                                                        // TODO implement polygon pad in Pad.java
                                                        Pad.RoundRectangle pad =
                                                                new Pad.RoundRectangle();
                                                        pad.setUnits(LengthUnit.Millimeters);
                                                        pad.setRoundness(0);
                                                        pad.setHeight(
                                                                (vertex_y_max - vertex_y_min));
                                                        pad.setWidth((vertex_x_max - vertex_x_min));

                                                        BoardPad boardPad = new BoardPad(pad,
                                                                new Location(LengthUnit.Millimeters,
                                                                        x + (vertex_x_max
                                                                                + vertex_x_min) / 2,
                                                                        y + (vertex_y_max
                                                                                + vertex_y_min) / 2,
                                                                        0, 0));
                                                        logger.info("Pad generated width is "
                                                                + pad.getWidth() + " height "
                                                                + pad.getHeight()
                                                                + " centered at x = "
                                                                + boardPad.getLocation().getX()
                                                                + " y = "
                                                                + boardPad.getLocation().getY());
                                                        boardPad.setName(element.getName() + "-"
                                                                + "Polygon "); // Polygons are not
                                                                               // named so just name
                                                                               // it as "Polygon"

                                                        if (((org.openpnp.model.eagle.xml.Polygon) e)
                                                                .getLayer()
                                                                .equalsIgnoreCase(tCreamLayer))
                                                            boardPad.setSide(Side.Top);
                                                        else
                                                            boardPad.setSide(Side.Bottom);

                                                        pads.add(boardPad);

                                                        board.addSolderPastePad(boardPad); // This
                                                                                           // adds
                                                                                           // the
                                                                                           // pad to
                                                                                           // the
                                                                                           // SolderPaste
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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
        private JCheckBox chckbxImportTop;
        private JCheckBox chckbxImportBottom;

        public Dlg(Frame parent) {
            super(parent, DESCRIPTION, true);
            getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

            JPanel panel = new JPanel();
            panel.setBorder(new TitledBorder(null, "Files", TitledBorder.LEADING, TitledBorder.TOP,
                    null, null));
            getContentPane().add(panel);
            panel.setLayout(new FormLayout(
                    new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,
                            FormSpecs.RELATED_GAP_COLSPEC, ColumnSpec.decode("default:grow"),
                            FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                    new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

            JLabel lblBoardFilebrd = new JLabel("Eagle PCB Board File (.brd)");
            panel.add(lblBoardFilebrd, "2, 2, right, default");

            textFieldBoardFile = new JTextField();
            panel.add(textFieldBoardFile, "4, 2, fill, default");
            textFieldBoardFile.setColumns(10);

            JButton btnBrowse = new JButton("Browse");
            btnBrowse.setAction(browseBoardFileAction);
            panel.add(btnBrowse, "6, 2");

            JPanel panel_1 = new JPanel();
            panel_1.setBorder(new TitledBorder(null, "Options", TitledBorder.LEADING,
                    TitledBorder.TOP, null, null));
            getContentPane().add(panel_1);
            panel_1.setLayout(new FormLayout(
                    new ColumnSpec[] {FormSpecs.RELATED_GAP_COLSPEC, FormSpecs.DEFAULT_COLSPEC,},
                    new RowSpec[] {FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,
                            FormSpecs.RELATED_GAP_ROWSPEC, FormSpecs.DEFAULT_ROWSPEC,}));

            chckbxCreateMissingParts = new JCheckBox("Create Missing Parts");
            chckbxCreateMissingParts.setSelected(true);
            panel_1.add(chckbxCreateMissingParts, "2, 2");

            chckbxImportTop = new JCheckBox("Import Parts on the Top of the board");
            chckbxImportTop.setSelected(true);
            panel_1.add(chckbxImportTop, "2, 4");

            chckbxImportBottom = new JCheckBox("Import Parts on the Bottom of the board");
            chckbxImportBottom.setSelected(true);
            panel_1.add(chckbxImportBottom, "2, 6");

            JSeparator separator = new JSeparator();
            getContentPane().add(separator);

            JPanel panel_2 = new JPanel();
            FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
            flowLayout.setAlignment(FlowLayout.RIGHT);
            getContentPane().add(panel_2);

            JButton btnCancel = new JButton("Cancel");
            btnCancel.setAction(cancelAction);
            panel_2.add(btnCancel);

            JButton btnImport = new JButton("Import");
            btnImport.setAction(importAction);
            panel_2.add(btnImport);

            setSize(400, 400);
            setLocationRelativeTo(parent);

            JRootPane rootPane = getRootPane();
            KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
            InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            inputMap.put(stroke, "ESCAPE");
            rootPane.getActionMap().put("ESCAPE", cancelAction);
        }

        private class SwingAction extends AbstractAction {
            public SwingAction() {
                putValue(NAME, "Browse");
                putValue(SHORT_DESCRIPTION, "Browse");
            }

            public void actionPerformed(ActionEvent e) {
                FileDialog fileDialog = new FileDialog(Dlg.this);
                fileDialog.setFilenameFilter(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.toLowerCase().endsWith(".brd");
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
                putValue(NAME, "Import");
                putValue(SHORT_DESCRIPTION, "Import");
            }

            public void actionPerformed(ActionEvent e) {
                boardFile = new File(textFieldBoardFile.getText());
                board = new Board();
                List<Placement> placements = new ArrayList<>();
                try {
                    if (boardFile.exists()) {
                        if (chckbxImportTop.isSelected() && chckbxImportBottom.isSelected())
                            placements.addAll(parseFile(boardFile, null,
                                    chckbxCreateMissingParts.isSelected())); // both Top and Bottom
                                                                             // of the board
                        else if (chckbxImportTop.isSelected())
                            placements.addAll(parseFile(boardFile, Side.Top,
                                    chckbxCreateMissingParts.isSelected())); // Just the Top side of
                                                                             // the board
                        else if (chckbxImportBottom.isSelected())
                            placements.addAll(parseFile(boardFile, Side.Bottom,
                                    chckbxCreateMissingParts.isSelected())); // Just the Bottom side
                                                                             // of the board
                    }
                }
                catch (Exception e1) {
                    MessageBoxes.errorBox(Dlg.this, "Import Error", e1);
                    return;
                }

                setVisible(false);
            }
        }

        private class SwingAction_3 extends AbstractAction {
            public SwingAction_3() {
                putValue(NAME, "Cancel");
                putValue(SHORT_DESCRIPTION, "Cancel");
            }

            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        }
    }
}
