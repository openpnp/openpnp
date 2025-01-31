package org.openpnp.machine.photon.sheets.gui;

import org.openpnp.machine.photon.PhotonFeeder;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FeederSearchProgressBar extends JPanel {
    private int numberOfElements;
    private final Map<Integer, PhotonFeeder.FeederSearchState> feederSearchStateMap;

    private static final Color searching_color = new Color(0xDAA520);
    private static final Color missing_color = new Color(0x6495ED);
    private static final Color found_color = new Color(0x3CB371);

    public FeederSearchProgressBar() {
        numberOfElements = 0;
        feederSearchStateMap = new HashMap<>();
    }

    private Rectangle getRectangleForElement(int elementNumber) {
        int totalWidth = getWidth();
        int totalHeight = getHeight();

        int startX = (elementNumber * totalWidth) / numberOfElements;
        int endX = ((elementNumber + 1) * totalWidth) / numberOfElements;
        return new Rectangle(startX, 0, endX - startX, totalHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int totalWidth = getWidth();
        int totalHeight = getHeight();

        for (int elementNumber = 0; elementNumber < numberOfElements; elementNumber++) {
            PhotonFeeder.FeederSearchState feederSearchState;
            feederSearchState = feederSearchStateMap.getOrDefault(elementNumber, PhotonFeeder.FeederSearchState.UNKNOWN);
            switch (feederSearchState) {
                case UNKNOWN:
                    g.setColor(Color.LIGHT_GRAY);
                    break;
                case SEARCHING:
                    g.setColor(searching_color);
                    break;
                case FOUND:
                    g.setColor(found_color);
                    break;
                case MISSING:
                    g.setColor(missing_color);
                    break;
            }
            Rectangle rectangle = getRectangleForElement(elementNumber);
            g.fillRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
        }

        g.setColor(Color.black);
        g.drawRect(0, 0, totalWidth, totalHeight);
    }

    public void clearAllState() {
        feederSearchStateMap.clear();
        this.repaint();
    }

    public void updateFeederState(int feederAddress, PhotonFeeder.FeederSearchState feederSearchState) {
        feederSearchStateMap.put(feederAddress - 1, feederSearchState);
        this.repaint();  // TODO Make this repaint only our small area
    }

    public void setNumberOfElements(int numberOfElements) {
        this.numberOfElements = numberOfElements;
        this.repaint();
    }
}
