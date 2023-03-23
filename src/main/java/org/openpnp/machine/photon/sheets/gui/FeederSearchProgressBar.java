package org.openpnp.machine.photon.sheets.gui;

import org.openpnp.machine.photon.PhotonFeeder;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FeederSearchProgressBar extends JPanel {
    private int numberOfElements;
    private final Map<Integer, PhotonFeeder.FeederSearchState> feederSearchStateMap;

    public FeederSearchProgressBar() {
        numberOfElements = 50;
        feederSearchStateMap = new HashMap<>();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int totalWidth = getWidth();
        int totalHeight = getHeight();

        int currentX = 0;
        for (int elementNumber = 0; elementNumber < numberOfElements; elementNumber++) {
            int endX = ((elementNumber + 1) * totalWidth) / numberOfElements;
            PhotonFeeder.FeederSearchState feederSearchState;
            feederSearchState = feederSearchStateMap.getOrDefault(elementNumber, PhotonFeeder.FeederSearchState.UNKNOWN);
            switch (feederSearchState) {
                case UNKNOWN:
                    g.setColor(Color.LIGHT_GRAY);
                    break;
                case SEARCHING:
                    g.setColor(Color.CYAN);
                    break;
                case FOUND:
                    g.setColor(Color.GREEN);
                    break;
                case MISSING:
                    g.setColor(Color.RED);
                    break;
            }
            g.fillRect(currentX, 0, endX - currentX, totalHeight);
            currentX = endX;
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
    }
}
