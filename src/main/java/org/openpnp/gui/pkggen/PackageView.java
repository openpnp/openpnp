package org.openpnp.gui.pkggen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JPanel;

import org.openpnp.model.Footprint;
import org.openpnp.model.Footprint.Pad;
import org.openpnp.model.Package;

public class PackageView extends JPanel {
    private boolean top = false;
    private Package pkg = null;
    
    public PackageView() {
    }

    @Override
    protected void paintComponent(Graphics gr) {
        super.paintComponent(gr);

        Graphics2D g = (Graphics2D) gr;
        
        AffineTransform tx = g.getTransform();
        
        g.setStroke(new BasicStroke(2f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (pkg == null) {
            return;
        }
        
        Footprint footprint = pkg.getFootprint();
        if (footprint == null) {
            return;
        }
        
        Insets insets = getInsets();

        // Get the viewport bounds
        int width = getWidth() - insets.left - insets.right;
        int height = getHeight() - insets.top - insets.bottom;
        int x = insets.left;
        int y = insets.top;
        
        // Fill the background
        g.setColor(Color.black);
        g.fillRect(x, y, width, height);
        
        // Get the shape bounds
        Path2D.Double shape = new Path2D.Double();
        shape.append(footprint.getPadsShape(), false);
        shape.append(footprint.getBodyShape(), false);
        Rectangle2D bounds = shape.getBounds2D();

        // Get the maximum scale possible given the bounds
        double scale = Math.min(width / bounds.getWidth(), height / bounds.getHeight());
        // Reduce the scale by 10% to give a little padding around the edges.
        scale *= 0.9;
        
        g.translate(x + width / 2, y + height / 2);
        g.scale(scale, scale);

        Pad pin1 = null;
        for (Pad pad : footprint.getPads()) {
            if (pad.getName().equals("1")) {
                pin1 = pad;
                break;
            }
        }
        
        if (top) {
            fillShape(g, footprint.getPadsShape(), new Color(216, 216, 216));
            fillShape(g, footprint.getBodyShape(), new Color(60, 60, 60));
            fillShape(g, pin1.getShape(), Color.red);
        }
        else {
            fillShape(g, footprint.getBodyShape(), new Color(60, 60, 60));
            fillShape(g, footprint.getPadsShape(), new Color(216, 216, 216));
            fillShape(g, pin1.getShape(), Color.red);
        }

        g.setTransform(tx);
        g.translate(x + width / 2, y + height / 2);
        
        g.setColor(Color.black);
        for (Pad pad : footprint.getPads()) {
            // TODO center and highlight
            Rectangle2D r = g.getFontMetrics().getStringBounds(pad.getName(), g);
            g.drawString(pad.getName(), 
                    (float) ((pad.getX() * scale) - (r.getWidth() / 2)), 
                    (float) ((pad.getY() * -scale) + (r.getHeight() / 2)));
        }
        
        g.setTransform(tx);
        
        g.setColor(Color.orange);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(x + width / 2, y + 0, x + width / 2, y + height);
        g.drawLine(x + 0, y + height / 2, x + width, y + height / 2);
    }
    
    private void fillShape(Graphics2D g, Shape shape, Color color) {
        g.setColor(color);
        g.fill(shape);
    }

    public boolean isTop() {
        return top;
    }
    
    public void setTop(boolean top) {
        this.top = top;
        repaint();
    }
    
    public Package getPkg() {
        return pkg;
    }
    
    public void setPkg(Package pkg) {
        this.pkg = pkg;
        repaint();
    }
}
