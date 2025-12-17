package org.openpnp.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.spi.Camera;
import org.openpnp.spi.Feeder;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;

public class PlacementsPreviewDialog extends JDialog {
    public enum PreviewMode {
        PHOTO, OUTLINE
    }

    private final BoardLocation boardLocation;
    private final Camera camera;
    private final Nozzle defaultNozzle;
    private final PreviewMode mode;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JPanel contentPanel;

    public PlacementsPreviewDialog(BoardLocation boardLocation, Camera camera, PreviewMode mode) {
        super(MainFrame.get(), "Placements Preview (" + mode + ")", false);
        this.boardLocation = boardLocation;
        this.camera = camera;
        this.mode = mode;

        // Capture Nozzle on EDT
        this.defaultNozzle = MainFrame.get().getMachineControls().getSelectedNozzle();

        setLayout(new BorderLayout());
        setSize(800, 600);
        setLocationRelativeTo(MainFrame.get());

        contentPanel = new JPanel(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        statusLabel = new JLabel("Initializing...");
        statusPanel.add(statusLabel, BorderLayout.NORTH);
        statusPanel.add(progressBar, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);

        startWorker();
    }

    private void startWorker() {
        SwingWorker<BufferedImage, String> worker = new SwingWorker<BufferedImage, String>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                publish("Scanning Board...");
                BufferedImage boardImage = scanBoard(this::setProgress);

                if (mode == PreviewMode.OUTLINE) {
                    publish("Rendering Outlines...");
                    return renderOutlines(boardImage);
                }

                publish("Forming Placements Map...");
                Map<Part, BufferedImage> partImages = new HashMap<>();
                List<Placement> placements = boardLocation.getBoard().getPlacements();

                int total = placements.size();
                int current = 0;

                for (Placement p : placements) {
                    if (p.getType() != Placement.Type.Placement) {
                        continue;
                    }
                    Part part = p.getPart();
                    if (part == null) {
                        continue;
                    }

                    if (!partImages.containsKey(part)) {
                        publish("Capturing image for part: " + part.getId());
                        BufferedImage partImg = capturePartImage(part);
                        if (partImg != null) {
                            partImages.put(part, partImg);
                        }
                    }
                    current++;
                }

                publish("Compositing...");
                return composite(boardImage, partImages);
            }

            @Override
            protected void process(List<String> chunks) {
                for (String s : chunks) {
                    statusLabel.setText(s);
                }
            }

            @Override
            protected void done() {
                try {
                    BufferedImage result = get();
                    if (result != null) {
                        showResult(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    MessageBoxes.errorBox(PlacementsPreviewDialog.this, "Error during preview", e);
                    dispose();
                }
            }
        };
        worker.execute();
    }

    // --- Board Scanning Logic ---
    private BufferedImage scanBoard(Consumer<Integer> progressCallback) throws Exception {
        return org.openpnp.gui.support.BoardScanner.scanBoard(boardLocation, camera, progressCallback);
    }

    private BufferedImage capturePartImage(Part part) throws Exception {
        return Configuration.get().getMachine().submit(() -> {
            Feeder feeder = findFeeder(part);
            if (feeder == null) {
                return null;
            }

            Location pickLoc = preliminaryPickLocation(feeder, defaultNozzle);

            MovableUtils.moveToLocationAtSafeZ(camera, pickLoc);

            BufferedImage raw = org.openpnp.gui.support.ImageCaptureUtils.settleAndCaptureWithLight(camera);

            BufferedImage rotated = org.openpnp.gui.support.ImageCaptureUtils.rotateImage(raw, pickLoc.getRotation());

            return org.openpnp.gui.support.ImageCaptureUtils.cropImageToPart(rotated, part, camera);
        }).get();
    }

    private Feeder findFeeder(Part part) {
        for (Feeder f : Configuration.get().getMachine().getFeeders()) {
            if (f.getPart() == part && f.isEnabled()) {
                return f;
            }
        }
        return null;
    }

    // Copied from FeedersPanel/Utils
    // Copied from FeedersPanel/Utils
    private Location preliminaryPickLocation(Feeder feeder, Nozzle nozzle) throws Exception {
        return org.openpnp.gui.support.FeederUtils.preliminaryPickLocation(feeder, nozzle);
    }

    private BufferedImage composite(BufferedImage boardImg, Map<Part, BufferedImage> partImages) {
        BufferedImage result = new BufferedImage(boardImg.getWidth(), boardImg.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.drawImage(boardImg, 0, 0, null);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle2D.Double bounds = org.openpnp.gui.support.BoardScanner.getBoardBounds(boardLocation.getBoard());
        double minX = bounds.x;
        double minY = bounds.y;
        double boardH = bounds.height;
        double maxY = minY + boardH;

        double margin = 5.0;
        double upp = Math.abs(camera.getUnitsPerPixel().getX());

        // Canvas coords setup (Must match scanBoard)
        AffineTransform localToCanvas = new AffineTransform();
        localToCanvas.translate(margin / upp, margin / upp);
        localToCanvas.scale(1.0 / upp, -1.0 / upp);
        localToCanvas.translate(-minX, -maxY);

        for (Placement p : boardLocation.getBoard().getPlacements()) {
            BufferedImage partImg = partImages.get(p.getPart());
            if (partImg == null) {
                continue;
            }

            // Placement Location matches board Local Coordinates
            Location loc = p.getLocation();

            // Transform Local -> Canvas
            Point2D.Double lPt = new Point2D.Double(loc.getX(), loc.getY());
            Point2D.Double cPt = new Point2D.Double();
            localToCanvas.transform(lPt, cPt);

            // Draw
            AffineTransform txt = new AffineTransform();
            txt.translate(cPt.x, cPt.y);
            txt.rotate(Math.toRadians(loc.getRotation()) * -1);
            txt.translate(-partImg.getWidth() / 2.0, -partImg.getHeight() / 2.0);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
            g2.drawImage(partImg, txt, null);
        }

        g2.dispose();
        return result;
    }

    private BufferedImage renderOutlines(BufferedImage boardImg) {
        BufferedImage result = new BufferedImage(boardImg.getWidth(), boardImg.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        g2.drawImage(boardImg, 0, 0, null);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.YELLOW);
        g2.setStroke(new BasicStroke(2.0f));

        Rectangle2D.Double bounds = org.openpnp.gui.support.BoardScanner.getBoardBounds(boardLocation.getBoard());
        double minX = bounds.x;
        double minY = bounds.y;
        double boardH = bounds.height;
        double maxY = minY + boardH;

        double margin = 5.0;
        double upp = Math.abs(camera.getUnitsPerPixel().getX());

        // Canvas coords setup
        AffineTransform localToCanvas = new AffineTransform();
        localToCanvas.translate(margin / upp, margin / upp);
        localToCanvas.scale(1.0 / upp, -1.0 / upp);
        localToCanvas.translate(-minX, -maxY);

        for (Placement p : boardLocation.getBoard().getPlacements()) {
            if (p.getType() != Placement.Type.Placement) {
                continue;
            }
            Part part = p.getPart();
            if (part == null || part.getPackage() == null || part.getPackage().getFootprint() == null) {
                continue;
            }

            Shape shape = part.getPackage().getFootprint().getPadsShape();
            if (shape == null) {
                continue;
            }

            Location loc = p.getLocation();

            // Transform: Footprint Local -> Placed Local -> Canvas
            AffineTransform tx = new AffineTransform();
            tx.setTransform(localToCanvas);
            tx.translate(loc.getX(), loc.getY());
            tx.rotate(Math.toRadians(loc.getRotation()));

            // Draw
            g2.draw(tx.createTransformedShape(shape));
        }

        g2.dispose();
        return result;
    }

    private void showResult(BufferedImage image) {
        statusLabel.setText("Done.");
        progressBar.setVisible(false);
        contentPanel.removeAll();
        JLabel label = new JLabel(new ImageIcon(image));
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(label);
        org.openpnp.util.UiUtils.enableDragPanning(scrollPane);

        // Limit size to 75% of screen or image size, whichever is smaller
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = (int) (screenSize.width * 0.75);
        int maxHeight = (int) (screenSize.height * 0.75);
        scrollPane.setPreferredSize(new java.awt.Dimension(
                Math.min(image.getWidth(), maxWidth),
                Math.min(image.getHeight(), maxHeight)));

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JButton btnSave = new JButton("Save Image...");
        btnSave.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = chooser.getSelectedFile();
                    if (!file.getName().toLowerCase().endsWith(".png")) {
                        file = new File(file.getParentFile(), file.getName() + ".png");
                    }
                    ImageIO.write(image, "png", file);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        add(btnSave, BorderLayout.SOUTH);

        pack();
        revalidate();
    }
}
