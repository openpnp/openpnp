package org.openpnp.gui.components.nav;

import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class SelectedEffect extends DropShadow {
    public SelectedEffect() {
        super(BlurType.GAUSSIAN, Color.BLACK, 2, 0.65, 0, 0);
    }
}
