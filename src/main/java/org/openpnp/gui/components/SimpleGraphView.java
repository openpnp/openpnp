package org.openpnp.gui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;

import javax.swing.JComponent;

import org.openpnp.machine.reference.ReferenceNozzleTip;

public class SimpleGraphView extends JComponent {  
    
    private Map<Double, Double> graph;
    private final Dimension PREF_SIZE = new Dimension(100, 50);
    private ReferenceNozzleTip nozzleTip;
    
    public SimpleGraphView(ReferenceNozzleTip nozzleTip) {
        this.nozzleTip = nozzleTip; 
        //this.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Graph", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
    }

    public Map<Double, Double> getGraph() {
        return graph;
    }
    public void setGraph(Map<Double, Double> graph) {
        this.graph = graph;
    }
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color color1 = new Color(225, 0, 0);
        int w = getWidth();
        int h = getHeight();
        double tMin = Double.MAX_VALUE;
        double tMax = Double.MIN_VALUE;
        double vMin = Double.MAX_VALUE;
        double vMax = Double.MIN_VALUE;
        if (graph != null && graph.size() > 0) {
            for (double t : graph.keySet()) {
                double v = graph.get(t);
                tMin = Math.min(t,  tMin);
                tMax = Math.max(t,  tMax);
                vMin = Math.min(v,  vMin);
                vMax = Math.max(v,  vMax);
            }
            double tScale = (w-1)/(tMax-tMin);
            double vScale = (h-1)/(vMax-vMin);
            double v0 = Double.NaN;
            double t0 = Double.NaN;
            g2d.setColor(color1);
            for (double t : graph.keySet()) {
                double v = graph.get(t);
                if (!Double.isNaN(t0)) {
                    g2d.drawLine((int)((t0-tMin)*tScale), (int)(h-1-(v0-vMin)*vScale), 
                            (int)((t-tMin)*tScale), (int)(h-1-(v-vMin)*vScale));
                }
            }
        }
        else {
            g2d.setColor(color1);
            g2d.drawLine(0, 0, w-1, h-1);
            g2d.drawLine(0, h-1, w-1, 0);
        }
    }
    @Override
    public Dimension getPreferredSize() {
        Dimension superDim = super.getPreferredSize();
        int width = (int)Math.max(superDim.getWidth(), PREF_SIZE.getWidth());
        int height = (int)Math.max(superDim.getHeight(), PREF_SIZE.getHeight()); 
        return new Dimension(width, height);
    }
}