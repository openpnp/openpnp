package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.openpnp.gui.components.ComponentDecorators;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.spi.Camera;
import org.openpnp.util.UiUtils;

import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.FormSpecs;
import com.jgoodies.forms.layout.RowSpec;

/**
 * A dialog that allows the user to scan a rectangular area using a camera and
 * stitch the images together.
 */
public class BoardScannerDialog extends JDialog {
    private JComboBox<BoardLocation> boardComboBox;
    private JTextField minX;
    private JTextField minY;
    private JTextField width;
    private JTextField height;
    private JTextField zHeight;
    private JComboBox<Camera> cameraComboBox;
    private JProgressBar progressBar;
    private JLabel previewLabel;
    private JButton btnScan;
    private JButton btnSave;
    private BufferedImage resultImage;

    private LengthUnit units = LengthUnit.Millimeters;

    public BoardScannerDialog() {
        super(MainFrame.get(), "Board Scanner", false);
        setSize(800, 600);
        setLocationRelativeTo(MainFrame.get());
        setLayout(new BorderLayout());

        JPanel typesPanel = new JPanel();
        typesPanel.setLayout(new FormLayout(new ColumnSpec[] {
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC,
                FormSpecs.DEFAULT_COLSPEC,
                FormSpecs.RELATED_GAP_COLSPEC,
                ColumnSpec.decode("default:grow"),
                FormSpecs.RELATED_GAP_COLSPEC },
                new RowSpec[] {
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC,
                        FormSpecs.RELATED_GAP_ROWSPEC,
                        FormSpecs.DEFAULT_ROWSPEC }));

        add(typesPanel, BorderLayout.NORTH);

        // Camera
        typesPanel.add(new JLabel("Camera:"), "2, 2, right, default");
        cameraComboBox = new JComboBox<>();
        for (Camera c : Configuration.get().getMachine().getAllCameras()) {
            if (c.getLooking() == Camera.Looking.Down) {
                cameraComboBox.addItem(c);
            }
        }
        typesPanel.add(cameraComboBox, "4, 2, 5, 1");

        // Board Selection
        typesPanel.add(new JLabel("Board:"), "2, 4, right, default");
        boardComboBox = new JComboBox<>();
        boardComboBox.addItem(null); // "Custom / None"
        for (BoardLocation bl : MainFrame.get().getJobTab().getJob().getBoardLocations()) {
            boardComboBox.addItem(bl);
        }
        boardComboBox.addActionListener(e -> updateBoundsFromBoard());
        typesPanel.add(boardComboBox, "4, 4, 5, 1");

        // Bounds
        typesPanel.add(new JLabel("Min X:"), "2, 6, right, default");
        minX = new JTextField("0");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(minX);
        typesPanel.add(minX, "4, 6");

        typesPanel.add(new JLabel("Min Y:"), "6, 6, right, default");
        minY = new JTextField("0");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(minY);
        typesPanel.add(minY, "8, 6");

        typesPanel.add(new JLabel("Width:"), "2, 8, right, default");
        width = new JTextField("100");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(width);
        typesPanel.add(width, "4, 8");

        typesPanel.add(new JLabel("Height:"), "6, 8, right, default");
        height = new JTextField("100");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(height);
        typesPanel.add(height, "8, 8");

        typesPanel.add(new JLabel("Z Height:"), "2, 10, right, default");
        zHeight = new JTextField("0");
        ComponentDecorators.decorateWithAutoSelectAndLengthConversion(zHeight);
        typesPanel.add(zHeight, "4, 10");

        // Action Buttons
        JPanel buttonPanel = new JPanel();
        btnScan = new JButton(scanAction);
        buttonPanel.add(btnScan);

        btnSave = new JButton(saveAction);
        btnSave.setEnabled(false);
        buttonPanel.add(btnSave);

        add(buttonPanel, BorderLayout.SOUTH);

        // Progress Bar
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        typesPanel.add(progressBar, "2, 12, 7, 1");

        // Preview
        previewLabel = new JLabel();
        previewLabel.setHorizontalAlignment(JLabel.CENTER);
        add(new JScrollPane(previewLabel), BorderLayout.CENTER);

        // Initialize fields with current location/defaults
        try {
            if (cameraComboBox.getItemCount() > 0) {
                cameraComboBox.setSelectedIndex(0);
                Camera cam = (Camera) cameraComboBox.getSelectedItem();
                Location loc = cam.getLocation();
                minX.setText(new Length(loc.getX(), units).toString());
                minY.setText(new Length(loc.getY(), units).toString());
                zHeight.setText(new Length(loc.getZ(), units).toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBoundsFromBoard() {
        BoardLocation bl = (BoardLocation) boardComboBox.getSelectedItem();
        if (bl == null) {
            return;
        }

        try {
            Location boardDims = bl.getBoard().getDimensions();
            AffineTransform t = bl.getLocalToGlobalTransform();

            double globalMinX = Double.MAX_VALUE;
            double globalMinY = Double.MAX_VALUE;
            double globalMaxX = -Double.MAX_VALUE;
            double globalMaxY = -Double.MAX_VALUE;

            // Check if board has dimensions defined
            if (boardDims.getX() > 0 && boardDims.getY() > 0) {
                // Use board corners (Origin at Bottom-Left)
                Point2D.Double[] corners = new Point2D.Double[] {
                        new Point2D.Double(0, 0),
                        new Point2D.Double(boardDims.getX(), 0),
                        new Point2D.Double(boardDims.getX(), boardDims.getY()),
                        new Point2D.Double(0, boardDims.getY())
                };

                for (Point2D.Double corner : corners) {
                    Point2D.Double global = new Point2D.Double();
                    t.transform(corner, global);
                    globalMinX = Math.min(globalMinX, global.x);
                    globalMinY = Math.min(globalMinY, global.y);
                    globalMaxX = Math.max(globalMaxX, global.x);
                    globalMaxY = Math.max(globalMaxY, global.y);
                }
            } else {
                // Use placements bounding box
                List<Placement> placements = bl.getBoard().getPlacements();
                boolean hasPlacements = false;
                for (Placement p : placements) {
                    Location loc = p.getLocation();
                    Point2D.Double pt = new Point2D.Double(loc.getX(), loc.getY());
                    Point2D.Double global = new Point2D.Double();
                    t.transform(pt, global);

                    globalMinX = Math.min(globalMinX, global.x);
                    globalMinY = Math.min(globalMinY, global.y);
                    globalMaxX = Math.max(globalMaxX, global.x);
                    globalMaxY = Math.max(globalMaxY, global.y);
                    hasPlacements = true;
                }

                if (!hasPlacements) {
                    // Fallback to current location if nothing else info
                    Location loc = bl.getLocation();
                    globalMinX = loc.getX() - 10;
                    globalMinY = loc.getY() - 10;
                    globalMaxX = loc.getX() + 10;
                    globalMaxY = loc.getY() + 10;
                }
            }

            // Add margin for scanning (5mm)
            double margin = 5.0;
            globalMinX -= margin;
            globalMinY -= margin;
            globalMaxX += margin;
            globalMaxY += margin;

            minX.setText(new Length(globalMinX, units).toString());
            minY.setText(new Length(globalMinY, units).toString());
            width.setText(new Length(globalMaxX - globalMinX, units).toString());
            height.setText(new Length(globalMaxY - globalMinY, units).toString());
            zHeight.setText(new Length(bl.getLocation().getZ(), units).toString());

        } catch (Exception e) {
            UiUtils.showError(e);
        }
    }

    private final Action scanAction = new AbstractAction("Scan") {
        @Override
        public void actionPerformed(ActionEvent e) {
            startScan();
        }
    };

    private final Action saveAction = new AbstractAction("Save Image...") {
        @Override
        public void actionPerformed(ActionEvent e) {
            saveImage();
        }
    };

    private void startScan() {
        final Camera camera = (Camera) cameraComboBox.getSelectedItem();
        if (camera == null) {
            return;
        }

        final BoardLocation boardLocation = (BoardLocation) boardComboBox.getSelectedItem();

        final Length xLen = Length.parseWithDefaultUnits(minX.getText(), units);
        final Length yLen = Length.parseWithDefaultUnits(minY.getText(), units);
        final Length wLen = Length.parseWithDefaultUnits(width.getText(), units);
        final Length hLen = Length.parseWithDefaultUnits(height.getText(), units);
        final Length zLen = Length.parseWithDefaultUnits(zHeight.getText(), units);

        final double minXVal = xLen.getValue();
        final double minYVal = yLen.getValue();
        final double wVal = wLen.getValue();
        final double hVal = hLen.getValue();
        final double zVal = zLen.getValue();

        // Calculate Center of the Scan Area
        final double centerX = minXVal + (wVal / 2.0);
        final double centerY = minYVal + (hVal / 2.0);

        final Location unitsPerPixel = camera.getUnitsPerPixel();
        final double uppX = Math.abs(unitsPerPixel.getX());
        final double uppY = Math.abs(unitsPerPixel.getY());

        // Use average UPP for the canvas to keep aspect ratio 1:1 if possible, or use
        // X/Y.
        // Usually UPP X and Y are very close.
        final double canvasUpp = uppX;

        // Use field of view.
        final double fovW = camera.getWidth() * uppX;
        final double fovH = camera.getHeight() * uppY;

        // Calculate overlap (10% overlap)
        final double overlap = 0.1;
        final double stepX = fovW * (1.0 - overlap);
        final double stepY = fovH * (1.0 - overlap);

        final int cols = (int) Math.ceil(wVal / stepX);
        final int rows = (int) Math.ceil(hVal / stepY);
        final int totalShots = cols * rows;

        btnScan.setEnabled(false);
        progressBar.setMaximum(totalShots);
        progressBar.setValue(0);

        SwingWorker<BufferedImage, Integer> worker = new SwingWorker<BufferedImage, Integer>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                // Determine Canvas Size and Transform
                AffineTransform worldToCanvas;
                double imageW, imageH;

                if (boardLocation != null) {
                    // Logic 1: Crop to Board (using Board Local Coordinates)
                    double localMinX = 0, localMinY = 0, localMaxX = 0, localMaxY = 0;
                    Location boardDims = boardLocation.getBoard().getDimensions();
                    if (boardDims.getX() > 0 && boardDims.getY() > 0) {
                        localMaxX = boardDims.getX();
                        localMaxY = boardDims.getY();
                    } else {
                        // Find bounds from placements in local coordinates
                        boolean hasPlacements = false;
                        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
                        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
                        for (Placement p : boardLocation.getBoard().getPlacements()) {
                            Location loc = p.getLocation();
                            minX = Math.min(minX, loc.getX());
                            minY = Math.min(minY, loc.getY());
                            maxX = Math.max(maxX, loc.getX());
                            maxY = Math.max(maxY, loc.getY());
                            hasPlacements = true;
                        }
                        if (hasPlacements) {
                            localMinX = minX;
                            localMinY = minY;
                            localMaxX = maxX;
                            localMaxY = maxY;
                        } else {
                            // Fallback to default small area
                            localMaxX = 20;
                            localMaxY = 20;
                        }
                    }

                    // Add Output Margin (1mm)
                    double margin = 1.0;

                    // Canvas dimensions in pixels
                    imageW = Math.ceil(((localMaxX - localMinX) + 2 * margin) / canvasUpp);
                    imageH = Math.ceil(((localMaxY - localMinY) + 2 * margin) / canvasUpp);

                    // Transform: World -> Local -> Canvas
                    // 1. World -> Local
                    AffineTransform worldToLocal = boardLocation.getLocalToGlobalTransform().createInverse();

                    // 2. Local -> Canvas
                    // Map (localMinX - margin, localMaxY + margin) to (0,0) in canvas
                    // Local Y+ is Up. Canvas Y+ is Down.
                    // Scale (1/upp, -1/upp)

                    AffineTransform localToCanvas = new AffineTransform();
                    // Translate origin to margin
                    // We want localMinX to be at margin.
                    // The scale flips logic. Let's do it step by step.
                    // Point P_local.
                    // P_scaled.x = P_local.x / upp
                    // P_scaled.y = -P_local.y / upp
                    // We want:
                    // PixelX = (P_local.x - localMinX + margin) / upp
                    // PixelY = (localMaxY + margin - P_local.y) / upp <-- Standard mapping from
                    // cartesian to image
                    // = ( (localMaxY+margin)/upp ) - P_local.y/upp

                    // Wait, manual matrix is easier?
                    // Let's use the standard steps.
                    // 1. Translate so Top-Left (MinX, MaxY) is at (0,0).
                    localToCanvas.translate(margin / canvasUpp, margin / canvasUpp); // Add margin in pixels
                    localToCanvas.scale(1.0 / canvasUpp, -1.0 / canvasUpp); // Flip Y
                    localToCanvas.translate(-localMinX, -localMaxY); // Move Top-Left to Origin

                    worldToCanvas = new AffineTransform(localToCanvas);
                    worldToCanvas.concatenate(worldToLocal); // Apply World->Local first

                } else {
                    // Logic 2: Fallback (Use Scan Area)
                    // No rotation applied to the canvas itself, so it's aligned with machine
                    // coordinates.
                    // The scan area is defined by minXVal, minYVal, wVal, hVal.
                    // We want to map (minXVal, minYVal) to the bottom-left of the canvas,
                    // and (minXVal + wVal, minYVal + hVal) to the top-right.
                    // Canvas Y+ is down, Machine Y+ is up.

                    imageW = Math.ceil(wVal / canvasUpp) + camera.getWidth();
                    imageH = Math.ceil(hVal / canvasUpp) + camera.getHeight();

                    // Map (minXVal, minYVal + hVal) to (0,0) in canvas (top-left)
                    worldToCanvas = new AffineTransform();
                    worldToCanvas.translate(0, imageH); // Move origin to bottom-left of canvas
                    worldToCanvas.scale(1.0 / canvasUpp, -1.0 / canvasUpp); // Scale and flip Y
                    worldToCanvas.translate(-minXVal, -(minYVal + hVal)); // Move scan area's top-left to origin
                }

                BufferedImage combined = new BufferedImage((int) imageW, (int) imageH, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = combined.createGraphics();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                        java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING,
                        java.awt.RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(java.awt.Color.BLACK);
                g2.fillRect(0, 0, (int) imageW, (int) imageH);

                g2.setTransform(worldToCanvas);

                int count = 0;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        if (isCancelled()) {
                            return null;
                        }

                        // Calculate center location for this shot in Machine Coordinates
                        double cx = minXVal + (fovW / 2) + (c * stepX);
                        double cy = minYVal + (fovH / 2) + (r * stepY);

                        Location target = new Location(units, cx, cy, zVal, 0);
                        camera.moveTo(target);
                        BufferedImage shot = camera.settleAndCapture();

                        // Create Transform for the Shot Image -> Machine Coordinates
                        // Shot Image: (0,0) top-left.
                        // Machine: Center (cx, cy). Y is Up.
                        // Image (u,v) -> Machine (x,y)
                        // 1. Center the image: (u - w/2, v - h/2)
                        // 2. Scale UPP and Flip Y: (u*UPP, -v*UPP)
                        // 3. Translate to (cx, cy)

                        AffineTransform shotToWorld = new AffineTransform();
                        shotToWorld.translate(cx, cy);
                        shotToWorld.scale(uppX, -uppY);
                        shotToWorld.translate(-shot.getWidth() / 2.0, -shot.getHeight() / 2.0);

                        g2.drawImage(shot, shotToWorld, null);

                        count++;
                        publish(count);
                    }
                }
                g2.dispose();
                return combined;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int val = chunks.get(chunks.size() - 1);
                progressBar.setValue(val);
            }

            @Override
            protected void done() {
                try {
                    resultImage = get();
                    if (resultImage != null) {
                        previewLabel.setIcon(new ImageIcon(resultImage.getScaledInstance(
                                Math.min(resultImage.getWidth(), 600),
                                -1,
                                java.awt.Image.SCALE_DEFAULT)));
                        btnSave.setEnabled(true);
                    }
                } catch (Exception ex) {
                    UiUtils.showError(ex);
                } finally {
                    btnScan.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void saveImage() {
        if (resultImage == null) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getParentFile(), file.getName() + ".png");
            }
            try {
                ImageIO.write(resultImage, "png", file);
            } catch (Exception ex) {
                UiUtils.showError(ex);
            }
        }
    }
}
