package org.openpnp.gui.pkggen;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;

import javax.swing.JPanel;

import org.openpnp.model.Footprint;
import org.openpnp.model.Package;

public class PackageView extends JPanel {
    private boolean top = false;
    private Package pkg = null;

    public PackageView() {
        setBackground(Color.black);
    }
    
    @Override
    protected void paintComponent(Graphics gr) {
        super.paintComponent(gr);
        
        Graphics2D g = (Graphics2D) gr;
        
        g.setStroke(new BasicStroke(2f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (pkg == null) {
            return;
        }
        
        Footprint footprint = pkg.getFootprint();
        if (footprint == null) {
            return;
        }

        g.translate(200, 200);
        g.scale(50, 50);
        
        if (top) {
            fillShape(g, footprint.getPadsShape(), new Color(216, 216, 216));
            fillShape(g, footprint.getBodyShape(), new Color(60, 60, 60));
        }
        else {
            fillShape(g, footprint.getBodyShape(), new Color(60, 60, 60));
            fillShape(g, footprint.getPadsShape(), new Color(216, 216, 216));
        }
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
