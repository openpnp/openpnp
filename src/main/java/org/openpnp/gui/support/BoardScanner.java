package org.openpnp.gui.support;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import org.openpnp.model.BoardLocation;
import org.openpnp.model.Location;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.model.Configuration;
import org.openpnp.util.MovableUtils;

public class BoardScanner {

    /**
     * Scans the board area by capturing multiple images and stitching them
     * together.
     * 
     * @param boardLocation    The board location to scan.
     * @param camera           The camera to use.
     * @param progressCallback Optional callback for progress (0-100).
     * @return The stitched image.
     * @throws Exception
     */
    public static BufferedImage scanBoard(BoardLocation boardLocation, Camera camera,
            Consumer<Integer> progressCallback) throws Exception {
        Location boardDims = boardLocation.getBoard().getDimensions();
        double boardW = boardDims.getX();
        double boardH = boardDims.getY();

        if (boardW <= 0) {
            boardW = 100;
        }
        if (boardH <= 0) {
            boardH = 100;
        }

        double margin = 5.0; // 5mm margin

        Location unitsPerPixel = camera.getUnitsPerPixel();
        double uppX = Math.abs(unitsPerPixel.getX());
        double uppY = Math.abs(unitsPerPixel.getY());
        double canvasUpp = uppX;

        // Field of View
        double fovW = camera.getWidth() * uppX;
        double fovH = camera.getHeight() * uppY;

        // Overlap
        double overlap = 0.1;

        // Rotation scaling to ensure coverage
        double rad = Math.toRadians(boardLocation.getLocation().getRotation());
        double cos = Math.abs(Math.cos(rad));
        double sin = Math.abs(Math.sin(rad));

        // Project FOV limits onto the rotated axes.
        double limitW = fovW * (1.0 - overlap);
        double limitH = fovH * (1.0 - overlap);

        double stepX = Math.min(limitW / Math.max(cos, 1e-9), limitH / Math.max(sin, 1e-9));
        double stepY = Math.min(limitW / Math.max(sin, 1e-9), limitH / Math.max(cos, 1e-9));

        int cols = (int) Math.ceil((boardW + 2 * margin) / stepX);
        int rows = (int) Math.ceil((boardH + 2 * margin) / stepY);
        int totalShots = cols * rows;

        // Canvas Size
        int canvasW = (int) Math.ceil((boardW + 2 * margin) / canvasUpp);
        int canvasH = (int) Math.ceil((boardH + 2 * margin) / canvasUpp);

        BufferedImage combined = new BufferedImage(canvasW, canvasH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = combined.createGraphics();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, canvasW, canvasH);

        // Transform Setup: Machine -> World -> Local -> Canvas
        AffineTransform worldToLocal = boardLocation.getLocalToGlobalTransform().createInverse();

        // Local -> Canvas
        AffineTransform localToCanvas = new AffineTransform();
        localToCanvas.translate(margin / canvasUpp, margin / canvasUpp);
        localToCanvas.scale(1.0 / canvasUpp, -1.0 / canvasUpp);
        localToCanvas.translate(0, -boardH);

        AffineTransform worldToCanvas = new AffineTransform(localToCanvas);
        worldToCanvas.concatenate(worldToLocal);

        double zVal = boardLocation.getLocation().getZ();

        int shotCount = 0;

        // Grid Scan in Local Coordinates
        double startLocalX = -margin + (fovW / 2);
        double startLocalY = -margin + (fovH / 2);

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        Actuator light = camera.getLightActuator();
        boolean lightWasOn = false;
        if (light != null) {
            final Actuator lightFinal = light;
            lightWasOn = Configuration.get().getMachine().submit(() -> {
                Boolean actuated = lightFinal.isActuated();
                if (actuated == null || !actuated) {
                    lightFinal.actuate(true);
                    Thread.sleep(250);
                    return false;
                } else {
                    return true;
                }
            }).get();
        }

        try {
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    double lx = startLocalX + c * stepX;
                    double ly = startLocalY + r * stepY;

                    // Convert Local to Machine
                    Point2D.Double lPt = new Point2D.Double(lx, ly);
                    Point2D.Double mPt = new Point2D.Double();
                    boardLocation.getLocalToGlobalTransform().transform(lPt, mPt);

                    Location target = new Location(boardLocation.getLocation().getUnits(), mPt.x, mPt.y, zVal, 0);

                    MovableUtils.moveToLocationAtSafeZ(camera, target);
                    BufferedImage shot = camera.settleAndCapture();

                    // Draw Shot
                    AffineTransform shotToWorld = new AffineTransform();
                    shotToWorld.setTransform(worldToCanvas);
                    shotToWorld.translate(mPt.x, mPt.y);
                    shotToWorld.scale(uppX, -uppY);
                    shotToWorld.translate(-shot.getWidth() / 2.0, -shot.getHeight() / 2.0);

                    g2.drawImage(shot, shotToWorld, null);

                    shotCount++;
                    if (progressCallback != null) {
                        progressCallback.accept(100 * shotCount / totalShots);
                    }
                }
            }
        } finally {
            if (light != null && !lightWasOn) {
                final Actuator lightFinal = light;
                Configuration.get().getMachine().submit(() -> {
                    lightFinal.actuate(false);
                    return null;
                }).get();
            }
        }
        g2.dispose();

        return combined;
    }
}
